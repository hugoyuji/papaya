CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    contributor_trust_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT users_email_not_blank CHECK (email ~ '[^[:space:]]'),
    CONSTRAINT users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT users_display_name_not_blank CHECK (display_name ~ '[^[:space:]]'),
    CONSTRAINT users_password_hash_not_blank CHECK (password_hash ~ '[^[:space:]]'),
    CONSTRAINT users_role_valid CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT users_contributor_trust_status_valid
        CHECK (contributor_trust_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT users_email_unique UNIQUE (email)
);
