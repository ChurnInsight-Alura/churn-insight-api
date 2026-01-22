# Fintech - Customer Churn | Backend API

<img width="1024" height="638" alt="Arquitectura Backend Churn Insight" src="https://github.com/user-attachments/assets/c6570e83-b0dd-474e-86ba-ebe7fee664ac" />

---

## Índice 📋

1. Descripción del proyecto
2. Acceso al proyecto
3. Responsabilidad del Backend
4. Endpoints principales
5. Catálogo de datos (Backend)
6. Manejo de errores y validaciones
7. Migraciones de Base de Datos (Flyway)
8. Tecnologías utilizadas
9. Test Automatizados
10. Cómo ejecutar el proyecto
11. Configuración CORS
12. Agradecimientos 
13. Desarrolladores del proyecto 
14. Licencia y uso del código

---

## 1. Descripción del proyecto 📚

### Contexto
Este repositorio corresponde al **Backend del proyecto Churn Insight**, encargado de orquestar la comunicación entre:

- el **modelo de Machine Learning** expuesto vía **FastAPI**,
- la **base de datos relacional**,
- y los consumidores de la API.

El Backend actúa como **capa intermedia**, asegurando validaciones, persistencia, consistencia de datos y manejo centralizado de errores.

---

### Objetivo
Proveer una **API REST robusta y reutilizable** que permita:

- Ingestar datos de clientes y actividad.
- Ejecutar predicciones de churn individuales y batch.
- Persistir predicciones por periodo (mensual) evitando reprocesamientos.
- Exponer resultados listos para consumo por Frontend.
- Manejar errores de forma clara y consistente.

---

## 2. Acceso al proyecto 📂

### Clonar el repositorio
```bash
git clone https://github.com/ChurnInsight-Alura/churn-insight-api.git
```
### Requisitos
- Java 17+
- Maven
- Base de datos relacional (MySQL)
- Servicio FastAPI levantado (para predicción)

---

## 3. Responsabilidad del Backend 🧠
El Backend NO entrena el modelo de Machine Learning.
Sus responsabilidades principales son:
- Validar requests de entrada.
- Persistir información de clientes, sesiones y transacciones.
- Orquestar llamadas al modelo de churn (FastAPI).
- Persistir predicciones mensuales.
- Evitar reprocesamiento de batches.
- Integrar recomendaciones vía LLM (best-effort).
- Exponer resultados y estadísticas agregadas.
- Proveer contratos claros de error.

---

## 4. Endpoints principales 📡
### Ingesta de datos
 - `POST /ingestion/customer`
 - `POST /ingestion/account`
 - `POST /ingestion/customer-status`
 - `POST /ingestion/product`
 - `POST /ingestion/assign-product`
 - `POST /ingestion/customer-transaction`
 - `POST /ingestion/customer-session`

### Predicción
 - `POST /predict/integration/{customerId}` → Predicción individual
 - `POST /predict/integration` → Predicción vía payload
 - `POST /predict/integration/batch/pro` → Predicción batch persistente
 - `POST /predict/integration/batch/pro/all` → Ejecuta predicciones batch para TODOS los clientes registrados en BD y genera estadisticas

---

## 5. Cálogo de datos (Backend) 📊
### Clientes

| Campo            | Tipo   | Descripción                          |
|------------------|--------|--------------------------------------|
| customerId       | String | Identificador único del cliente      |
| surname          | String | Apellido del cliente                 |
| geography        | String | País de residencia                   |
| gender           | String | Género                               |
| birthDate        | Date   | Fecha de nacimiento                  |
| estimatedSalary  | Double | Salario estimado del cliente         |

### Transacciones

| Campo            | Tipo     | Descripción                       |
|------------------|----------|-----------------------------------|
| transactionId    | String   | Identificador de la transacción   |
| transactionDate  | Datetime | Fecha de la transacción           |
| amount           | Float    | Monto                             |
| transactionType  | String   | Tipo (PAYMENT, TRANSFER, etc.)    |

### Sesiones

| Campo         | Tipo     | Descripción                         |
|---------------|----------|-------------------------------------|
| sessionId     | String   | Identificador de sesión             |
| sessionDate   | Datetime | Fecha de la sesión                  |
| durationMin   | Float    | Duración en minutos                 |
| usedTransfer  | Integer  | Usó transferencia (1/0)             |
| usedPayment   | Integer  | Usó pago (1/0)                      |
| usedInvest    | Integer  | Usó inversión (1/0)                 |
| openedPush    | Integer  | Abrió notificación (1/0)            |
| failedLogin   | Integer  | Falló inicio de sesión (1/0)        |

---

## 6. Manejo de errores y validaciones ⚠️
La API implementa manejo centralizado de errores mediante `GlobalExceptionHandler`.

