-- ============================================
-- 추가 Mock Data: 상품 10,000개 추가
-- 기존 3,000건에 더해 총 13,000건
-- Target: PostgreSQL 16
-- ============================================

DO $$
DECLARE
    brand_rec RECORD;
    i INT;
    product_count INT := 0;

    categories TEXT[] := ARRAY['러닝화','스니커즈','워킹화','트레이닝화','농구화','축구화','슬리퍼','부츠','로퍼','샌들'];
    colors TEXT[] := ARRAY[
        'White','Black','Grey','Navy','Red',
        'Blue','Green','Beige','Brown','Pink',
        'Orange','Purple','Cream','Olive','Burgundy',
        'Sky Blue','Charcoal','Sand','Mint','Coral'
    ];

    suffixes TEXT[] := ARRAY['SE','LE','Pro','Elite','Plus','V2','V3','EVO','GTX','Premium',
                             'Retro','OG','Boost','Lite','Max','Ultra','Neo','Flex','X','DX'];

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
    desc_text TEXT;
    cat TEXT;

BEGIN
    FOR brand_rec IN SELECT id, name FROM brands ORDER BY id LOOP

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

        -- 브랜드당 500개 추가 (20 x 500 = 10,000)
        FOR i IN 1..500 LOOP
            model_name := models[1 + ((i - 1) % array_length(models, 1))];
            color_name := colors[1 + ((i - 1) / array_length(models, 1)) % array_length(colors, 1)];
            suffix := suffixes[1 + (i % array_length(suffixes, 1))];
            full_name := brand_rec.name || ' ' || model_name || ' ' || color_name || ' ' || suffix;

            cat := categories[1 + (i % array_length(categories, 1))];

            CASE
                WHEN brand_rec.id IN (7, 17) THEN
                    base_price := 500000 + (floor(random() * 15) * 100000)::INT;
                WHEN brand_rec.id IN (6, 20) THEN
                    base_price := 150000 + (floor(random() * 20) * 10000)::INT;
                WHEN brand_rec.id IN (14, 15, 16) THEN
                    base_price := 140000 + (floor(random() * 15) * 10000)::INT;
                WHEN brand_rec.id IN (8, 9, 11, 12) THEN
                    base_price := 59000 + (floor(random() * 12) * 10000)::INT;
                ELSE
                    base_price := 89000 + (floor(random() * 20) * 10000)::INT;
            END CASE;

            has_sale := random() < 0.4;
            IF has_sale THEN
                sale_p := base_price - (floor(random() * 4 + 1) * 10000)::INT;
                IF sale_p < 30000 THEN sale_p := 30000; END IF;
            ELSE
                sale_p := NULL;
            END IF;

            CASE
                WHEN brand_rec.id IN (7, 17) THEN
                    stock := 5 + floor(random() * 30)::INT;
                WHEN brand_rec.id IN (8, 9, 11, 12) THEN
                    stock := 100 + floor(random() * 400)::INT;
                ELSE
                    stock := 30 + floor(random() * 200)::INT;
            END CASE;

            desc_text := brand_rec.name || ' ' || model_name || ' ' || suffix || ' ' || cat || ' - ' || color_name || ' 컬러';

            INSERT INTO products (brand_id, name, price, sale_price, stock_quantity, like_count, description, created_at, updated_at)
            VALUES (brand_rec.id, full_name, base_price, sale_p, stock, floor(random() * 500)::INT, desc_text,
                    NOW() - (floor(random() * 365) || ' days')::INTERVAL,
                    NOW() - (floor(random() * 30) || ' days')::INTERVAL);

            product_count := product_count + 1;
        END LOOP;
    END LOOP;

    RAISE NOTICE '추가 % 개 상품 삽입 완료', product_count;
END $$;

-- 검증
SELECT '총 브랜드 수' AS label, COUNT(*) AS cnt FROM brands
UNION ALL
SELECT '총 상품 수', COUNT(*) FROM products
UNION ALL
SELECT '세일 상품 수', COUNT(*) FROM products WHERE sale_price IS NOT NULL;

SELECT b.name AS brand, COUNT(p.id) AS product_count,
       MIN(p.price) AS min_price, MAX(p.price) AS max_price,
       AVG(p.price)::INT AS avg_price
FROM brands b
JOIN products p ON b.id = p.brand_id
GROUP BY b.name
ORDER BY b.name;