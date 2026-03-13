-- ============================================
-- Mock Data: 브랜드 20개 + 상품 100,000개
-- Target: PostgreSQL 16
-- 목적: 인덱스 최적화 및 성능 테스트용 대량 데이터
-- ============================================

-- 기존 데이터 정리
TRUNCATE TABLE likes RESTART IDENTITY CASCADE;
TRUNCATE TABLE products RESTART IDENTITY CASCADE;
TRUNCATE TABLE brands RESTART IDENTITY CASCADE;

-- ============================================
-- 1. 브랜드 20개
-- ============================================
INSERT INTO brands (name, description, created_at, updated_at) VALUES
('Nike', '미국 스포츠웨어 브랜드', NOW(), NOW()),
('Adidas', '독일 스포츠웨어 브랜드', NOW(), NOW()),
('Puma', '독일 스포츠 브랜드', NOW(), NOW()),
('New Balance', '미국 러닝화 전문 브랜드', NOW(), NOW()),
('ASICS', '일본 러닝화 전문 브랜드', NOW(), NOW()),
('Timberland', '미국 아웃도어 부츠 브랜드', NOW(), NOW()),
('Prada', '이탈리아 럭셔리 브랜드', NOW(), NOW()),
('Converse', '미국 캔버스 스니커즈 브랜드', NOW(), NOW()),
('Vans', '미국 스케이트보드 슈즈 브랜드', NOW(), NOW()),
('Reebok', '미국 피트니스 브랜드', NOW(), NOW()),
('FILA', '이탈리아 스포츠 브랜드', NOW(), NOW()),
('Skechers', '미국 컴포트 슈즈 브랜드', NOW(), NOW()),
('Under Armour', '미국 퍼포먼스 스포츠 브랜드', NOW(), NOW()),
('Salomon', '프랑스 트레일러닝 브랜드', NOW(), NOW()),
('Hoka', '프랑스 러닝화 브랜드', NOW(), NOW()),
('On Running', '스위스 러닝화 브랜드', NOW(), NOW()),
('Balenciaga', '프랑스 럭셔리 브랜드', NOW(), NOW()),
('Jordan', '나이키 산하 바스켓볼 브랜드', NOW(), NOW()),
('Mizuno', '일본 스포츠 브랜드', NOW(), NOW()),
('Dr. Martens', '영국 부츠 브랜드', NOW(), NOW());

-- ============================================
-- 2. 상품 100,000개 (브랜드당 5,000개)
-- ============================================
-- 분포 설계:
--   like_count: 멱법칙(power-law) — 60% 0~50, 25% 50~500, 10% 500~2000, 5% 2000~10000
--   price: 브랜드 티어별 차등 (럭셔리/프리미엄/대중/일반)
--   sale_price: 40% 확률로 할인가 존재
--   stock: 브랜드 특성별 차등 (럭셔리 소량, 대중 대량)
--   deleted_at: 5% 확률로 소프트 삭제 (WHERE deleted_at IS NULL 필터 테스트용)
--   created_at: 최근 1년에 걸쳐 분포
-- ============================================

