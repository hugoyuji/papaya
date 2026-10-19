CREATE TABLE product_moderation_events (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT,
    product_id_snapshot BIGINT NOT NULL,
    actor_user_id BIGINT,
    actor_user_id_snapshot BIGINT,
    actor_display_name_snapshot VARCHAR(100),
    action VARCHAR(30) NOT NULL,
    reason VARCHAR(1000),
    product_snapshot JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT product_moderation_events_product_fk
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    CONSTRAINT product_moderation_events_actor_fk
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT product_moderation_events_action_valid
        CHECK (action IN ('SUBMITTED', 'RESUBMITTED', 'AUTO_APPROVED', 'APPROVED', 'REJECTED')),
    CONSTRAINT product_moderation_events_rejection_reason_required
        CHECK (action <> 'REJECTED' OR (reason IS NOT NULL AND reason ~ '[^[:space:]]')),
    CONSTRAINT product_moderation_events_snapshot_not_empty
        CHECK (product_snapshot <> '{}'::jsonb)
);

CREATE INDEX product_moderation_events_product_timeline_idx
    ON product_moderation_events (product_id, occurred_at, id);

CREATE INDEX product_moderation_events_actor_idx
    ON product_moderation_events (actor_user_id);
