# 🚀 Churn Insight – Pull Request

## 📌 Descripción
Se integró un LLM para generar recomendaciones de retención (`aiInsight`) a partir de:
1) la “ficha”/contexto del cliente (features usadas en la predicción) y
2) la predicción del modelo (probabilidad, label, segmento, prioridad).

La integración NO modifica FastAPI (prohibido), solo consume sus endpoints existentes desde el backend Java.

## 🧩 Componente del proyecto
- [x] API – Backend / Servicio de predicción
- [ ] ML – Modelo predictivo
- [ ] APP – Frontend / Visualización

## 🎯 Objetivo del cambio
- Añadir `aiInsight` generado por LLM como valor agregado en las predicciones.
- Guardar `aiInsight` en BD sin romper el flujo si la API key falla.
- Agregar `aiInsightStatus` para que Front sepa si mostrar la tarjeta o un estado de error.

## 📊 Impacto en el MVP
- ¿Afecta la predicción de churn? No (la predicción viene del FastAPI igual)
- ¿Cambia el contrato JSON? Sí (se agregan `aiInsight` y `aiInsightStatus` en DataPredictionDetail)
- ¿Requiere actualización en README? Opcional (variables `llm.api.url` / `llm.api.key`)

## 🔧 Cambios principales (archivos)
- + `src/main/java/com/alura/churnnsight/client/LlmClient.java`
- * `src/main/java/com/alura/churnnsight/service/PredictionService.java`
- * `src/main/java/com/alura/churnnsight/controller/PredictionController.java`
- * `src/main/java/com/alura/churnnsight/dto/consult/DataPredictionDetail.java`
- * `src/main/java/com/alura/churnnsight/model/Prediction.java`
- * `src/main/resources/application.properties`
- + Flyway migration: `V10__add_ai_insight_status.sql` (agrega `ai_insight_status` y backfill)

## ✅ Checklist
- [x] El código corre correctamente
- [x] No rompe funcionalidades existentes
- [x] Mantiene coherencia con el MVP
- [x] Código claro y entendible para el equipo

## 🧪 Cómo probé
- POST `/predict/integration/{customerId}` y GET `/predict/{customerId}/latest`
- Con API key válida: `aiInsightStatus=OK` y `aiInsight` como JSON de estrategia
- Sin API key / key inválida: `aiInsightStatus=ERROR` y `aiInsight` como JSON de error, sin romper el endpoint
