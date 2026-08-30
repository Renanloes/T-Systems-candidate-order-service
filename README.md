# T-Systems International Developer Challenge

Solution for the T-Systems International Developer Challenge.

The original application used a local price catalog to obtain product prices.

The application was changed to use the external Pricing API provided by T-Systems.

The main goal of the change is to allow an order to exist even when the Pricing API is temporarily unavailable.

## Requirements

* Java 21+
* Maven 3.9+
* Docker

## Running the Pricing API

Start the external Pricing API with:

```bash id="n2zyc9"
docker run --rm --name pricing-api -p 8090:8080 eduardosassegdcbrazil/tsystems-pricing-api:1.0
```

Check that it is running:

```bash id="btl43i"
curl http://localhost:8090/health
```

The application uses this default Pricing API URL:

```text id="ldverw"
http://localhost:8090
```

The URL can also be changed with the `PRICING_API_URL` environment variable.

## Running the application

Run the tests:

```bash id="rbyigx"
mvn test
```

Start the application:

```bash id="qh5yh0"
mvn spring-boot:run
```

The application starts on:

```text id="erz68p"
http://localhost:8080
```

Dashboard:

```text id="rh7jrv"
http://localhost:8080/
```

REST API:

```text id="idf8at"
http://localhost:8080/api/orders
```

## API

Create an order:

```http id="uer206"
POST /api/orders
```

Example:

```json id="mq79ti"
{
  "customerId": "customer-42",
  "productId": "SKU-1001",
  "quantity": 2,
  "country": "DE",
  "currency": "EUR"
}
```

Get an order:

```http id="dvl9qf"
GET /api/orders/{id}
```

List orders:

```http id="fyyup3"
GET /api/orders
```

## Main Changes

### External Pricing API

The local `LocalCatalogPriceService` is no longer used to obtain prices for new orders.

A dedicated `PricingApiClient` communicates with the external Pricing API.

The application requests prices using:

```text id="4t1e07"
GET /v1/prices/{productId}
```

with the order country and currency.

The returned amount is converted to `BigDecimal` and used to calculate the order total.

### Order State

The order status was extended to support the pricing lifecycle:

* `PENDING_PRICING`
* `CONFIRMED`
* `NEEDS_ATTENTION`

A new order receives a UUID before the pricing request is made.

The order is first stored as `PENDING_PRICING`.

If a valid price is received, the same order ID is stored as `CONFIRMED`.

If the Pricing API returns a client-side error such as `404`, the order is changed to `NEEDS_ATTENTION`.

If the Pricing API is temporarily unavailable or returns a server-side error, the order remains `PENDING_PRICING`.

### Pricing Retry

Orders with `PENDING_PRICING` are checked by a scheduled retry service.

The current implementation retries pending pricing every 10 seconds.

When pricing becomes available again, the same order ID is used and the order moves to `CONFIRMED`.

The retry interval is intentionally short so that the recovery behavior can be demonstrated during the exercise.

### Repository

The existing `InMemoryOrderRepository` was kept.

A `findByStatus` operation was added so that pending orders can be located by the retry service.

The repository uses the order UUID as its key, allowing an updated order representation to replace the previous one while keeping the same ID.

### Browser Dashboard

The existing server-rendered Thymeleaf dashboard was kept and extended.

It now distinguishes between:

* `CONFIRMED`
* `AWAITING PRICING`
* `NEEDS ATTENTION`

Orders without a price are shown as not yet priced or as having a pricing failure.

No JavaScript, TypeScript, frontend framework, or CSS framework was added.

## Assumptions

* The Pricing API is the source of truth for new order prices.
* A valid price must be obtained before an order can become `CONFIRMED`.
* A temporary pricing failure must not cause the order itself to be lost.
* HTTP `4xx` responses are treated as problems that require attention.
* HTTP `5xx` responses and connection failures are treated as recoverable pricing failures.
* The retry interval is 10 seconds for this exercise.
* The in-memory repository is sufficient for the scope of the challenge.
* The third-party Pricing API is treated as a black box and only its documented contract is used.

These classifications are exercise assumptions because the customer did not explicitly define all temporary and permanent provider failures.

## Important Engineering Decisions

### Create the order before pricing

The order receives a stable UUID and is stored before the Pricing API result is known.

This prevents a temporary pricing outage from forcing the store user to submit the same order again.

### Separate Pricing API communication

A dedicated `PricingApiClient` is responsible for communicating with the external API.

This keeps HTTP communication and provider-specific details outside the main order business logic.

### Keep the existing domain model

The existing `Order` record was kept.

Because records are immutable, a new `Order` instance is created when the pricing state changes, while keeping the same UUID.

This represents the same business order with updated data instead of creating a second order type.

### Keep the existing repository

The challenge already provides an in-memory repository and does not require a database.

The solution therefore avoids adding unnecessary infrastructure.

## Known Limitations and Considerations

The repository is still in memory, so orders are lost when the application restarts.

The retry policy is intentionally simple and uses a fixed 10-second interval.

The classification of provider failures is based on assumptions made for this exercise and would need a more detailed policy in a production system.

The current application does not provide order cancellation or order editing.

These operations would require explicit business rules, especially for orders that are still waiting for pricing.

A possible future improvement would be a lightweight file-based persistence mechanism for pending orders. This could preserve pending orders across an application or machine restart without introducing a full database. This was not implemented because persistent storage was outside the scope of the exercise.

### Discarded Product Selection Approach

During development, I considered adding a product selector based on the Pricing API `/v1/products` endpoint.

I decided not to keep this approach because it added another dependency on the external API to the order form and increased the complexity of the failure path, while not being required by CR-002.

The final implementation therefore keeps the original product input and focuses the external integration on the pricing flow.

## Testing

The automated tests were updated to reflect the external Pricing API integration.

The main scenarios to validate are:

* successful pricing;
* Pricing API unavailable;
* Pricing API returning an error;
* order remaining identifiable by its stable ID;
* pending order recovery after pricing becomes available;
* order not being confirmed without a valid price;
* browser order creation and validation;
* dashboard rendering with the updated pricing flow.

## CHANGE_REQUEST.md

CR-002 changed the original order creation flow.

The starter application obtained the price before saving the order.

The new requirement requires the order to be accepted and assigned a stable ID even when the Pricing API is unavailable.

This changed:

* the order creation sequence;
* the order status model;
* the repository behavior;
* the pricing integration;
* the retry mechanism;
* the browser dashboard.

## AI Assistance

An AI assistant was used during development to help understand the existing codebase and discuss implementation approaches.

The assistance included understanding the existing flow, identifying the classes affected by the change, and helping with the development of code blocks for the Pricing API integration, order state handling, retry flow, tests, and dashboard changes.

The suggestions were reviewed against the existing source code, the Pricing API contract, and `CHANGE_REQUEST.md`.

AI-generated suggestions were not treated as automatically correct. The implementation was adjusted when needed to preserve the existing application structure and keep the changes focused on the requirements of the exercise.

## Security

No secrets, credentials, or private keys are included in the repository.

Configuration values required to run the application are kept in application configuration or environment variables.
