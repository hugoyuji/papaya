CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(200) NOT NULL,
                          source VARCHAR(50) NOT NULL,
                          verification_status VARCHAR(30) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL
);
