# Contrato de Integración (v1) — ChurnInsight (MVP)

Este contrato es el **único** que deben usar todos los equipos (Front/Clientes → API Spring → Servicio Modelo FastAPI) **sin cambios** durante el hackathon.

---

## Endpoint

**POST** `/predict`  
**Headers:** `Content-Type: application/json`

---

## Request JSON (8 features + ID)

> **ID canónico:** `CustomerId`  
> El modelo usa **exactamente 8 features**: `CreditScore, Geography, Gender, Age, Tenure, Balance, NumOfProducts, IsActiveMember`

### Estructura
```json
{
  "CustomerId": "string",
  "CreditScore": 650,
  "Geography": "Mexico",
  "Gender": 1,
  "Age": 35,
  "Tenure": 12,
  "Balance": 1500.5,
  "NumOfProducts": 2,
  "IsActiveMember": 1
}


TIPOS DE DATO

| Campo          | Tipo    | Reglas                                                   |
| -------------- | ------- | -------------------------------------------------------- |
| CustomerId     | string  | obligatorio, no vacío                                    |
| CreditScore    | integer | >= 0                                                     |
| Geography      | string  | obligatorio, no vacío                                    |
| Gender         | integer | convención fija para el hackathon (ej. 0=female, 1=male) |
| Age            | integer | >= 0                                                     |
| Tenure         | integer | >= 0                                                     |
| Balance        | float   | >= 0                                                     |
| NumOfProducts  | integer | >= 0                                                     |
| IsActiveMember | integer | 0 o 1                                                    |

Campos obligatorios (v1)

Todos los campos del request son obligatorios:

CustomerId
CreditScore
Geography
Gender
Age
Tenure
Balance
NumOfProducts
IsActiveMember

RESPONSE
{
  "CustomerId": "string",
  "PredictedProba": 0.81,
  "PredictedLabel": 1,
  "CustomerSegment": "string",
  "InterventionPriority": "HIGH"
}

REGLAS DE SALIDA
PredictedProba: float entre 0 y 1 (probabilidad de churn)
PredictedLabel: 1 = churn (va a cancelar), 0 = no churn (va a continuar)
InterventionPriority: recomendado HIGH | MEDIUM | LOW
CustomerSegment: string (puede venir "Unknown" si no se calcula)

REQUEST DE PRUEBA

1)Alto riesgo.
{
  "CustomerId": "CUST_HIGH_001",
  "CreditScore": 420,
  "Geography": "Mexico",
  "Gender": 1,
  "Age": 49,
  "Tenure": 2,
  "Balance": 0.0,
  "NumOfProducts": 1,
  "IsActiveMember": 0
}

2)Riesgo medio.
{
  "CustomerId": "CUST_MED_001",
  "CreditScore": 610,
  "Geography": "Mexico",
  "Gender": 0,
  "Age": 34,
  "Tenure": 10,
  "Balance": 850.0,
  "NumOfProducts": 2,
  "IsActiveMember": 1
}

3)Bajo riesgo.
{
  "CustomerId": "CUST_LOW_001",
  "CreditScore": 780,
  "Geography": "Mexico",
  "Gender": 1,
  "Age": 28,
  "Tenure": 36,
  "Balance": 5600.0,
  "NumOfProducts": 3,
  "IsActiveMember": 1
}


