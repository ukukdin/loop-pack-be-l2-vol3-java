import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Passw0rd11';
const ADMIN_HEADER = 'X-Loopers-Ldap';
const ADMIN_VALUE = 'loopers.admin';
const TARGET_PRODUCT_ID = 3; // 테스트 대상 상품

const likeDuration = new Trend('like_duration', true);
const adminUpdateDuration = new Trend('admin_update_duration', true);
const lostUpdateDetected = new Counter('lost_updates_detected');

// ============================================
// Setup: 테스트 유저 생성 + 초기 상태 기록
// ============================================
export function setup() {
  // 상품 #3의 초기 상태 확인
  const productRes = http.get(`${BASE_URL}/api/v1/products/${TARGET_PRODUCT_ID}`);
  const product = JSON.parse(productRes.body);
  console.log(`초기 상태 - 상품 #${TARGET_PRODUCT_ID}: likeCount=${product.likeCount}, price=${product.price}`);

  // 테스트 유저 50명 생성
  const users = [];
  for (let i = 1; i <= 50; i++) {
    const loginId = `lostuser${i}`;
    const res = http.post(
      `${BASE_URL}/api/v1/users`,
      JSON.stringify({
        loginId: loginId,
        password: PASSWORD,
        name: `losttest${i}`,
        birthday: '1990-06-20',
        email: `lost${i}@test.com`,
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 200) users.push(loginId);
  }
  console.log(`생성된 유저: ${users.length}명`);

  return {
    initialLikeCount: product.likeCount || 0,
    initialPrice: product.price,
    productName: product.name,
  };
}

// ============================================
// 시나리오 설정
// ============================================
export const options = {
  scenarios: {
    // 시나리오 1: 유저들이 좋아요를 동시에 누름
    concurrent_likes: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      exec: 'likeProduct',
      tags: { test_type: 'like' },
    },

    // 시나리오 2: 어드민이 동시에 상품 정보를 수정 (가격 변경)
    admin_update: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 3,
      exec: 'adminUpdateProduct',
      tags: { test_type: 'admin_update' },
    },
  },
};

// ============================================
// 시나리오 1: 좋아요 등록
// ============================================
export function likeProduct(data) {
  const userIndex = ((__VU - 1) % 50) + 1;
  const loginId = `lostuser${userIndex}`;
  const headers = {
    'Content-Type': 'application/json',
    'X-Loopers-LoginId': loginId,
    'X-Loopers-LoginPw': PASSWORD,
  };

  group('좋아요 등록', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/products/${TARGET_PRODUCT_ID}/likes`,
      null,
      { headers }
    );
    check(res, { 'like 200': (r) => r.status === 200 });
    likeDuration.add(res.timings.duration);
  });
}

// ============================================
// 시나리오 2: 어드민 상품 수정 (가격만 변경)
// ============================================
export function adminUpdateProduct(data) {
  const headers = {
    'Content-Type': 'application/json',
    [ADMIN_HEADER]: ADMIN_VALUE,
  };

  group('어드민 상품 수정', () => {
    // 먼저 현재 상품 정보 조회
    const getRes = http.get(`${BASE_URL}/api/v1/products/${TARGET_PRODUCT_ID}`);
    const product = JSON.parse(getRes.body);

    // 가격만 변경하여 수정 (다른 필드는 기존 값 유지)
    const newPrice = product.price + 1000;
    const updateBody = JSON.stringify({
      name: product.name,
      price: newPrice,
      salePrice: product.salePrice || null,
      stock: product.stock,
      description: product.description || 'updated',
    });

    const res = http.put(
      `${BASE_URL}/api-admin/v1/products/${TARGET_PRODUCT_ID}`,
      updateBody,
      { headers }
    );
    check(res, { 'admin update 200': (r) => r.status === 200 });
    adminUpdateDuration.add(res.timings.duration);

    if (res.status === 200) {
      console.log(
        `어드민 수정: price ${product.price} → ${newPrice}, 수정 시점 likeCount=${product.likeCount}`
      );
    }
  });

  sleep(0.05); // 약간의 간격
}

// ============================================
// Teardown: 결과 검증
// ============================================
export function teardown(data) {
  console.log('');
  console.log('=== Lost Update 검증 ===');

  // 최종 상품 상태
  const productRes = http.get(`${BASE_URL}/api/v1/products/${TARGET_PRODUCT_ID}`);
  const product = JSON.parse(productRes.body);

  // likes 테이블의 실제 좋아요 수
  // (API로는 확인 불가하므로 likeCount와 비교)
  const finalLikeCount = product.likeCount;
  const expectedLikeCount = data.initialLikeCount + 50; // 50명이 좋아요

  console.log(`초기 likeCount:   ${data.initialLikeCount}`);
  console.log(`기대 likeCount:   ${expectedLikeCount} (초기 + 50명)`);
  console.log(`실제 likeCount:   ${finalLikeCount}`);
  console.log(`최종 price:       ${product.price}`);
  console.log('');

  if (finalLikeCount !== expectedLikeCount) {
    const lost = expectedLikeCount - finalLikeCount;
    console.log(`🚨 LOST UPDATE 감지! ${lost}건의 좋아요가 유실됨`);
    console.log(`   원인: 어드민 수정이 좋아요 카운트를 덮어씀`);
  } else {
    console.log(`✅ likeCount 정상 (${finalLikeCount})`);
  }

  console.log('');
  console.log('=== DB 검증 쿼리 ===');
  console.log(`SELECT like_count FROM products WHERE id = ${TARGET_PRODUCT_ID};`);
  console.log(`SELECT COUNT(*) FROM likes WHERE product_id = ${TARGET_PRODUCT_ID};`);
}

// ============================================
// 결과 요약
// ============================================
export function handleSummary(data) {
  var lines = [
    '',
    '╔══════════════════════════════════════════════╗',
    '║    Lost Update 동시성 테스트 결과              ║',
    '╚══════════════════════════════════════════════╝',
    '',
  ];
  console.log(lines.join('\n'));
  return {};
}