DO $$
DECLARE
    brand_rec RECORD;
    i INT;
    product_count INT := 0;

    -- 카테고리 10종
    categories TEXT[] := ARRAY['러닝화','스니커즈','워킹화','트레이닝화','농구화','축구화','슬리퍼','부츠','로퍼','샌들'];

    -- 컬러 20종
    colors TEXT[] := ARRAY[
        'White','Black','Grey','Navy','Red',
        'Blue','Green','Beige','Brown','Pink',
        'Orange','Purple','Cream','Olive','Burgundy',
        'Sky Blue','Charcoal','Sand','Mint','Coral'
    ];

    -- 서픽스 (모델 변형) — 조합 다양성 확보
    suffixes TEXT[] := ARRAY[
        'SE','LE','Pro','Elite','Plus','V2','V3','EVO','GTX','Premium',
        'Retro','OG','Boost','Lite','Max','Ultra','Neo','Flex','X','DX',
        '2024','2025','Limited','Classic','Sport','Tech','Air','Wide','Slim','Mid'
    ];

    -- 사이즈 라벨 (추가 변형)
    size_labels TEXT[] := ARRAY['240','245','250','255','260','265','270','275','280','285','290','295','300'];

    -- 브랜드별 모델 배열 (15개씩)
    nike_models TEXT[] := ARRAY['Air Force 1','Air Max 90','Air Max 97','Dunk Low','Dunk High','Blazer Mid','Cortez','Pegasus 41','Vomero 18','Zoom Fly 6','React Infinity','Waffle One','Air Rift','Free Run 5.0','Structure 25'];
    adidas_models TEXT[] := ARRAY['Samba OG','Gazelle','Ultraboost Light','NMD R1','Forum Low','Stan Smith','Superstar','Ozweego','4DFWD','Adizero Boston 12','Terrex Free Hiker','Continental 80','ZX 750','Campus 00s','Rivalry Low'];
    puma_models TEXT[] := ARRAY['Suede Classic','RS-X','Clyde All-Pro','Palermo','Speedcat OG','Rider FV','Mayze','CA Pro','Morphic','Slipstream','Future Rider','Wild Rider','Deviate Nitro 2','Magnify Nitro 2','Mirage Sport'];
    nb_models TEXT[] := ARRAY['530','993','990v6','2002R','574','327','550','1906R','Fresh Foam X','FuelCell Rebel v4','860v14','1080v13','9060','580','725'];
    asics_models TEXT[] := ARRAY['Gel-Kayano 14','Gel-1130','Gel-Nimbus 26','Gel-NYC','GT-2160','Gel-Cumulus 26','Gel-Quantum 360','Noosa Tri 15','Gel-Sonoma 7','Gel-Venture 9','Gel-Excite 10','Metaspeed Sky+','Gel-Resolution 9','Gel-Trabuco 12','Japan S'];
    timb_models TEXT[] := ARRAY['6-Inch Premium','Euro Sprint','Bradstreet Ultra','Solar Wave','Greyfield','Sprint Trekker','Timberloop','Brooklyn Side Zip','Maple Grove','Atwells Ave','Heritage Chukka','Courma Kid','Adventure 2.0','Euro Hiker','Treeline'];
    prada_models TEXT[] := ARRAY['Cloudbust Thunder','Americas Cup','Monolith','Wheel','Adidas Luna Rossa','Downtown','Rev','Prax','Macro','District','Collision','Knit','Stratus','Linea Rossa','Soft Padded'];
    conv_models TEXT[] := ARRAY['Chuck Taylor All Star','Chuck 70','One Star','Jack Purcell','Run Star Hike','All Star BB','Pro Leather','Weapon','Star Player','Lugged','CONS AS-1','Star Chevron','Fastbreak','ERX','Bishop'];
    vans_models TEXT[] := ARRAY['Old Skool','Sk8-Hi','Authentic','Era','Slip-On','Ultrarange','Knu Skool','Sport 73','Rowley Classic','Style 36','Bold Ni','Varix WC','AVE Pro','Rowan Pro','Half Cab'];
    reebok_models TEXT[] := ARRAY['Club C 85','Instapump Fury 95','Classic Leather','Nano X4','Zig Kinetica','Answer IV','Floatzig','LT Court','BB 4000 II','Club C Extra','Premier Road','Energen','Classic Nylon','Royal Glide','Vector'];
    fila_models TEXT[] := ARRAY['Disruptor 2','Ray Tracer','Oakmont TR','Grant Hill 1','Cage','Renno','Fusion','Luminance','Overtake','Trail Panda','Wavelet','Acd','Dynamico','Grant Hill 2','Teratach 600'];
    skechers_models TEXT[] := ARRAY['D Lites','Go Walk 7','Max Cushioning','Slip-ins','Arch Fit','Skech-Air','Track','Summits','Stamina','Equalizer 5','Flex Appeal','Energy','Relaxed Fit','Go Run','Ultra Flex'];
    ua_models TEXT[] := ARRAY['Curry Flow 10','HOVR Phantom 3','Charged Assert 10','SlipSpeed','Surge 4','Flow Velociti','HOVR Machina 3','Charged Rogue 4','Flow Dynamic','Spawn 6','Lockdown 7','Drive Pro','Infinite Pro','Pursuit 3','Assert 10'];
    salomon_models TEXT[] := ARRAY['XT-6','Speedcross 6','Ultra Glide 2','S/Lab Phantasm','ACS Pro','X Ultra 360','Sense Ride 5','Pulsar Trail','Aero Glide','Cross Hike 2','Alphacross 5','XA Pro 3D V9','Predict Hike','Outpulse','RX Slide 3.0'];
    hoka_models TEXT[] := ARRAY['Clifton 9','Bondi 8','Speedgoat 5','Mach 6','Arahi 7','Kawana 2','Rincon 4','Carbon X 3','Challenger 7','Torrent 3','Gaviota 5','Transport','Tecton X 2','Ora Recovery','Hopara 2'];
    on_models TEXT[] := ARRAY['Cloud 5','Cloudmonster','Cloudrunner 2','Cloudsurfer','Cloudflow 4','Cloudnova','Cloudswift 3','Cloudultra 2','Cloudventure','Roger Pro','Cloudace 3','Cloudgo','Cloudstratus 3','Cloudvista','The Roger Centre'];
    balen_models TEXT[] := ARRAY['Track','Triple S','Speed','Runner','Defender','3XL','Phantom','Rally','Steroid','Tyrex','X-Pander','Drive','Sharkhead','Bouncer','Bulldozer'];
    jordan_models TEXT[] := ARRAY['Air Jordan 1 High OG','Air Jordan 4 Retro','Air Jordan 3','Air Jordan 11','Air Jordan 5','Air Jordan 6','Air Jordan 12','Air Jordan 13','Jumpman MVP','Air Jordan 1 Low','Luka 2','Tatum 2','Zion 3','Why Not .7','Air Jordan 2'];
    mizuno_models TEXT[] := ARRAY['Wave Rider 27','Contender','Wave Inspire 20','Wave Sky 7','Wave Rebellion Pro','Creation 25','Neo Wind','Rebellion Flash 2','Wave Exceed','Morelia Neo IV','Wave Lightning Z8','Cyclone Speed 4','Wave Stealth V','TC-01','Wave Luminous 2'];
    dr_models TEXT[] := ARRAY['1460 8-Eye','1461 3-Eye','Jadon Platform','2976 Chelsea','1490 10-Eye','Sinclair','Audrick','1461 Quad','Combs Tech','1919 Steel Toe','Adrian Tassel','Ramsey','Tarik','Jorge','Bonny Tech'];

    models TEXT[];
    model_name TEXT;
    color_name TEXT;
    suffix TEXT;
    full_name TEXT;
    base_price INT;
    has_sale BOOLEAN;
    sale_p INT;
    stock INT;
    like_cnt INT;
    desc_text TEXT;
    cat TEXT;
    is_deleted BOOLEAN;
    rand_val DOUBLE PRECISION;
    created_ts TIMESTAMP;
    batch_size INT := 500;
    batch_count INT := 0;

