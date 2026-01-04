CREATE TABLE customer_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    credit_score integer NOT NULL,
    is_active_member BOOLEAN NOT NULL,

    CONSTRAINT fk_customer_status_customer
        FOREIGN KEY(customer_id)
        REFERENCES customer (id)
        ON DELETE CASCADE
);