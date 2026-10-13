ALTER TABLE products
    ADD CONSTRAINT products_name_not_blank
        CHECK (name ~ '[^[:space:]]');
