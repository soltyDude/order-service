CREATE TABLE order_read_model (
                                  order_id            UUID            PRIMARY KEY,
                                  user_id             UUID            NOT NULL,
                                  status              VARCHAR(30)     NOT NULL,
                                  total_amount        DECIMAL(12, 2)  NOT NULL,
                                  item_count          INTEGER         NOT NULL,
                                  items               JSONB           NOT NULL,
                                  payment_id          VARCHAR(50),
                                  payment_status      VARCHAR(20),
                                  reservation_id      VARCHAR(50),
                                  payment_method      VARCHAR(20)     NOT NULL,
                                  shipping_address    JSONB           NOT NULL,
                                  cancel_reason       VARCHAR(500),
                                  created_at          TIMESTAMPTZ     NOT NULL,
                                  updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_order_read_model_user_id ON order_read_model (user_id);
CREATE INDEX idx_order_read_model_user_id_status ON order_read_model (user_id, status);