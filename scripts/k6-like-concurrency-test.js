import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter, Rate, Gauge } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ============================================
// 커스텀 메트릭
// ============================================
const likeDuration = new Trend('like_duration', true);
const unlikeDuration = new Trend('unlike_duration', true);
const likeListDuration = new Trend('like_list_duration', true);
const likeErrorRate = new Rate('like_errors');
const concurrencyConflicts = new Counter('concurrency_conflicts');
const slowLikeCount = new Counter('slow_likes');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SLOW_THRESHOLD = 500;
const USER_COUNT = 99;
const PASSWORD = 'Passw0rd11';

// ============================================
// 1단계: Setup — 테스트용 유저 생성
// ============================================
export function setup() {
  console.log('=== Setup: 테스트 유저 생성 ===');

  const users = [];
  for (let i = 1; i <= USER_COUNT; i++) {
    const loginId = `likeuser${i}`;
    const res = http.post(
      `${BASE_URL}/api/v1/users`,
      JSON.stringify({
        loginId: loginId,
        password: PASSWORD,
        name: `테스트유저${i}`,
        birthday: '1990-06-20',
        email: `k6user${i}@test.com`,
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status === 200 || res.status === 409) {
      users.push(loginId);
    }
  }

  console.log(`생성된 유저 수: ${users.length}`);
  return { users };
}

// ============================================
// 테스트 시나리오 설정
// ============================================
export const options = {
  scenarios: {
    // 시나리오 1: 좋아요 부하 테스트 (다양한 유저 → 다양한 상품)
    like_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },   // 워밍업
        { duration: '20s', target: 50 },   // 일반 부하
        { duration: '20s', target: 100 },  // 고부하
        { duration: '10s', target: 0 },    // 쿨다운
      ],
      exec: 'likeLoadTest',
      tags: { test_type: 'like_load' },
    },

    // 시나리오 2: 동시성 테스트 (다수 유저 → 동일 상품에 동시 좋아요)
    concurrency_test: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      startTime: '65s',
      exec: 'concurrencyTest',
      tags: { test_type: 'concurrency' },
    },

    // 시나리오 3: 좋아요 + 취소 반복 (동시성 충돌 유발)
    like_unlike_race: {
      executor: 'per-vu-iterations',
      vus: 30,
      iterations: 5,
      startTime: '75s',
      exec: 'likeUnlikeRace',
      tags: { test_type: 'race_condition' },
    },
  },
  thresholds: {
    like_duration: ['p(95)<500', 'p(99)<1000'],
    unlike_duration: ['p(95)<500', 'p(99)<1000'],
    like_list_duration: ['p(95)<300', 'p(99)<500'],
    like_errors: ['rate<0.05'],  // 에러율 5% 미만
  },
};

// ============================================
// 헬퍼 함수
// ============================================
function authHeaders(loginId) {
  return {
    'Content-Type': 'application/json',
    'X-Loopers-LoginId': loginId,
    'X-Loopers-LoginPw': PASSWORD,
  };
}

function trackResponse(res, metricTrend, label) {
  metricTrend.add(res.timings.duration);
  if (res.timings.duration > SLOW_THRESHOLD) {
    slowLikeCount.add(1);
    console.warn(`🐢 SLOW [${res.timings.duration.toFixed(0)}ms] ${label} - status: ${res.status}`);
  }
  if (res.status >= 400 && res.status !== 409) {
    likeErrorRate.add(1);
  } else {
    likeErrorRate.add(0);
  }
}

// ============================================
// 시나리오 1: 좋아요 부하 테스트
// ============================================
export function likeLoadTest(data) {
  const userIndex = (__VU % USER_COUNT) + 1;
  const loginId = `likeuser${userIndex}`;
  const headers = authHeaders(loginId);
  const productId = Math.floor(Math.random() * 13000) + 1;

  // 1. 좋아요 등록
  group('좋아요 등록', () => {
    const res = http.post(`${BASE_URL}/api/v1/products/${productId}/likes`, null, { headers });
    check(res, { 'like 200 OK': (r) => r.status === 200 });
    trackResponse(res, likeDuration, `POST /products/${productId}/likes`);
  });

  sleep(0.05);

  // 2. 좋아요 목록 조회
  group('좋아요 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/users/${loginId}/likes`, { headers });
    check(res, { 'like list 200': (r) => r.status === 200 });
    trackResponse(res, likeListDuration, `GET /users/${loginId}/likes`);
  });

  sleep(0.05);

  // 3. 좋아요 취소
  group('좋아요 취소', () => {
    const res = http.del(`${BASE_URL}/api/v1/products/${productId}/likes`, null, { headers });
    check(res, { 'unlike 200 OK': (r) => r.status === 200 });
    trackResponse(res, unlikeDuration, `DELETE /products/${productId}/likes`);
  });

  sleep(0.1);
}

// ============================================
// 시나리오 2: 동시성 테스트 — 50명이 동일 상품에 동시 좋아요
// ============================================
export function concurrencyTest(data) {
  const targetProductId = 1;  // 모든 VU가 같은 상품
  const userIndex = ((__VU - 1) % USER_COUNT) + 1;
  const loginId = `likeuser${userIndex}`;
  const headers = authHeaders(loginId);

  group('동시 좋아요 (상품 #1)', () => {
    const res = http.post(`${BASE_URL}/api/v1/products/${targetProductId}/likes`, null, { headers });

    const ok = check(res, {
      'concurrent like success': (r) => r.status === 200,
    });

    if (!ok) {
      concurrencyConflicts.add(1);
      console.warn(`⚡ CONFLICT VU=${__VU} status=${res.status} body=${res.body}`);
    }

    trackResponse(res, likeDuration, `CONCURRENT POST /products/${targetProductId}/likes`);
  });
}

