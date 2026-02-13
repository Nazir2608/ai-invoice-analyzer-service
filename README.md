#  AI Invoice Analyzer Service

A production-ready, modular monolith backend service built using **Spring Boot 3** for managing and processing invoices with AI-based data extraction and categorization.

This project demonstrates clean architecture, enterprise coding standards, audit support, pagination, strategy patterns, and event-driven processing.

---

#  Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Features](#features)
- [System Flow](#system-flow)
- [Invoice Status Lifecycle](#invoice-status-lifecycle)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Database Design](#database-design)
- [Audit Support](#audit-support)
- [Logging & Exception Handling](#logging--exception-handling)
- [Testing Strategy](#testing-strategy)
- [Running the Project](#running-the-project)
- [Docker Support](#docker-support)
- [Design Patterns Used](#design-patterns-used)
- [Future Enhancements](#future-enhancements)
- [Author](#author)

---

#  Project Overview

AI Invoice Analyzer Service provides:

- 📄 Invoice CRUD operations
- 📤 Invoice file upload (PDF/Image)
- 🤖 AI-based structured data extraction
- 🏷️ Automatic categorization of invoice items
- 📊 Pagination & sorting support
- 📝 Audit tracking (createdAt, createdBy, updatedAt, updatedBy)
- ⚙️ Property-driven AI provider switching
- 🧾 Global exception handling
- 📦 Docker-ready deployment

The system is designed as a **Clean Modular Monolith** and is extensible to microservices in the future.

---

#  Architecture

This project follows **Clean Layered Architecture**:

```
api → application → domain → infrastructure → config
```

## Layer Responsibilities

| Layer | Responsibility |
|--------|----------------|
| api | REST Controllers & DTOs |
| application | Business logic & orchestration |
| domain | Core entities & interfaces |
| infrastructure | AI, storage, messaging implementations |
| config | Configuration & conditional beans |
| exception | Global error handling |

---

#  Project Structure

```
ai-invoice-analyzer-service
 ├── api
 ├── application
 ├── domain
 ├── infrastructure
 ├── config
 ├── exception
 ├── docs
 └── README.md
```

---

#  Features

##  Invoice Management

- Create invoice
- Update invoice
- Delete invoice
- Fetch invoice by ID
- Paginated invoice listing
- Sorting support

---

##  AI Extraction (Strategy Pattern)

Supports multiple AI providers:

```
ai.provider = mock | openai
```

Switch providers without changing code.

---

##  Categorization Strategy

Supports:

- Rule-based categorization
- AI-based categorization

---

##  Pagination & Sorting

Example request:

```
GET /api/invoices?page=0&size=10&sort=createdAt,desc
```

Example response:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10
}
```

---

#  System Flow

## Invoice Creation & Processing Flow

```
Client
   ↓
InvoiceController
   ↓
InvoiceService
   ↓
InvoiceRepository
   ↓
Database
   ↓
InvoiceCreatedEvent
   ↓
InvoiceProcessingOrchestrator
   ↓
AiExtractionStrategy
   ↓
CategorizationStrategy
   ↓
Update Invoice Status → COMPLETED
```

---

#  Invoice Status Lifecycle

Enum values:

- PROCESSING
- COMPLETED
- FAILED

Flow:

```
CREATE → PROCESSING → COMPLETED
```

If error occurs:

```
PROCESSING → FAILED
```

---

#  API Endpoints

## Create Invoice

```
POST /api/invoices
```

Request:

```json
{
  "vendorName": "ABC Store",
  "invoiceNumber": "INV-101"
}
```

---

## Get Invoice

```
GET /api/invoices/{id}
```

---

## List Invoices

```
GET /api/invoices?page=0&size=10
```

---

## Update Invoice

```
PUT /api/invoices/{id}
```

---

## Delete Invoice

```
DELETE /api/invoices/{id}
```

---

## Upload Invoice File

```
POST /api/invoices/{id}/upload
```

---

# ⚙️ Configuration

## Profiles

- local
- prod

Set active profile:

```
spring.profiles.active=local
```

---

## AI Provider Switching

```
ai.provider=mock
```

or

```
ai.provider=openai
```

---

## Storage Switching

```
storage.type=memory
```

or

```
storage.type=s3
```

---

# 🗄️ Database Design

## Table: invoices

| Column | Type |
|---------|--------|
| id | UUID |
| vendor_name | VARCHAR |
| invoice_number | VARCHAR |
| invoice_date | DATE |
| total_amount | DECIMAL |
| status | VARCHAR |
| file_url | VARCHAR |
| created_at | TIMESTAMP |
| created_by | VARCHAR |
| updated_at | TIMESTAMP |
| updated_by | VARCHAR |

---

## Table: invoice_items

| Column | Type |
|---------|--------|
| id | UUID |
| invoice_id | UUID |
| name | VARCHAR |
| quantity | DECIMAL |
| price | DECIMAL |
| category | VARCHAR |

---

#  Audit Support

All entities extend a base auditable class including:

- createdAt
- createdBy
- updatedAt
- updatedBy

Automatically populated using JPA lifecycle hooks.

---

#  Logging & Exception Handling

## Logging

- Uses SLF4J
- Logs invoice creation
- Logs AI processing
- Logs status transitions
- Logs errors

Example:

```
Invoice created with id={}
AI processing started for invoice={}
```

---

## Global Exception Handling

All exceptions are handled centrally.

Example response:

```json
{
  "status": 404,
  "error": "Invoice not found",
  "timestamp": "2026-02-14T10:30:00"
}
```

---

#  Testing Strategy

- Unit tests for service layer
- Integration tests using H2
- Future:
    - TestContainers
    - Kafka integration tests

Run tests:

```
mvn test
```

---

#  Running the Project

## Run Locally

```
mvn clean install
mvn spring-boot:run
```

Application runs at:

```
http://localhost:8080
```

---

#  Docker Support

Future-ready for:

- PostgreSQL
- Kafka
- MinIO
- Application container

Run:

```
docker-compose up --build
```

---

#  Design Patterns Used

| Pattern | Purpose |
|----------|----------|
| Strategy | AI & Categorization |
| Repository | Data access |
| Observer | Event-driven processing |
| Factory (Spring Conditional) | Property-based switching |
| Mapper | DTO conversion |

---

#  Future Enhancements

- Kafka-based async processing
- Retry & Dead Letter Queue
- Multi-tenant support
- JWT security
- Observability (Prometheus & Grafana)
- Idempotency handling
- Duplicate invoice detection

---

#  Author

Nazir  
Backend Engineer | Java | Spring Boot | AI-driven Systems

---

#  Why This Project?

This project demonstrates:

- Clean architecture
- Enterprise backend design
- Extensibility via patterns
- Production readiness
- AI integration capability

Ideal for:

- Senior backend interviews
- SaaS product foundation
- Portfolio showcase

---
