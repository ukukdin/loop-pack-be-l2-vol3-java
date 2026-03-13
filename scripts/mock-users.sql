-- 테스트 유저 100명 생성
-- 비밀번호: TestPass99! (SHA256 with salt)
-- Sha256PasswordEncoder 형식: salt:hash

DO $$
DECLARE
    i INT;
    login_id TEXT;
    salt TEXT;
    raw_password TEXT := 'TestPass99!';
    hash_input TEXT;
    encoded TEXT;
BEGIN
    FOR i IN 1..100 LOOP
        login_id := 'k6_like_user_' || i;

        -- salt 생성 (16바이트 랜덤 → base64)
        salt := encode(gen_random_bytes(16), 'base64');

        -- SHA-256(rawPassword + salt) → base64
        hash_input := raw_password || salt;
        encoded := salt || ':' || encode(digest(hash_input, 'sha256'), 'base64');

        INSERT INTO users (user_id, encoded_password, username, birthday, email, wrong_password_count, created_at)
        VALUES (login_id, encoded, '테스트유저' || i, '1990-06-20', 'k6user' || i || '@test.com', 0, NOW())
        ON CONFLICT (user_id) DO NOTHING;
    END LOOP;

    RAISE NOTICE '테스트 유저 100명 생성 완료';
END $$;

SELECT COUNT(*) AS user_count FROM users WHERE user_id LIKE 'k6_like_user_%';
