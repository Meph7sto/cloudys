CREATE TABLE IF NOT EXISTS auth_users (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL
);

INSERT INTO auth_users (user_id, username)
SELECT 'super-admin-001', 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM auth_users WHERE user_id = 'super-admin-001'
);