// ============================================
// 시나리오 3: 좋아요/취소 레이스 컨디션
// ============================================
export function likeUnlikeRace(data) {
  const targetProductId = 2;  // 레이스 컨디션 타겟
  const userIndex = ((__VU - 1) % USER_COUNT) + 1;
  const loginId = `likeuser${userIndex}`;
  const headers = authHeaders(loginId);

  group('좋아요-취소 레이스', () => {
    // 좋아요
    const likeRes = http.post(`${BASE_URL}/api/v1/products/${targetProductId}/likes`, null, { headers });
    trackResponse(likeRes, likeDuration, `RACE POST /products/${targetProductId}/likes`);

    // 즉시 취소 (레이스 유발)
    const unlikeRes = http.del(`${BASE_URL}/api/v1/products/${targetProductId}/likes`, null, { headers });
    trackResponse(unlikeRes, unlikeDuration, `RACE DELETE /products/${targetProductId}/likes`);

    if (likeRes.status >= 500 || unlikeRes.status >= 500) {
      concurrencyConflicts.add(1);
      console.error(`🚨 RACE ERROR VU=${__VU} like=${likeRes.status} unlike=${unlikeRes.status}`);
    }
  });
}

// ============================================
// 테스트 종료 후 검증 + 요약
// ============================================
export function teardown(data) {
  console.log('=== Teardown: 동시성 검증 ===');

  // 상품 #1의 likeCount 확인 (50명이 동시 좋아요 → 50이어야 정상)
  const productRes = http.get(`${BASE_URL}/api/v1/products/1`);
  if (productRes.status === 200) {
    const product = JSON.parse(productRes.body);
    const likeCount = product.likeCount !== undefined ? product.likeCount : 'N/A';
    console.log(`🔍 상품 #1 likeCount: ${likeCount} (기대값: 50)`);

    if (likeCount !== 50 && likeCount !== 'N/A') {
      console.error(`🚨 동시성 문제 감지! 기대값=50, 실제값=${likeCount}`);
    }
  }

  // 상품 #2의 likeCount 확인 (레이스 후 0이어야 정상)
  const product2Res = http.get(`${BASE_URL}/api/v1/products/2`);
  if (product2Res.status === 200) {
    const product2 = JSON.parse(product2Res.body);
    const likeCount2 = product2.likeCount !== undefined ? product2.likeCount : 'N/A';
    console.log(`🔍 상품 #2 likeCount: ${likeCount2} (기대값: 0, 레이스 테스트 후)`);

    if (likeCount2 !== 0 && likeCount2 !== 'N/A') {
      console.warn(`⚠️ 레이스 컨디션 후 likeCount 불일치: 기대값=0, 실제값=${likeCount2}`);
    }
  }

  // DB 직접 확인용 안내
  console.log('');
  console.log('=== DB 검증 쿼리 ===');
  console.log("SELECT id, like_count FROM products WHERE id IN (1, 2);");
  console.log("SELECT product_id, COUNT(*) FROM likes WHERE product_id IN (1, 2) GROUP BY product_id;");
}

// ============================================
// 결과 요약
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

function getMetricAvg(data, name) {
  var m = data.metrics[name];
  if (!m || !m.values) return 'N/A';
  return m.values.avg ? m.values.avg.toFixed(0) + 'ms' : 'N/A';
}

export function handleSummary(data) {
  var totalReqs = getMetricCount(data, 'http_reqs');
  var duration = data.state ? data.state.testRunDurationMs / 1000 : 90;
  var qps = totalReqs > 0 ? (totalReqs / duration).toFixed(1) : 'N/A';

  var lines = [
    '',
    '╔══════════════════════════════════════════════════╗',
    '║      좋아요 부하 + 동시성 테스트 결과              ║',
    '╚══════════════════════════════════════════════════╝',
    '',
    '── 전체 지표 ──',
    '  총 요청 수:         ' + totalReqs,
    '  QPS:               ' + qps + ' req/s',
    '  총 슬로우 (>500ms): ' + getMetricCount(data, 'slow_likes'),
    '  동시성 충돌:        ' + getMetricCount(data, 'concurrency_conflicts'),
    '',
    '── 좋아요 등록 ──',
    '  avg: ' + getMetricAvg(data, 'like_duration'),
    '  p95: ' + getMetricP(data, 'like_duration', 95),
    '  p99: ' + getMetricP(data, 'like_duration', 99),
    '',
    '── 좋아요 취소 ──',
    '  avg: ' + getMetricAvg(data, 'unlike_duration'),
    '  p95: ' + getMetricP(data, 'unlike_duration', 95),
    '  p99: ' + getMetricP(data, 'unlike_duration', 99),
    '',
    '── 좋아요 목록 조회 ──',
    '  avg: ' + getMetricAvg(data, 'like_list_duration'),
    '  p95: ' + getMetricP(data, 'like_list_duration', 95),
    '  p99: ' + getMetricP(data, 'like_list_duration', 99),
    '',
    '── Threshold 결과 ──',
    '  좋아요 p95 < 500ms: ' + (data.metrics.like_duration && data.metrics.like_duration.thresholds ? JSON.stringify(data.metrics.like_duration.thresholds) : 'N/A'),
    '  취소 p95 < 500ms:   ' + (data.metrics.unlike_duration && data.metrics.unlike_duration.thresholds ? JSON.stringify(data.metrics.unlike_duration.thresholds) : 'N/A'),
    '  에러율 < 5%:        ' + (data.metrics.like_errors && data.metrics.like_errors.thresholds ? JSON.stringify(data.metrics.like_errors.thresholds) : 'N/A'),
    '',
  ];

  console.log(lines.join('\n'));
  return {};
}