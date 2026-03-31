CREATE TABLE order_status_history (
                                      id          UUID            PRIMARY KEY,
                                      order_id    UUID            NOT NULL,
                                      status      VARCHAR(30)     NOT NULL,
                                      timestamp   TIMESTAMPTZ     NOT NULL,

                                      CONSTRAINT fk_order_status_history_order
                                          FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);