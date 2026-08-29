## **T-Systems International Developer Challenge**

Order Management service built with **Java 21** and **Spring Boot**, integrating the external Pricing API provided for the challenge.

The main goal of the implementation is to allow orders to survive temporary Pricing API failures without requiring the store user to submit the same order again.

---

## **Requirements**

* **Java 21+**
* **Maven 3.9+**
* **Docker**

---

## **Running the Pricing API**

Start the external Pricing API with:

```bash
docker run --rm --name pricing-api -p 8090:8080 eduardosassegdcbrazil/tsystems-pricing-api:1.0
```

Check that it is running:

```bash
curl http://localhost:8090/health
```

The application uses the following default Pricing API URL:

```text
http://localhost:8090
```

It can be changed with the `PRICING_API_URL` environment variable.

---

## **Running the Application**

Run the tests:

```bash
mvn test
```

Start the application:

```bash
mvn spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

# **Browser Dashboard**

```text
http://localhost:8080/
```

# **REST API**

```text
/api/orders
```

---

## **Example Request**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-42",
    "productId": "SKU-1001",
    "quantity": 2,
    "country": "DE",
    "currency": "EUR"
  }'
```

---

## **Pricing API Integration**

The local `LocalCatalogPriceService` is no longer used as the source of truth for new orders.

The application requests pricing from:

```http
GET /v1/prices/{productId}
```

using:

* `productId`
* `country`
* `currency`

The HTTP communication is isolated in `PricingApiClient`.

The client handles:

* successful price responses;
* HTTP errors;
* communication failures;
* invalid price data.

---

## **Order Lifecycle**

An order starts as:

```text
PENDING_PRICING
```

when a valid price has not been obtained yet.

When pricing succeeds:

```text
PENDING_PRICING
        ↓
   CONFIRMED
```

For a provider error that is treated as non-recoverable:

```text
PENDING_PRICING
        ↓
NEEDS_ATTENTION
```

The order keeps the **same UUID** during these state changes.

This prevents the creation of a second order when pricing is delayed or temporarily unavailable.

---

## **Pricing Outage Handling**

The order is created and stored before pricing is considered successful.

When the Pricing API is unavailable:

1. The application generates a stable order ID.
2. The order is stored as `PENDING_PRICING`.
3. No price or total is assigned.
4. The order remains available in the application.
5. The pricing retry mechanism can attempt the request again.
6. When a valid price is obtained, the same order is updated to `CONFIRMED`.

A successful HTTP response from the Order Management service therefore does **not** mean that the order has already been successfully priced.

---

## **Retry Strategy**

Pending orders are checked periodically by `PricingRetryService`.

For the scope of this exercise, the retry interval is intentionally short so that the recovery behavior can be demonstrated easily.

A production system would use a configurable retry interval and a more complete backoff and failure policy.

---

## **Browser Dashboard**

The supplied Thymeleaf dashboard was kept and extended instead of replacing it.

The dashboard can show:

* **CONFIRMED** — pricing was successfully obtained;
* **AWAITING PRICING** — the order exists but pricing is not available yet;
* **NEEDS ATTENTION** — pricing failed in a way that requires attention.

The interface uses:

* server-side Thymeleaf rendering;
* semantic HTML;
* the existing CSS;
* no JavaScript;
* no frontend framework.

The screen communicates the business state to the store user instead of exposing stack traces or internal technical errors.

---

## **Important Engineering Decisions**

# **Stable Order ID**

The order ID is generated before the Pricing API request.

This ensures that a temporary pricing failure does not force the user to submit the order again.

# **Separate Pricing Client**

The external API communication is isolated in:

```text
PricingApiClient
```

This keeps HTTP details outside the main order business logic.

# **Keep the Existing Architecture**

The original controller, service, repository and domain structure were kept.

The solution adds only the components needed for the new pricing behavior instead of rebuilding the application.

# **In-Memory Repository**

The existing:

```text
InMemoryOrderRepository
```

was kept.

This avoids introducing a database or additional infrastructure that was not required by the exercise.

The trade-off is that orders do not survive an application restart.

# **Order Record**

`Order` remains a Java `record`.

Because records are immutable, a state transition creates a new `Order` instance while keeping the same UUID.

---

## **Assumptions**

The challenge leaves some behavior open to implementation decisions.

The main assumptions are:

* The external Pricing API is the source of truth for new prices.
* A valid price is required before an order can become `CONFIRMED`.
* Temporary provider or connectivity failures are retryable.
* A product that cannot be found is treated as a problem requiring attention.
* The order keeps the same ID during its lifecycle.
* The in-memory repository is acceptable for the scope of the exercise.
* The retry configuration is simplified for demonstration purposes.

---

## **Known Limitations**

This implementation is intentionally small and focused on the challenge requirements.

The current solution does not provide the full persistence and resilience capabilities expected from a production system.

For a production implementation, I would reconsider:

* durable database storage;
* configurable retry policies and exponential backoff;
* stronger error classification;
* request timeouts;
* distributed processing for pending orders;
* concurrency control;
* metrics and monitoring;
* distributed tracing;
* operational recovery mechanisms.

---

## **Testing**

The important scenarios covered by the implementation are:

* successful pricing;
* Pricing API unavailable;
* order stored without a price;
* stable order ID;
* transition from `PENDING_PRICING` to `CONFIRMED`;
* provider errors that require attention;
* retry of pending pricing.

The external Pricing API is treated as a **black-box dependency** according to the supplied OpenAPI contract.

---

## **CHANGE_REQUEST.md**

`CHANGE_REQUEST.md` changed the original design mainly by requiring an order to survive a Pricing API outage.

The original flow was:

```text
Create Order
    ↓
Get Price
    ↓
Confirm
    ↓
Save
```

The new flow is:

```text
Create Order
    ↓
Generate stable ID
    ↓
Save as PENDING_PRICING
    ↓
Request Pricing
    ↓
Confirm when a valid price is available
```

This requirement affected the order lifecycle, repository usage, pricing integration and browser dashboard.

---

## **AI Assistance**

An AI assistant was used during development to help understand the existing codebase, identify affected components and discuss implementation approaches.

The suggestions were checked against:

* the existing source code;
* the supplied Pricing API contract;
* `CHANGE_REQUEST.md`;
* the observed application behavior.

AI-generated code was reviewed and tested before being used.

The final implementation was intentionally kept close to the original project structure.

---

## **Security**

No secrets, credentials or private keys are included in this repository.

Configuration values such as the Pricing API URL are provided through application configuration or environment variables.
