![Build](https://github.com/theSirD/flash-sale-platform/actions/workflows/build.yml/badge.svg)

# Flash Sale Platform

Микросервисная платформа флэш-распродаж на Java 21 / Spring Boot 3.

Ограниченная партия товара выходит в продажу в заданный момент. Тысячи запросов конкурируют за один остаток. Цель проекта — показать, что при пиковой нагрузке **oversell = 0**.

## Сервисы

| Сервис | Порт | Назначение |
|---|---|---|
| catalog-service | 8081 | Распродажи и товары |
| inventory-service | 8082 | Остатки, заказы, Saga, Outbox |
| payment-service | 8083 | Мок оплаты |
| notification-service | 8084 | Kafka consumer → email (MailHog) |

У каждого сервиса своя база PostgreSQL. Стек поднимается через Docker Compose (PostgreSQL, Redis, Kafka, MailHog).

Покупатель и демо-скрипты ходят **только в inventory** (`:8082`). Catalog, payment и notification вызываются изнутри: inventory оркестрирует Saga, notification слушает Kafka. Снаружи они не нужны для сценария «купить».

## Быстрый старт

Нужен только Docker + Docker Compose.

```bash
git clone https://github.com/theSirD/flash-sale-platform.git
cd flash-sale-platform
docker compose up --build
```

Первая сборка образов занимает несколько минут (Gradle внутри Docker). После старта:

| | URL | Зачем |
|---|---|---|
| Inventory API | http://localhost:8082 | Заказы и остаток — главная точка входа |
| MailHog | http://localhost:8025 | UI писем после успешной покупки |

Опционально для отладки: catalog на http://localhost:8081 (`GET /api/sales/{saleId}`).

### Пример покупки

`saleId` ниже — из seed-миграции catalog (`V2__seed.sql`, `initial_stock = 100`).

1. Создать заказ:

```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-1" \
  -d '{
    "saleId": "22222222-2222-2222-2222-222222222222",
    "quantity": 1,
    "userEmail": "buyer@example.com"
  }'
```

В ответе будет `id` заказа и статус (`CONFIRMED` при успешной оплате).

2. Проверить остаток:

```bash
curl http://localhost:8082/api/sales/22222222-2222-2222-2222-222222222222
```

3. Проверить заказ (подставь `id` из шага 1):

```bash
curl http://localhost:8082/api/orders/<order-id>
```

4. Открыть http://localhost:8025 — письмо о подтверждении (или отмене) от notification-service.

Повтори `POST` с тем же `Idempotency-Key` — вернётся тот же заказ, без второй продажи.

## Как устроена покупка

Статусы заказа: `PENDING` → `CONFIRMED` | `CANCELLED` | `EXPIRED`. Всё крутится в `inventory-service`.

```
POST /api/orders
  → catalog: sale ACTIVE?
  → Redis: reserve (Lua)
  → Postgres: заказ PENDING
  → payment: charge
  → OK: sold++ в Postgres, consume reserve, Outbox(OrderConfirmed)
  → fail/timeout: release reserve, Outbox(OrderCancelled), refund если уже списали
```

Если оплата прошла, но Postgres не принял инкремент `sold` — refund и отмена. Просроченные `PENDING` снимает scheduler по cutoff чуть короче Redis TTL: `EXPIRED`, резерв в Redis возвращается.

**Saga:** inventory оркестрирует шаги сам. Компенсации — `releaseReserve` при fail/timeout оплаты и `refund`, если charge уже прошёл, а `sold++` в Postgres не приняли.

**Outbox:** событие пишется в той же транзакции, что смена статуса. Publisher помечает `published_at` только после ack от Kafka; при ошибке — retry на следующем poll. Notification дедуплицирует по `(order_id, event_type)`.

## Почему oversell невозможен

1. **Redis** — Lua `reserve_stock.lua`: декремент только если `stock >= quantity`.
2. **Postgres** — условный `UPDATE ... WHERE sold + q <= initial_stock`.
3. **CHECK** — `sold <= initial_stock` на уровне схемы.
4. **Идемпотентность** — `Idempotency-Key` на заказе; payment по `order_id`.

## Тесты и CI

```bash
./gradlew build
```

Интеграционные тесты (нужен Docker):

```bash
./gradlew :inventory-service:test --tests ConcurrencyIntegrationTest
./gradlew :inventory-service:test --tests PaymentFailureIntegrationTest
./gradlew :inventory-service:test --tests PaymentTimeoutIntegrationTest
```

- `ConcurrencyIntegrationTest` — параллельные покупки, проверка `sold <= initial_stock`.
- Payment-тесты — компенсации Saga при fail и timeout.
- CI: GitHub Actions на push в `dev` / `main` (`.github/workflows/build.yml`, `./gradlew build`).

## Стек

- Java 21, Spring Boot 3
- PostgreSQL, Redis, Kafka
- Flyway, Testcontainers
- Docker Compose, GitHub Actions
