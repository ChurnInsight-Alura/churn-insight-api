ALTER TABLE prediction
ADD COLUMN ai_insight_status VARCHAR(10) NOT NULL DEFAULT 'MISSING';


UPDATE prediction
SET ai_insight_status =
  CASE
    WHEN ai_insight IS NULL OR ai_insight = '' THEN 'MISSING'
    WHEN ai_insight LIKE 'No se pudo generar el análisis en este momento.%' THEN 'ERROR'
    WHEN ai_insight LIKE '%"error"%' THEN 'ERROR'
    ELSE 'OK'
  END;
