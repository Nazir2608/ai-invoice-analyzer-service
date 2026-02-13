#  System Flow & Architecture Diagram
## ai-invoice-analyzer-service

---

# 1. High Level Flow

Client
↓
Controller
↓
Service
↓
Repository
↓
Database

---

# 2. Invoice Creation Flow

POST /api/invoices
↓
InvoiceService.create()
↓
Save Invoice (status=PROCESSING)
↓
Publish InvoiceCreatedEvent
↓
InvoiceProcessingOrchestrator
↓
AiExtractionStrategy
↓
CategorizationStrategy
↓
Save InvoiceItems
↓
Update Status → COMPLETED

---

# 3. Complete Processing Diagram

                +------------------+
                |      Client      |
                +------------------+
                         |
                         v
                +------------------+
                | InvoiceController |
                +------------------+
                         |
                         v
                +------------------+
                |   InvoiceService  |
                +------------------+
                         |
                         v
                +------------------+
                |  InvoiceRepository|
                +------------------+
                         |
                         v
                +------------------+
                |     Database     |
                +------------------+
                         |
                         v
                +------------------+
                | InvoiceCreatedEvent |
                +------------------+
                         |
                         v
                +------------------+
                | Processing Orchestrator |
                +------------------+
                         |
                         v
                +------------------+
                | AiExtractionStrategy |
                +------------------+
                         |
                         v
                +------------------+
                | CategorizationStrategy |
                +------------------+
                         |
                         v
                +------------------+
                | Update Invoice Status |
                +------------------+

---

# 4. Strategy Switching (Property Based)

application-local.yml

ai.provider = mock  
storage.type = memory

application-prod.yml

ai.provider = openai  
storage.type = s3

No code change required.

---

# 5. Pagination Flow

GET /api/invoices?page=0&size=10&sort=createdAt,desc
↓
Service
↓
PageRequest
↓
Database
↓
PagedResponse<InvoiceResponse>

---

# 6. Error Handling Flow

Exception Thrown
↓
GlobalExceptionHandler
↓
ErrorResponse JSON

---

# 7. Future Extensions

- Kafka for async processing
- Retry mechanism
- Dead Letter Queue
- Multi-tenant support
- Security layer

---
