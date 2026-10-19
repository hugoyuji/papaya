ALTER TABLE products
    ADD COLUMN created_by_user_id BIGINT;

ALTER TABLE products
    ADD CONSTRAINT products_created_by_user_fk
        FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX products_created_by_user_idx ON products (created_by_user_id);