BEGIN
    FOR brand_rec IN SELECT id, name FROM brands ORDER BY id LOOP

        -- 브랜드별 모델 배열 선택
        CASE brand_rec.id
            WHEN 1  THEN models := nike_models;
            WHEN 2  THEN models := adidas_models;
            WHEN 3  THEN models := puma_models;
            WHEN 4  THEN models := nb_models;
            WHEN 5  THEN models := asics_models;
            WHEN 6  THEN models := timb_models;
            WHEN 7  THEN models := prada_models;
            WHEN 8  THEN models := conv_models;
            WHEN 9  THEN models := vans_models;
            WHEN 10 THEN models := reebok_models;
            WHEN 11 THEN models := fila_models;
            WHEN 12 THEN models := skechers_models;
            WHEN 13 THEN models := ua_models;
            WHEN 14 THEN models := salomon_models;
            WHEN 15 THEN models := hoka_models;
            WHEN 16 THEN models := on_models;
            WHEN 17 THEN models := balen_models;
            WHEN 18 THEN models := jordan_models;
            WHEN 19 THEN models := mizuno_models;
            WHEN 20 THEN models := dr_models;
        END CASE;

        FOR i IN 1..5000 LOOP
            -- 모델 + 컬러 + 서픽스 조합으로 상품명 생성
            model_name := models[1 + ((i - 1) % array_length(models, 1))];
            color_name := colors[1 + (((i - 1) / array_length(models, 1)) % array_length(colors, 1))];
            suffix := suffixes[1 + (((i - 1) / (array_length(models, 1) * array_length(colors, 1))) % array_length(suffixes, 1))];

            -- 처음 300개(15x20)는 서픽스 없이, 이후는 서픽스 포함
            IF i <= 300 THEN
                full_name := brand_rec.name || ' ' || model_name || ' ' || color_name;
            ELSE
                full_name := brand_rec.name || ' ' || model_name || ' ' || color_name || ' ' || suffix;
            END IF;

            -- 카테고리 순환
            cat := categories[1 + (i % array_length(categories, 1))];

            -- 브랜드 티어별 가격 설정
            CASE
                WHEN brand_rec.id IN (7, 17) THEN  -- Prada, Balenciaga (럭셔리)
                    base_price := 500000 + (floor(random() * 15) * 100000)::INT;
                WHEN brand_rec.id IN (6, 20) THEN  -- Timberland, Dr.Martens (부츠)
                    base_price := 150000 + (floor(random() * 20) * 10000)::INT;
                WHEN brand_rec.id IN (14, 15, 16) THEN  -- Salomon, Hoka, On (프리미엄 러닝)
                    base_price := 140000 + (floor(random() * 15) * 10000)::INT;
                WHEN brand_rec.id IN (18) THEN  -- Jordan (프리미엄 스포츠)
                    base_price := 150000 + (floor(random() * 25) * 10000)::INT;
                WHEN brand_rec.id IN (8, 9, 11, 12) THEN  -- Converse, Vans, FILA, Skechers (대중)
                    base_price := 59000 + (floor(random() * 12) * 10000)::INT;
                ELSE  -- 나머지 일반 스포츠 (Nike, Adidas, Puma, NB, ASICS, Reebok, UA, Mizuno)
                    base_price := 89000 + (floor(random() * 20) * 10000)::INT;
            END CASE;

            -- 40% 확률로 세일가 존재
            has_sale := random() < 0.4;
            IF has_sale THEN
                sale_p := base_price - (floor(random() * 4 + 1) * 10000)::INT;
                IF sale_p < 30000 THEN sale_p := 30000; END IF;
            ELSE
                sale_p := NULL;
            END IF;

            -- 재고: 브랜드 특성 반영
            CASE
                WHEN brand_rec.id IN (7, 17) THEN  -- 럭셔리: 소량
                    stock := 5 + floor(random() * 30)::INT;
                WHEN brand_rec.id IN (8, 9, 11, 12) THEN  -- 대중: 대량
                    stock := 100 + floor(random() * 400)::INT;
                ELSE  -- 일반
                    stock := 30 + floor(random() * 200)::INT;
            END CASE;

            -- like_count: 멱법칙 분포 (현실적인 좋아요 분포)
            -- 60% → 0~50, 25% → 50~500, 10% → 500~2000, 5% → 2000~10000
            rand_val := random();
            CASE
                WHEN rand_val < 0.60 THEN  -- 60%: 비인기 상품
                    like_cnt := floor(random() * 51)::INT;
                WHEN rand_val < 0.85 THEN  -- 25%: 보통 인기
                    like_cnt := 50 + floor(random() * 451)::INT;
                WHEN rand_val < 0.95 THEN  -- 10%: 인기 상품
                    like_cnt := 500 + floor(random() * 1501)::INT;
                ELSE  -- 5%: 바이럴 상품
                    like_cnt := 2000 + floor(random() * 8001)::INT;
            END CASE;

            -- 설명
            desc_text := brand_rec.name || ' ' || model_name || ' ' || cat || ' - ' || color_name || ' 컬러';

            -- created_at: 최근 1년에 걸쳐 분포
            created_ts := NOW() - (floor(random() * 365) || ' days')::INTERVAL
                              - (floor(random() * 24) || ' hours')::INTERVAL;

            -- 5% 확률로 소프트 삭제 (deleted_at IS NULL 조건 테스트용)
            is_deleted := random() < 0.05;

            INSERT INTO products (brand_id, name, price, sale_price, stock_quantity, like_count, description, created_at, updated_at, deleted_at)
            VALUES (
                brand_rec.id,
                full_name,
                base_price,
                sale_p,
                stock,
                like_cnt,
                desc_text,
                created_ts,
                created_ts + (floor(random() * 30) || ' days')::INTERVAL,
                CASE WHEN is_deleted THEN NOW() - (floor(random() * 30) || ' days')::INTERVAL ELSE NULL END
            );

            product_count := product_count + 1;
        END LOOP;

        RAISE NOTICE '브랜드 [%] 5,000개 삽입 완료 (누적: %건)', brand_rec.name, product_count;
    END LOOP;

    RAISE NOTICE '===== 총 %개 상품 삽입 완료 =====', product_count;
