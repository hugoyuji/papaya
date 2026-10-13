ALTER TABLE products
    ADD CONSTRAINT products_source_valid
        CHECK (source IN ('USER', 'EXTERNAL_DATABASE')),
    ADD CONSTRAINT products_verification_status_valid
        CHECK (verification_status IN ('UNVERIFIED', 'PENDING_REVIEW', 'VERIFIED', 'REJECTED'));
