CREATE TABLE orders (
                        id              UUID            PRIMARY KEY,
                        user_id         UUID            NOT NULL,
                        status          VARCHAR(30)     NOT NULL,
                        total_amount    DECIMAL(12, 2)  NOT NULL,
                        shipping_address JSONB           NOT NULL,
                        payment_method  VARCHAR(20)     NOT NULL,
                        payment_id      VARCHAR(50),
                        reservation_id  VARCHAR(50),
                        cancel_reason   VARCHAR(500),
                        version         INTEGER         NOT NULL DEFAULT 0,
                        created_at      TIMESTAMPTZ     NOT NULL,
                        updated_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_user_id_status ON orders (user_id, status);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);