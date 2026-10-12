ALTER TABLE products
    ADD CONSTRAINT products_name_not_blank
        CHECK (name ~ '[^[:space:]]'),
    ADD CONSTRAINT products_barcode_digits_only
        CHECK (barcode IS NULL OR barcode ~ '^[0-9]+$');