### Formato estándar
```json
{
  "message": "Descripción del error",
  "code": "CODIGO_ERROR",
  "details": []
}
```
### Códigos soportados
| HTTP | Code               | 
|------|--------------------|
| 400  | VALIDATION_ERROR   | 
| 400  | MALFORMED_JSON     | 
| 404  | NOT_FOUND          |
| 409  | CREATION_ERROR     | 
| 422  | BUSINESS_ERROR     | 
| 502  | DOWNSTREAM_ERROR   | 
| 504  | DOWNSTREAM_TIMEOUT |
| 500  | INTERNAL_ERROR     |

---

## 7. Migraciones de Base de Datos (Flyway) 🗄️
Este proyecto utiliza Flyway para versionar y administrar el esquema de la base de datos.

Las migraciones se encuentran en:
```css
 src/main/resources/db/migration
```
### Incluyen:
- Creación de tablas principales.
- Tablas de batch (batch_run, batch_run_customers).
-  Campos de integración con LLM (ai_insight, ai_insight_status).
- Seed inicial de datos (V13__seed_initial_data.sql).
- Flyway ejecuta automáticamente las migraciones al iniciar la aplicación.

---
## 8. Tecnologías utilizadas 🛠️
- Java 17
- Spring Boot
- Spring Web / WebFlux
- Spring Data JPA
- Bean Validation (Jakarta)
- Flyway
- MySQL
- Maven
- FastAPI (servicio externo)
- LLM (servicio externo)

---

## 9. Tests automatizados 🧪

El Backend cuenta con **tests automatizados** que validan los flujos críticos de negocio y la integración con servicios externos.

### Tipos de tests incluidos

#### 🔹 Tests de cliente FastAPI
- Validan la comunicación con el servicio de predicción (FastAPI).
- Simulan respuestas HTTP exitosas (200 OK).
- Verifican la correcta serialización y deserialización del contrato de integración.
- Aseguran que el cliente HTTP (WebClient) invoque los endpoints esperados del servicio externo

Archivo principal:
- `FastApiClientHackathonTest`

> Nota: el manejo de errores downstream (timeouts, 4xx/5xx) está soportado a nivel de código y puede ser cubierto con tests adicionales.

---

#### 🔹 Tests de flujo de predicción (Service Layer)
- Cubren el flujo completo de predicción:
  - Predicción individual desde base de datos.
  - Predicción batch persistente.
  - Evita reprocesar batches para el mismo periodo.
- Validan:
  - Persistencia de predicciones.
  - Generación de estadísticas agregadas.
  - Comportamiento *best-effort* del LLM (no bloquea el flujo si falla).

Archivo principal:
- `PredictionServiceHackathonFlowTest`

---

### Ejecución de tests

Los tests se ejecutan con Maven desde la raíz del proyecto:

```bash 
mvn test
```
---

## 10. Cómo ejecutar el proyecto ▶️
1. Crear una base de datos MySQL vacía.
2. Configurar application.properties.
3. Levantar FastAPI en http://localhost:8000.
4. Ejecutar:
```bash
 mvn spring-boot:run
```
La API quedará disponible en:
```arduino
 http://localhost:8080
```

---

## 11. Configuración CORS (Desarrollo local) 🌐

Cuando el **Frontend** se ejecuta en un origen distinto al Backend (por ejemplo,
React o Vite en `localhost:3000` o `localhost:5173`), es necesario habilitar CORS
mediante una variable de entorno.

### Activar CORS en desarrollo local

#### Windows (PowerShell)
```powershell
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:5173"
```
#### Windows (CMD)
```bat
set CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```
#### Linux / macOS
```bash
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

---
## 12. Agradecimientos 🤝

Agradecemos especialmente a:

- **Oracle**, por impulsar iniciativas de formación tecnológica de alto impacto.
- **Alura Latam**, por el acompañamiento académico y los contenidos de calidad.
- **NoCountry**, por la coordinación del hackathon y el trabajo colaborativo entre equipos.
- **Programa ONE (Oracle Next Education)**, por fomentar el desarrollo de talento tecnológico.

---

## 13. Desarrolladores del proyecto 👷
- Amalia Anto Alzamora
  - Rol: Backend Developer
- Cindy Jiménez Saldarriaga
  - Rol: Backend Developer
- Jaime Muguruza Cabanillas
  - Rol: Backend Developer
- Luis Isaac Torres
  - Rol: Backend Developer

---

## 14. Licencia y uso del código 📄

Este proyecto fue desarrollado en el marco de un Hackathon educativo, como parte del programa ONE – Oracle Next Education, con el acompañamiento de Alura Latam y NoCountry.
El objetivo del proyecto es educativo y demostrativo, orientado a mostrar un MVP funcional de análisis de churn end-to-end.
El uso del código está orientado a fines académicos y de aprendizaje.  
Para usos comerciales o productivos, se recomienda realizar las validaciones técnicas y legales correspondientes.
Se agradece el reconocimiento del contexto educativo del proyecto y de las organizaciones que lo hicieron posible.

