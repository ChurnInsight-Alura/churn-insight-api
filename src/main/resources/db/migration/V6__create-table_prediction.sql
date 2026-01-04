CREATE TABLE prediction (
    id BIGINT NOT NULL AUTO_INCREMENT,

    customer_id BIGINT NOT NULL,

    predicted_proba DOUBLE NOT NULL,
    predicted_label INT NOT NULL,

    intervention_priority VARCHAR(20) NOT NULL,
    customer_segment VARCHAR(50),

    prediction_date DATE NOT NULL,
    predicted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_prediction_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_prediction_customer_day
        UNIQUE (customer_id, prediction_date)
);
