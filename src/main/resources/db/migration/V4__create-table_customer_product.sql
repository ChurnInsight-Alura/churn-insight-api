CREATE TABLE customer_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    CONSTRAINT fk_customer_product_customer
        FOREIGN KEY(customer_id)
        REFERENCES customer(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_customer_product_product
        FOREIGN KEY(product_id)
        REFERENCES product(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_customer_product UNIQUE (customer_id, product_id)
);