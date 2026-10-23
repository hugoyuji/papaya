CREATE TABLE auth_login_failures (
    scope VARCHAR(10) NOT NULL,
    identifier_hash CHAR(64) NOT NULL,
    failure_count INTEGER NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT auth_login_failures_pk PRIMARY KEY (scope, identifier_hash),
    CONSTRAINT auth_login_failures_scope_valid CHECK (scope IN ('ACCOUNT', 'IP')),
    CONSTRAINT auth_login_failures_count_positive CHECK (failure_count > 0)
);

CREATE INDEX auth_login_failures_updated_at_idx ON auth_login_failures (updated_at);
