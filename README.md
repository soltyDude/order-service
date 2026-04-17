[![CI](https://github.com/soltyDude/order-service/actions/workflows/ci.yml/badge.svg)](https://github.com/soltyDude/order-service/actions/workflows/ci.yml)
# Order Service

Saga orchestrator and order lifecycle manager for the [Order Management Platform](https://github.com/soltyDude/order-management-infra). Accepts orders, manages state transitions, coordinates payment and inventory services through Kafka, and serves optimized reads via CQRS.

## Responsibility

- Accept and validate orders from clients
- Orchestrate CreateOrderSaga (payment → inventory)
- Enforce order state machine (only valid transitions)
- Maintain denormalized read model for fast queries
- Handle cancellation with compensating transactions

## Patterns

| Pattern | Why |
|---------|-----|
| **Saga Orchestration** | Order creation spans 2 services. Orchestrator keeps the entire flow in one place — single point of visibility, not single point of failure (stateless, scalable). |
| **Outbox Pattern** | Order save + event publish must be atomic. Event is written to `outbox_events` table in the same transaction. Poller publishes to Kafka separately. No dual-write risk. |
| **CQRS** | Write model: normalized `orders` + `order_items`. Read model: denormalized `order_read_model` with items as JSONB, payment/reservation IDs. GET endpoints read from the read model — zero JOINs, zero N+1. |
| **State Machine** | `OrderStatus` enum with `validTransitions` map. `Order.transitionTo()` validates every transition. Invalid moves throw `InvalidOrderStateException`. |
| **Optimistic Locking** | `@Version` on Order entity. Concurrent updates detected at commit time — no DB-level locks held. |

## Tech Stack

Java 17 · Spring Boot 3.x · PostgreSQL 16 · Flyway · Maven

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create order (starts saga). Requires `Idempotency-Key` header |
| GET | `/api/v1/orders/{id}` | Order details from CQRS read model |
| GET | `/api/v1/orders` | List orders (paginated, filtered by user from JWT) |
| PATCH | `/api/v1/orders/{id}/cancel` | Cancel order, triggers compensations |
| GET | `/api/v1/orders/{id}/status` | Lightweight status polling for saga progress |

### Create Order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "items": [
      { "productId": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "quantity": 2, "price": 29.99 },
      { "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "quantity": 1, "price": 49.99 }
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Warsaw",
      "zipCode": "00-001",
      "country": "PL"
    },
    "paymentMethod": "CARD"
  }'
```

**Response (201):**
```json
{
  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "items": [
    { "productId": "7c9e6679-...", "quantity": 2, "price": 29.99, "subtotal": 59.98 },
    { "productId": "a1b2c3d4-...", "quantity": 1, "price": 49.99, "subtotal": 49.99 }
  ],
  "totalAmount": 109.97,
  "paymentMethod": "CARD",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

`totalAmount` is always calculated server-side. `items[].price` is treated as "expected price" — the server validates it against the catalog and rejects with 409 on mismatch.

### Cancel Order

```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{id}/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{ "reason": "Changed my mind" }'
```

Compensations triggered depend on how far the saga progressed:

| Cancelled From | Compensations |
|----------------|---------------|
| PENDING | None |
| PAYMENT_PROCESSING | Payment cancel (best-effort) |
| PAYMENT_CONFIRMED | Refund via Kafka |
| INVENTORY_RESERVING | Refund via Kafka |
| CONFIRMED | Refund + stock release via Kafka |

## Order State Machine

```
PENDING
  ├──► PAYMENT_PROCESSING        payment.charge.requested sent
  └──► CANCELLED                 user cancel before payment

PAYMENT_PROCESSING
  ├──► PAYMENT_CONFIRMED         payment.processed received
  └──► CANCELLED                 payment.failed received

PAYMENT_CONFIRMED
  ├──► INVENTORY_RESERVING       inventory.reserve.requested sent
  └──► CANCELLED                 user cancel → refund triggered

INVENTORY_RESERVING
  ├──► CONFIRMED                 inventory.reserved received
  └──► CANCELLED                 inventory.failed → refund triggered

CONFIRMED
  ├──► SHIPPED                   (future: fulfillment)
  └──► CANCELLED                 user cancel → refund + release

SHIPPED ──► DELIVERED            (future: delivery confirmation)
```

Terminal states: `DELIVERED`, `CANCELLED`.

## Database Schema

Six tables in `orders_db` (port 5432):

| Table | Purpose |
|-------|---------|
| `orders` | Write model. Order header with `@Version` for optimistic locking |
| `order_items` | Order line items (productId, quantity, price snapshot) |
| `order_status_history` | Append-only audit of every status transition |
| `order_read_model` | CQRS denormalized projection. Items as JSONB, cross-service IDs (paymentId, reservationId) |
| `outbox_events` | Transactional outbox for reliable Kafka publishing |
| `processed_events` | Consumer idempotency — tracks processed Kafka eventIds |

Migrations managed by Flyway (`src/main/resources/db/migration/`):
```
V001__create_orders_table.sql
V002__create_order_items_table.sql
V003__create_order_status_history_table.sql
V004__create_order_read_model_table.sql
V005__create_outbox_events_table.sql
V006__create_processed_events_table.sql
```

## Package Structure

```
com.example.orderservice/
├── api/
│   ├── controller/          OrderController
│   ├── dto/
│   │   ├── request/         CreateOrderRequest, CancelOrderRequest
│   │   └── response/        OrderDto, OrderSummaryDto, OrderStatusDto, OrderCancelDto
│   └── exception/           GlobalExceptionHandler, ErrorResponse
├── domain/
│   ├── model/               Order (@Entity), OrderItem, OrderStatus (state machine), ShippingAddress
│   ├── event/               OrderCreatedEvent, OrderCancelledEvent, OrderConfirmedEvent
│   └── service/             OrderService (writes), OrderQueryService (CQRS reads)
├── infrastructure/
│   ├── kafka/
│   │   ├── producer/        OutboxPoller
│   │   └── consumer/        PaymentResultConsumer, InventoryResultConsumer
│   ├── persistence/
│   │   ├── entity/          OutboxEvent, ProcessedEvent, OrderReadModel
│   │   ├── repository/      OrderRepository, OutboxEventRepository, OrderReadModelRepository
│   │   └── mapper/          OrderMapper
│   └── config/              KafkaConfig, SecurityConfig
└── OrderServiceApplication.java
```

**Layering rules** (enforced by ArchUnit):
- `api` → `domain` ✅
- `api` → `infrastructure` ❌
- `domain` → `infrastructure` ❌ (domain is pure)
- `infrastructure` → `domain` ✅

## Kafka Topics

**Produces (commands):**
| Topic | When | Partition Key |
|-------|------|---------------|
| `payment.charge.requested` | Order created | orderId |
| `payment.refund.requested` | Order cancelled after payment | orderId |
| `inventory.reserve.requested` | Payment confirmed | orderId |
| `inventory.release.requested` | Order cancelled after reservation | orderId |

**Consumes (results):**
| Topic | Action |
|-------|--------|
| `payment.processed` | Order → PAYMENT_CONFIRMED, trigger inventory reservation |
| `payment.failed` | Order → CANCELLED |
| `inventory.reserved` | Order → CONFIRMED |
| `inventory.failed` | Order → CANCELLED, trigger refund |
| `payment.refunded` | Update read model |
| `inventory.released` | Update read model |

All events partitioned by `orderId` — guarantees ordering per order.

## Running Locally

**Prerequisites:** Docker, Java 17+, Maven 3.9+

```bash
# 1. Start infrastructure (from order-management-infra repo)
docker compose up -d

# 2. Run the service
mvn spring-boot:run

# 3. Health check
curl http://localhost:8080/actuator/health
```

### Configuration

```yaml
# application.yml (defaults for local dev)
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate    # Flyway manages schema
    open-in-view: false      # no lazy loading in controllers
  kafka:
    bootstrap-servers: localhost:9092
```

All external configs are overridable via environment variables (`DB_URL`, `DB_USERNAME`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`).

## Current Status

**Phase 1 complete** — REST API works end-to-end:
- ✅ Domain model with state machine and optimistic locking
- ✅ Flyway migrations (6 tables)
- ✅ Persistence layer with CQRS read/write separation
- ✅ OrderService (create, cancel with state validation)
- ✅ OrderQueryService (read model queries)
- ✅ REST controller with full error handling
- ✅ Outbox events saved transactionally (publishing in Phase 4)

**Coming next:**
- Unit tests (Phase 1.10)
- Kafka consumers + saga orchestration (Phase 4)
- JWT security + RBAC (Phase 5)
- Integration tests with Testcontainers (Phase 6)
- GitHub Actions CI pipeline (Phase 6)

## Related

- [order-management-infra](https://github.com/soltyDude/order-management-infra) — Docker Compose, documentation, project overview
- [Architecture Decision Records](https://github.com/soltyDude/order-management-infra/blob/main/docs/adr-full.md)
- [Full API Contract](https://github.com/soltyDude/order-management-infra/blob/main/docs/api-contract-full.md)
