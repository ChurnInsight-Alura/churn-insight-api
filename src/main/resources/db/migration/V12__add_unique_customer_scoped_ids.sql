-- Transacciones: evitar duplicados por cliente
ALTER TABLE customer_transaction
ADD CONSTRAINT uk_tx_customer UNIQUE (customer_id, transaction_id);

-- Sesiones: evitar duplicados por cliente
ALTER TABLE customer_session
ADD CONSTRAINT uk_session_customer UNIQUE (customer_id, session_id);