END $$;

-- ============================================
-- 3. 검증 쿼리
-- ============================================

-- 전체 요약
SELECT '총 브랜드 수' AS label, COUNT(*) AS cnt FROM brands
UNION ALL
SELECT '총 상품 수', COUNT(*) FROM products
UNION ALL
SELECT '활성 상품 수 (deleted_at IS NULL)', COUNT(*) FROM products WHERE deleted_at IS NULL
UNION ALL
SELECT '삭제 상품 수 (deleted_at IS NOT NULL)', COUNT(*) FROM products WHERE deleted_at IS NOT NULL
UNION ALL
SELECT '세일 상품 수', COUNT(*) FROM products WHERE sale_price IS NOT NULL;

-- 브랜드별 상품 수 및 가격 분포
SELECT b.name AS brand, COUNT(p.id) AS product_count,
       MIN(p.price) AS min_price, MAX(p.price) AS max_price,
       AVG(p.price)::INT AS avg_price,
       COUNT(*) FILTER (WHERE p.deleted_at IS NULL) AS active_count
FROM brands b
JOIN products p ON b.id = p.brand_id
GROUP BY b.name
ORDER BY b.name;

-- like_count 분포 확인
SELECT
    CASE
        WHEN like_count BETWEEN 0 AND 50 THEN '0-50 (비인기)'
        WHEN like_count BETWEEN 51 AND 500 THEN '51-500 (보통)'
        WHEN like_count BETWEEN 501 AND 2000 THEN '501-2000 (인기)'
        ELSE '2001+ (바이럴)'
    END AS like_range,
    COUNT(*) AS cnt,
    ROUND(COUNT(*)::NUMERIC / (SELECT COUNT(*) FROM products) * 100, 1) AS pct
FROM products
GROUP BY 1
ORDER BY MIN(like_count);

-- 가격대별 분포
SELECT
    CASE
        WHEN price < 100000 THEN '~10만'
        WHEN price < 200000 THEN '10~20만'
        WHEN price < 300000 THEN '20~30만'
        WHEN price < 500000 THEN '30~50만'
        ELSE '50만+'
    END AS price_range,
    COUNT(*) AS cnt,
    ROUND(COUNT(*)::NUMERIC / (SELECT COUNT(*) FROM products) * 100, 1) AS pct
FROM products
GROUP BY 1
ORDER BY MIN(price);
