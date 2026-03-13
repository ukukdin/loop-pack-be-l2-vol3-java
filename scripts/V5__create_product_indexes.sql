-- ============================================
-- V5: 상품 테이블 Partial Index 생성
-- 목적: 상품 목록/상세 조회 성능 최적화
-- Target: PostgreSQL 16
-- ============================================

-- 1. 브랜드 필터 + 좋아요 순 정렬
-- 용도: GET /api/v1/products?brandId={id}&sort=likes_desc
CREATE INDEX IF NOT EXISTS idx_products_active_brand_likes
    ON products (brand_id, like_count DESC)
    WHERE deleted_at IS NULL;

-- 2. 가격순 정렬
-- 용도: GET /api/v1/products?sort=price_asc / price_desc
CREATE INDEX IF NOT EXISTS idx_products_active_price
    ON products (price ASC)
    WHERE deleted_at IS NULL;

-- 3. 좋아요 순 정렬 (브랜드 필터 없이)
-- 용도: GET /api/v1/products?sort=likes_desc
CREATE INDEX IF NOT EXISTS idx_products_active_likes
    ON products (like_count DESC)
    WHERE deleted_at IS NULL;

-- 4. 최신순 정렬 (기본 정렬)
-- 용도: GET /api/v1/products (기본)
CREATE INDEX IF NOT EXISTS idx_products_active_created
    ON products (created_at DESC)
    WHERE deleted_at IS NULL;
