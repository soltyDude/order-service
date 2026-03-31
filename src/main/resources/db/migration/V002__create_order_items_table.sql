CREATE TABLE order_items (
                             id          UUID            PRIMARY KEY,
                             order_id    UUID            NOT NULL,
                             product_id  UUID            NOT NULL,
                             quantity    INTEGER         NOT NULL,
                             price       DECIMAL(12, 2)  NOT NULL,
                             subtotal    DECIMAL(12, 2)  NOT NULL,

                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,

                             CONSTRAINT chk_order_items_quantity
                                 CHECK (quantity > 0),

                             CONSTRAINT chk_order_items_price
                                 CHECK (price > 0),

                             CONSTRAINT chk_order_items_subtotal
                                 CHECK (subtotal > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);