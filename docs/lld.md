# 📘 Low Level Design (LLD)
## Project: ai-invoice-analyzer-service

---

# 1. Architecture

Layered architecture:

api → application → domain → infrastructure

---

# 2. Database Design

## 2.1 Table: invoices

| Column | Type | Notes |
|---------|--------|--------|
| id | UUID | Primary Key |
| vendor_name | VARCHAR | Required |
| invoice_number | VARCHAR | Optional |
| invoice_date | DATE | |
| total_amount | DECIMAL | |
| status | VARCHAR | Enum |
| file_url | VARCHAR | |
| created_at | TIMESTAMP | |
| created_by | VARCHAR | |
| updated_at | TIMESTAMP | |
| updated_by | VARCHAR | |

Indexes:
- vendor_name
- created_at

---

## 2.2 Table: invoice_items

| Column | Type | Notes |
|---------|--------|--------|
| id | UUID | Primary Key |
| invoice_id | UUID | FK |
| name | VARCHAR | |
| quantity | DECIMAL | |
| price | DECIMAL | |
| category | VARCHAR | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

---

# 3. Domain Classes

## 3.1 BaseAuditableEntity

Fields:
- createdAt
- createdBy
- updatedAt
- updatedBy

---

## 3.2 Invoice

Fields:
- id
- vendorName
- invoiceNumber
- invoiceDate
- totalAmount
- status (Enum)
- fileUrl

---

## 3.3 InvoiceItem

Fields:
- id
- name
- quantity
- price
- category
- invoice reference

---

# 4. Service Design

## InvoiceService

Responsibilities:
- Create invoice
- Update invoice
- Delete invoice
- Fetch invoice
- Paginated list
- Publish domain event

---

## InvoiceProcessingOrchestrator

Responsibilities:
- Listen to InvoiceCreatedEvent
- Trigger AI extraction
- Update invoice status

---

# 5. Strategy Pattern

## AiExtractionStrategy

Method:
extract(UUID invoiceId)

Implementations:
- MockAiExtractionService
- OpenAiExtractionService

---

## CategorizationStrategy

Method:
categorize(String itemName)

Implementations:
- RuleBasedCategorizationService
- AiCategorizationService

---

## StorageStrategy

Method:
store(MultipartFile file)

Implementations:
- InMemoryStorageService
- S3StorageService

---

# 6. Transaction Management

Use @Transactional on:
- Create
- Update
- AI processing

---

# 7. Exception Handling

GlobalExceptionHandler handles:

- ResourceNotFoundException
- BadRequestException
- ConflictException
- Generic Exception

Response format:

{
"status": 404,
"error": "Invoice not found",
"timestamp": "..."
}

---
