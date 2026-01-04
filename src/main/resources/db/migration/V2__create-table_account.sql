CREATE TABLE account (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    balance NUMERIC(15,2) NOT NULL,
    opened_at DATE NOT NULL,
    closed_at DATE,

    CONSTRAINT fk_account_customer
           FOREIGN KEY (customer_id)
           REFERENCES customer (id)
           ON DELETE CASCADE
   );

   CREATE INDEX idx_account_customer_id
       ON account (customer_id);
);