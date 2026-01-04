CREATE TABLE customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id VARCHAR(100) NOT NULL UNIQUE,
    geography VARCHAR(100) NOT NULL,
    gender INTEGER NOT NULL,
    birth_date DATE NOT NULL,
    created_at DATE NOT NULL
);