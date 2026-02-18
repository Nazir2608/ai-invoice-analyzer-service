# AI Invoice Analyzer Service

A production-grade **Invoice Intelligence Platform** built on **Spring Boot 3 / Java 21**.

It ingests invoices, extracts structured data using AI, analyzes risk, applies manual review rules, and exposes analytics dashboards and audit trails.

---

## Table of Contents

- [1. High‑Level Overview](#1-high-level-overview)
- [2. Architecture & Project Structure](#2-architecture--project-structure)
- [3. Core Flows](#3-core-flows)
  - [3.1 Invoice Upload & Processing Modes](#31-invoice-upload--processing-modes)
  - [3.2 AI Extraction & Risk Analysis](#32-ai-extraction--risk-analysis)
  - [3.3 Dashboard & Analytics](#33-dashboard--analytics)
  - [3.4 Audit Trail](#34-audit-trail)
- [4. Domain Model](#4-domain-model)
- [5. Configuration](#5-configuration)
- [6. API Overview](#6-api-overview)
  - [6.1 OpenAPI / Swagger UI](#61-openapi--swagger-ui)
- [7. Observability & Logging](#7-observability--logging)
- [8. Running the Project](#8-running-the-project)
  - [8.1 Local Mode (no Kafka, local AI)](#81-local-mode-no-kafka-local-ai)
  - [8.2 Docker Mode (Kafka + Ollama/OpenAI)](#82-docker-mode-kafka--ollamaopenai)
- [9. Design Patterns](#9-design-patterns)
- [10. Future Enhancements](#10-future-enhancements)

---

## 1. High‑Level Overview

This service provides:

- Invoice CRUD APIs
- Invoice file upload (PDF/Office documents)
- AI-based field extraction (invoice number, vendor, amounts, dates, etc.)
- Risk analysis (duplicate detection, amount/tax mismatch, overdue detection)
- AI confidence scoring and **manual review flow**
- Dashboard metrics for operations teams
- Full audit trail of processing events
- Kafka-based asynchronous processing
- Health, metrics, and structured logs for production observability

Tech stack:

- **Java 21**, **Spring Boot 3.2**
- **MySQL** (JPA/Hibernate)
- **Kafka** (producer + consumer)
- **Ollama / OpenAI / Local Regex** for extraction
- **Maven** for build

---

## 2. Architecture & Project Structure

The codebase is organized as a **clean modular monolith**:

```text
api → application → domain → infrastructure → config → exception
```

### Layer Responsibilities

| Layer        | Responsibility                                                |
|-------------|----------------------------------------------------------------|
| `api`       | REST controllers, DTOs, API response wrapper                  |
| `application` | Use cases, services, orchestration, risk rules              |
| `domain`    | Entities, enums, repositories, core business abstractions     |
| `infrastructure` | AI clients, storage implementation, Kafka, text extraction |
| `config`    | Bean wiring, strategy selection (`ai.provider`, etc.)         |
| `exception` | Global error handling and API error model                     |

### Package Structure (simplified)

```text
src/main/java/com/nazir/aiinvoice
 ├── api
 │   ├── controller      # REST endpoints (InvoiceController)
 │   └── dto             # Request/response DTOs
 ├── application
 │   ├── mapper          # Mapping JSON/DTOs <-> domain
 │   └── service         # InvoiceService, InvoiceRiskService, InvoiceEventService, ...
 ├── domain
 │   ├── model           # Invoice, InvoiceItem, InvoiceEvent, InvoiceStatus, ...
 │   └── repository      # Spring Data JPA repositories
 ├── infrastructure
 │   ├── ai              # LocalRegex, OpenAI, Ollama, Hybrid, Mock extraction services
 │   ├── kafka           # InvoiceEventProducer, InvoiceEventConsumer
 │   └── storage         # FileSystemStorageService (local filesystem)
 ├── config              # AiStrategyConfig and other wiring
└── exception           # Global exception handling
```

---

## 3. Core Flows

### 3.1 Invoice Upload & Processing Modes

The upload flow is the same entrypoint but can run in two modes depending on configuration.

#### Local mode (no Kafka, fully in‑process)

Used for simple local development with no Kafka or Docker.

```text
Client
  → POST /api/invoices/upload
      ↓
InvoiceController
      ↓
InvoiceService.createFromFile()
  - Store file via StorageStrategy (local filesystem)
  - Create Invoice with status = UPLOADED
  - Publish InvoiceCreatedEvent (domain event)
      ↓
InvoiceProcessingOrchestrator (AFTER_COMMIT, @Async)
  - Calls AiExtractionStrategy.extract(invoiceId)
      ↓
AiExtractionStrategy (local / openai / ollama)
  - Updates invoice fields
  - Invokes InvoiceRiskService
  - Progresses status to COMPLETED
```

#### Kafka / Docker mode (event‑driven)

Used when Kafka is enabled (e.g. Docker stack with Ollama/OpenAI).

```text
Client
  → POST /api/invoices/upload
      ↓
InvoiceController
      ↓
InvoiceService.createFromFile()
  - Store file via StorageStrategy (local filesystem)
  - Create Invoice with status = UPLOADED
  - Publish InvoiceUploadedEvent
      ↓
InvoiceUploadKafkaPublisher (AFTER_COMMIT)
  - Sends Kafka message to topic "invoice-uploaded"
      ↓
InvoiceEventConsumer (Kafka Listener)
  - Idempotency: if invoice.status == COMPLETED → skip
  - Delegate to AiExtractionStrategy.extract(invoiceId)
      ↓
AiExtractionStrategy (local / openai / ollama)
  - Same extraction + risk flow as above
```

### 3.2 AI Extraction & Risk Analysis

AI extraction is routed via a strategy configured in `AiStrategyConfig` and selected using the property `ai.provider`:

```text
ai.provider = local | openai | ollama
```

Supported strategies:

- `local` – regex‑based extractor (no external AI dependency, runs fully locally)
- `openai` – calls OpenAI Chat Completions API
- `ollama` – calls local Ollama instance over HTTP (Docker setup)

Extraction flow (for AI providers):

```text
1. Set invoice.status = PROCESSING
2. Extract raw text from file (PDF/Word/etc.)
3. Call AI model → JSON with invoice fields + confidenceScore
4. Map JSON to Invoice (InvoiceJsonMapper)
5. Apply risk checks (InvoiceRiskService)
   - Duplicate detection
   - Amount/tax mismatch
   - Overdue payment
   - Low AI confidence
6. Update manual review flags (requiresManualReview, reviewReason)
7. Progress status:
   UPLOADED → PROCESSING → AI_COMPLETED → RISK_ANALYZED → COMPLETED
```

**AI Confidence & Manual Review Rules**

The `Invoice` entity includes:

- `Integer aiConfidenceScore`
- `Boolean requiresManualReview`
- `String reviewReason`

Manual review is triggered when:

- Duplicate invoice detected
- Tax/amount mismatch
- `aiConfidenceScore < 70`

### 3.3 Dashboard & Analytics

Endpoint:

```http
GET /api/invoices/dashboard
```

Response model (`DashboardResponse`):

- `totalInvoices` – total count of invoices
- `totalAmount` – sum of all invoice totals
- `duplicateCount` – invoices flagged as possible duplicates
- `overdueCount` – overdue invoices
- `requiresReviewCount` – invoices that need manual review or are not fully completed

### 3.4 Audit Trail

Every important step writes to `invoice_event` via `InvoiceEventService`:

- Entity: `InvoiceEvent` (id, invoiceId, eventType, message, timestamps)
- Enum: `InvoiceEventType` (e.g. `FILE_UPLOADED`, `TEXT_EXTRACTED`, `AI_STARTED`, `AI_COMPLETED`, `RISK_FLAG_DUPLICATE`, `RISK_FLAG_TAX_MISMATCH`, `PROCESSING_FAILED`, ...)

This gives a **processing timeline** for each invoice.

---

## 4. Domain Model

### Invoice

Key fields:

- `id` (UUID)
- `vendorName`, `invoiceNumber`, `invoiceDate`, `dueDate`
- `subtotal`, `taxAmount`, `totalAmount`, `currency`
- `status` (`InvoiceStatus` enum)
  - `UPLOADED`, `PROCESSING`, `AI_COMPLETED`, `RISK_ANALYZED`, `COMPLETED`, `FAILED`
- `riskFlag`, `paymentStatus`
- `aiConfidenceScore`, `requiresManualReview`, `reviewReason`
- `fileUrl`, `extractedRawText`, `aiSummary`
- Audit fields from `BaseAuditableEntity`

### InvoiceItem

- `id` (UUID)
- `invoice` (Many‑to‑One to `Invoice`)
- `name`, `quantity`, `price`, `lineTotal`, `category`

### InvoiceEvent

- `id` (UUID)
- `invoiceId` (UUID)
- `eventType` (`InvoiceEventType`)
- `message`
- Audit timestamps

---

## 5. Configuration

## 5. Configuration

### Profiles

- Default dev profile: `local` (see `src/main/resources/application-local.yml`).
- Docker profile: `docker` (set via `SPRING_PROFILES_ACTIVE=docker` in `docker-compose.yml`).

### AI provider and Kafka matrix

The main toggles:

- `ai.provider` – which extraction strategy to use.
- `app.kafka-enabled` – whether Kafka integration is active.

| Mode            | Profile  | `ai.provider` | `app.kafka-enabled` | Kafka required | External AI        |
|----------------|----------|---------------|---------------------|----------------|--------------------|
| Local only      | `local`  | `local`       | `false`             | No             | No                 |
| Local + Kafka   | `local`  | `local`       | `true`              | Yes            | No                 |
| OpenAI (local)  | `local`  | `openai`      | `true` or `false`   | Optional       | OpenAI API         |
| Ollama (local)  | `local`  | `ollama`      | `true` or `false`   | Optional       | Local Ollama       |
| Docker + Ollama | `docker` | `ollama`      | `true` (default)    | Yes (container)| Ollama container   |
| Docker + OpenAI | `docker` | `openai`      | `true` (default)    | Yes (container)| OpenAI API         |

If `app.kafka-enabled=false`, all Kafka producer/consumer beans are disabled and file upload goes through the in‑process `InvoiceProcessingOrchestrator`.

### Local profile configuration (excerpt)

From `application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/invoice_analyzer?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: MySql@123

  jpa:
    hibernate:
      ddl-auto: update

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: invoice-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  kafka-enabled: false

ai:
  provider: local          # local | openai | ollama
  openai:
    api-key: ${OPENAI_API_KEY:}
    model: gpt-3.5-turbo
  ollama:
    url: http://localhost:11434
    model: gemma3:4b

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Storage uses the local filesystem via `FileSystemStorageService` by default.

### Docker / docker-compose configuration (excerpt)

From `docker-compose.yml`:

```yaml
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: MySql@123
      MYSQL_DATABASE: invoice_analyzer
    ports:
      - "3308:3306"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: invoice-kafka
    ports:
      - "9092:9092"

  ollama:
    image: ollama/ollama:latest
    container_name: invoice-ollama
    ports:
      - "11434:11434"

  invoice-app:
    build: .
    container_name: ai-invoice-analyzer-service
    environment:
      SPRING_PROFILES_ACTIVE: docker

      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/invoice_analyzer?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: MySql@123
      SPRING_JPA_HIBERNATE_DDL_AUTO: update

      SPRING_KAFKA_BOOTSTRAP_SERVERS: invoice-kafka:9092

      # AI provider for Docker mode
      AI_PROVIDER: ollama          # local | openai | ollama
      AI_OLLAMA_URL: http://ollama:11434
      AI_OLLAMA_MODEL: gemma3:4b

    ports:
      - "8080:8080"
```

To use OpenAI in Docker:

- Change `AI_PROVIDER: openai`.
- Add `OPENAI_API_KEY` to `invoice-app.environment`.

---

## 6. API Overview

All responses are wrapped in `ApiResponse<T>`:

- `success`
- `data`
- `message`
- `timestamp`

Base path: `/api/invoices`

### 6.1 OpenAPI / Swagger UI

The project uses Springdoc OpenAPI to expose machine-readable API docs and an interactive Swagger UI.

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui/index.html`

When running locally:

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

When running via Docker compose:

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Create Invoice

```http
POST /api/invoices
Content-Type: application/json
```

```json
{
  "vendorName": "ABC Store",
  "invoiceNumber": "INV-101"
}
```

### Get Invoice

```http
GET /api/invoices/{id}
```

### List Invoices (paged)

```http
GET /api/invoices?page=0&size=10
```

### Update Invoice

```http
PUT /api/invoices/{id}
```

### Delete Invoice

```http
DELETE /api/invoices/{id}
```

### Upload Invoice File

```http
POST /api/invoices/upload
Content-Type: multipart/form-data
```

Form field:

- `file`: invoice file (PDF/doc)

### Dashboard

```http
GET /api/invoices/dashboard
```

---

## 7. Observability & Logging

### Actuator

With `spring-boot-starter-actuator` and the management config, you get:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus` (for Prometheus scraping)

### Prometheus & Grafana (Docker)

When you run via `docker compose up --build`, the stack also includes:

- **Prometheus** (`invoice-prometheus`) on `http://localhost:9090`
- **Grafana** (`invoice-grafana`) on `http://localhost:3000`

Prometheus is configured via `prometheus.yml` to scrape:

- Target: `invoice-app:8080`
- Path: `/actuator/prometheus`

In the Docker profile, actuator exposure is enabled via the environment variable:

```yaml
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,metrics,prometheus
```

Basic Grafana setup:

1. Open `http://localhost:3000` in your browser.
2. Log in with:
   - User: `admin`
   - Password: `admin`
3. Add a Prometheus data source:
   - Type: Prometheus
   - URL: `http://prometheus:9090`
4. Save & Test.

You can now build dashboards using metrics such as:

- `invoice_upload_consumed_total`
- `invoice_ollama_extraction_seconds_count`
- `invoice_ollama_extraction_seconds_sum`
- `invoice_duplicate_total`

Example panels you can create:

- Active DB connections
- HTTP requests per second
- Kafka consumer lag
- Live JVM threads
- Process CPU %
- JVM heap usage

These are useful for Docker/Kubernetes health checks and monitoring.

### Logging

- Event‑centric log style such as:
  - `event=invoice_upload_request`
  - `event=invoice_uploaded_published`
  - `event=invoice_uploaded_consumed`
  - `event=openai_extraction_started`
  - `event=duplicate_message_skipped`
- Makes it easy to trace invoice lifecycles in logs and debug issues.

---

## 8. Running the Project

This section covers both local setup (no Docker) and full Docker setup.

### 8.1 Local Mode (no Kafka, local AI)

Best for getting started quickly without running Kafka or Ollama.

**Prerequisites**

- Java 21
- Maven 3.9+
- MySQL database `invoice_analyzer` on `localhost:3306`

**Configuration**

- `spring.profiles.active=local` (already set in `application.yml`).
- In `application-local.yml`:
  - `app.kafka-enabled: false`
  - `ai.provider: local`

**Commands**

```bash
git clone https://github.com/<Nazir2608>/ai-invoice-analyzer-service.git
cd ai-invoice-analyzer-service

mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Application:

- API base: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

In this mode:

- File upload calls `LocalRegexExtractionService`.
- Kafka beans are disabled; no connection is made to `localhost:9092`.

### 8.2 Docker Mode (Kafka + Ollama/OpenAI)

This mode runs the full stack (MySQL + Kafka + Ollama + app) using Docker.

**Prerequisites**

- Docker and Docker Compose installed.

**Commands**

From the project root:

```bash
docker compose up --build
```

This starts:

- `invoice-mysql` (MySQL 8, port `3308` on host).
- `invoice-zookeeper` + `invoice-kafka` (Kafka broker on `localhost:9092`).
- `invoice-ollama` (Ollama server on `http://localhost:11434`).
- `ai-invoice-analyzer-service` (Spring Boot app on `http://localhost:8080`).

**Default Docker AI mode (Ollama)**

- `AI_PROVIDER=ollama`
- `AI_OLLAMA_URL=http://ollama:11434`
- `AI_OLLAMA_MODEL=gemma3:4b`

Inside the `invoice-ollama` container, pull the model once:

```bash
docker exec -it invoice-ollama ollama pull gemma3:4b
```

**Switching to OpenAI in Docker**

Edit `docker-compose.yml` `invoice-app.environment`:

```yaml
AI_PROVIDER: openai
OPENAI_API_KEY: your-key-here
```

Then restart:

```bash
docker compose down
docker compose up --build
```

Now AI extraction uses OpenAI instead of Ollama.

---

## 9. Design Patterns

| Pattern   | Purpose                                                     |
|----------|-------------------------------------------------------------|
| Strategy | `AiExtractionStrategy`, `CategorizationStrategy`           |
| Repository | Data access via Spring Data JPA                          |
| Observer | Event-driven processing via Kafka and domain events        |
| Factory  | `AiStrategyConfig` selects AI provider based on properties |
| Mapper   | DTO and JSON mapping (`InvoiceMapper`, `InvoiceJsonMapper`)|

---

## 10. Future Enhancements

- Rich UI dashboard for finance/operations teams
- JWT/OAuth2 security
- Dead Letter Queue (DLQ) and advanced retry policies
- Multi-tenant support

This project is designed as a clean, production‑ready backend foundation for an **Invoice Intelligence Platform**.
