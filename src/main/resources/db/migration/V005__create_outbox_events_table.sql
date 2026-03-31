CREATE TABLE outbox_events (
                               id              UUID            PRIMARY KEY,
                               aggregate_type  VARCHAR(50)     NOT NULL,
                               aggregate_id    VARCHAR(50)     NOT NULL,
                               event_type      VARCHAR(100)    NOT NULL,
                               payload         JSONB           NOT NULL,
                               created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
                               processed_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_unprocessed
    ON outbox_events (created_at ASC)
    WHERE processed_at IS NULL;