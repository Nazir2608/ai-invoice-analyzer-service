# AI Invoice Analyzer Service

A production-grade **Invoice Intelligence Platform** built on **Spring Boot 3 / Java 21**.

It ingests invoices, extracts structured data using AI, analyzes risk, applies manual review rules, and exposes analytics dashboards and audit trails.

---

## Table of Contents

- [1. High‑Level Overview](#1-high-level-overview)
- [2. Architecture & Project Structure](#2-architecture--project-structure)
- [3. Core Flows](#3-core-flows)
  - [3.1 Invoice Upload & Async Processing](#31-invoice-upload--async-processing)
  - [3.2 AI Extraction & Risk Analysis](#32-ai-extraction--risk-analysis)
  - [3.3 Dashboard & Analytics](#33-dashboard--analytics)
  - [3.4 Audit Trail](#34-audit-trail)
- [4. Domain Model](#4-domain-model)
- [5. Configuration](#5-configuration)
- [6. API Overview](#6-api-overview)
- [7. Observability & Logging](#7-observability--logging)
- [8. Running the Project Locally](#8-running-the-project-locally)
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

### 3.1 Invoice Upload & Async Processing

High‑level flow for file upload:

```text
Client
  → POST /api/invoices/upload
      ↓
InvoiceController
      ↓
InvoiceService.createFromFile()
  - Store file using StorageStrategy (local filesystem)
  - Create Invoice with status = UPLOADED
  - Publish Kafka event "invoice-uploaded"
      ↓
InvoiceEventProducer (Kafka)
      ↓
InvoiceEventConsumer (Kafka Listener)
  - Idempotency: if invoice.status == COMPLETED → skip
  - Delegate to AiExtractionStrategy.extract(invoiceId)
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

### Profiles

The project primarily uses the `local` profile for development:

```text
spring.profiles.active=local
```

You can add `docker`, `prod`, etc. as needed.

### Key Properties (local)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/invoice_analyzer?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: MySql@123

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

ai:
  provider: ollama   # mock | local | openai | hybrid | ollama
  openai:
    api-key: ${OPENAI_API_KEY:}
  ollama:
    url: http://localhost:11434

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Storage currently uses the local filesystem via `FileSystemStorageService` (`storage.type=local` by default).

---

## 6. API Overview

All responses are wrapped in `ApiResponse<T>`:

- `success`
- `data`
- `message`
- `timestamp`

Base path: `/api/invoices`

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

## 8. Running the Project Locally

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL database `invoice_analyzer`
- Kafka broker on `localhost:9092`
- Optional:
  - Ollama running on `http://localhost:11434` (for `ai.provider=ollama`)
  - An OpenAI API key (for `ai.provider=openai`)

### Commands

```bash
mvn clean install
mvn spring-boot:run
```

Application:

- API base: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

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
- Prometheus/Grafana integration
- Dead Letter Queue (DLQ) and advanced retry policies
- Multi-tenant support

This project is designed as a clean, production‑ready backend foundation for an **Invoice Intelligence Platform**.
