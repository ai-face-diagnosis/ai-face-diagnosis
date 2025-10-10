CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    age INTEGER,
    gender VARCHAR(10)
);

INSERT INTO users (username, email, full_name, active, created_at, updated_at, age, gender)
VALUES (
    'testuser',
    'testuser@example.com',
    'Test User',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    30,
    'MALE'
);