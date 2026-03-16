import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// ============================================
// 커스텀 메트릭: 시나리오별 응답 시간 추적
// ============================================
const productListDuration = new Trend('product_list_duration', true);
const productListByBrandDuration = new Trend('product_list_by_brand_duration', true);
const productListSortedDuration = new Trend('product_list_sorted_duration', true);
const productDetailDuration = new Trend('product_detail_duration', true);
const productDeepPageDuration = new Trend('product_deep_page_duration', true);
const brandListDuration = new Trend('brand_list_duration', true);
const slowQueryCount = new Counter('slow_queries');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SLOW_THRESHOLD = 500; // 500ms 이상이면 slow query로 판정

// ============================================
// 테스트 시나리오 설정
// ============================================
export const options = {
  scenarios: {
    // 1단계: 워밍업 (10 VU)
    warmup: {
      executor: 'constant-vus',
      vus: 10,
      duration: '15s',
      startTime: '0s',
      tags: { phase: 'warmup' },
    },
    // 2단계: 일반 부하 (50 VU)
    normal_load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      startTime: '15s',
      tags: { phase: 'normal' },
    },
    // 3단계: 고부하 (200 VU)
    high_load: {
      executor: 'constant-vus',
      vus: 200,
      duration: '30s',
      startTime: '45s',
      tags: { phase: 'high' },
    },
    // 4단계: 스파이크 (500 VU)
    spike: {
      executor: 'constant-vus',
      vus: 500,
      duration: '15s',
      startTime: '75s',
      tags: { phase: 'spike' },
    },
  },
  thresholds: {
    // 전체 요청의 95%가 1초 이내
    http_req_duration: ['p(95)<1000'],
    // 시나리오별 임계치
    product_list_duration: ['p(95)<800', 'p(99)<1500'],
    product_list_by_brand_duration: ['p(95)<500', 'p(99)<1000'],
    product_list_sorted_duration: ['p(95)<800', 'p(99)<1500'],
    product_detail_duration: ['p(95)<300', 'p(99)<500'],
    product_deep_page_duration: ['p(95)<1500', 'p(99)<3000'],
    brand_list_duration: ['p(95)<300', 'p(99)<500'],
  },
};

// ============================================
// 헬퍼: slow query 감지 및 기록
// ============================================
function trackResponse(res, metricTrend, label) {
  metricTrend.add(res.timings.duration);
  if (res.timings.duration > SLOW_THRESHOLD) {
    slowQueryCount.add(1);
    console.warn(
      `🐢 SLOW [${res.timings.duration.toFixed(0)}ms] ${label} - status: ${res.status}`
    );
  }
}

// ============================================
// 메인 테스트 함수
// ============================================
export default function () {
  const brandIds = Array.from({ length: 20 }, (_, i) => i + 1);
  const sorts = ['price_asc', 'price_desc', 'likes_desc', null];
  const randomBrandId = brandIds[Math.floor(Math.random() * brandIds.length)];
  const randomSort = sorts[Math.floor(Math.random() * sorts.length)];
  const randomProductId = Math.floor(Math.random() * 13000) + 1;

  // ── 1. 상품 목록 (필터 없음, 기본 정렬) ──
  group('상품 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/products?page=0&size=20`);
    check(res, { '200 OK': (r) => r.status === 200 });
    trackResponse(res, productListDuration, 'GET /products?page=0&size=20');
  });

  // ── 2. 브랜드 필터 조회 ──
  group('브랜드별 상품 조회', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/products?brandId=${randomBrandId}&page=0&size=20`
    );
    check(res, { '200 OK': (r) => r.status === 200 });
    trackResponse(
      res,
      productListByBrandDuration,
      `GET /products?brandId=${randomBrandId}`
    );
  });

  // ── 3. 정렬 조회 (price, likes) ──
  group('정렬 조회', () => {
    const sortParam = randomSort ? `&sort=${randomSort}` : '';
    const res = http.get(
      `${BASE_URL}/api/v1/products?page=0&size=20${sortParam}`
    );
    check(res, { '200 OK': (r) => r.status === 200 });
    trackResponse(
      res,
      productListSortedDuration,
      `GET /products?sort=${randomSort}`
    );
  });

  // ── 4. 깊은 페이지 조회 (offset 부하) ──
  group('깊은 페이지 조회', () => {
    const deepPage = Math.floor(Math.random() * 100) + 50; // page 50~149
    const res = http.get(
      `${BASE_URL}/api/v1/products?page=${deepPage}&size=20`
    );
    check(res, { '200 OK or empty': (r) => r.status === 200 });
    trackResponse(
      res,
      productDeepPageDuration,
      `GET /products?page=${deepPage}&size=20`
    );
  });

  // ── 5. 상품 상세 조회 ──
  group('상품 상세 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/products/${randomProductId}`);
    check(res, { '200 or 404': (r) => r.status === 200 || r.status === 404 });
    trackResponse(
      res,
      productDetailDuration,
      `GET /products/${randomProductId}`
    );
  });

  // ── 6. 브랜드 목록 조회 ──
  group('브랜드 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api-admin/v1/brands`);
    check(res, { '200 OK': (r) => r.status === 200 });
    trackResponse(res, brandListDuration, 'GET /brands');
  });

  // ── 7. 복합 시나리오: 브랜드 필터 + 정렬 + 큰 size ──
  group('복합 조회 (필터+정렬+큰사이즈)', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/products?brandId=${randomBrandId}&sort=price_asc&page=0&size=100`
    );
    check(res, { '200 OK': (r) => r.status === 200 });
    trackResponse(
      res,
      productListSortedDuration,
      `GET /products?brandId=${randomBrandId}&sort=price_asc&size=100`
    );
  });

  sleep(0.1); // 요청 간 100ms 간격
}

// ============================================
// 테스트 종료 후 요약
// ============================================
function getMetricP(data, name, percentile) {
  var m = data.metrics[name];
  if (!m || !m.values) return 'N/A';
  var v = m.values['p(' + percentile + ')'];
  return v ? v.toFixed(0) + 'ms' : 'N/A';
}

function getMetricCount(data, name) {
  var m = data.metrics[name];
  if (!m || !m.values) return 0;
  return m.values.count || 0;
}

export function handleSummary(data) {
  var lines = [
    '',
    '=== Slow Query 부하테스트 결과 ===',
    '  총 요청 수: ' + getMetricCount(data, 'http_reqs'),
    '  총 slow query 수: ' + getMetricCount(data, 'slow_queries'),
    '  전체 p95: ' + getMetricP(data, 'http_req_duration', 95),
    '  전체 p99: ' + getMetricP(data, 'http_req_duration', 99),
    '',
    '── 시나리오별 p95 ──',
    '  상품 목록:     ' + getMetricP(data, 'product_list_duration', 95),
    '  브랜드 필터:   ' + getMetricP(data, 'product_list_by_brand_duration', 95),
    '  정렬 조회:     ' + getMetricP(data, 'product_list_sorted_duration', 95),
    '  상세 조회:     ' + getMetricP(data, 'product_detail_duration', 95),
    '  깊은 페이지:   ' + getMetricP(data, 'product_deep_page_duration', 95),
    '  브랜드 목록:   ' + getMetricP(data, 'brand_list_duration', 95),
    '',
  ];

  console.log(lines.join('\n'));

  return {};
}
