CREATE TABLE IF NOT EXISTS batch_run (
  id BIGINT NOT NULL AUTO_INCREMENT,
  bucket_date DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  batch_hash VARCHAR(64) NOT NULL,
  stats_json JSON NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_run_bucket_hash (bucket_date, batch_hash)
);

CREATE TABLE IF NOT EXISTS batch_run_customer (
  batch_run_id BIGINT NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (batch_run_id, customer_id),
  CONSTRAINT fk_brc_batch_run
    FOREIGN KEY (batch_run_id) REFERENCES batch_run(id)
    ON DELETE CASCADE
);
