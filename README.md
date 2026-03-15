# Foreign Exchange Rate API

A Spring Boot REST API service that fetches and serves foreign exchange rates from the Bundesbank API. The service provides endpoints for retrieving currencies, exchange rates, historical data with pagination, and currency conversion.

---

## Table of Contents

1. [Architecture & Design](#architecture--design)
2. [API Endpoints & Examples](#api-endpoints--examples)
3. [Technology Stack](#technology-stack)
4. [Getting Started](#getting-started)
5. [API Documentation (Swagger)](#api-documentation-swagger)
6. [Test Coverage](#test-coverage)
7. [Configuration](#configuration)
8. [Project Structure](#project-structure)

---

## Architecture & Design

### System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT (Browser/App)                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SPRING BOOT APPLICATION                           │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         REST CONTROLLERS                              │  │
│  │  ┌─────────────────────────┐    ┌──────────────────────────────────┐  │  │
│  │  │   CurrencyController    │    │    ExchangeRateController        │  │  │
│  │  │   /api/currencies       │    │    /api/forex-rates              │  │  │
│  │  └─────────────────────────┘    └──────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                       │                                      │
│                                       ▼                                      │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    SERVICE INTERFACES + IMPLEMENTATIONS                │  │
│  │  ┌──────────────────┐  ┌───────────────────┐  ┌───────────────────┐  │  │
│  │  │ ICurrencyService │  │ IExchangeRate     │  │ ICurrencyConver-  │  │  │
│  │  │ CurrencyService  │  │ QueryService      │  │ sionService       │  │  │
│  │  │                  │  │ ExchangeRateQuery │  │ CurrencyConver-   │  │  │
│  │  │                  │  │ Service           │  │ sionService       │  │  │
│  │  └──────────────────┘  └───────────────────┘  └───────────────────┘  │  │
│  │                         ┌───────────────────┐                         │  │
│  │                         │ IExchangeRateSync │                         │  │
│  │                         │ Service           │                         │  │
│  │                         │ ExchangeRateSync  │                         │  │
│  │                         │ Service           │                         │  │
│  │                         └───────────────────┘                         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│              │                     │                      │                  │
│              ▼                     ▼                      ▼                  │
│  ┌────────────────────┐  ┌────────────────┐  ┌─────────────────────────┐   │
│  │    MAPPERS          │  │  REPOSITORIES  │  │   BUNDESBANK CLIENT     │   │
│  │  CurrencyMapper     │  │  Currency-     │  │   @Retryable            │   │
│  │  ExchangeRateMapper │  │  Repository    │  │   fetchAllExchangeRates │   │
│  │                     │  │  ExchangeRate- │  │   fetchLatestRates      │   │
│  │                     │  │  Repository    │  │   + @Recover fallback   │   │
│  └────────────────────┘  └────────────────┘  └─────────────────────────┘   │
│                                │                          │                  │
│                                ▼                          │                  │
│                       ┌────────────────┐                  │                  │
│                       │  H2 DATABASE   │                  │                  │
│                       │  currencies    │                  │                  │
│                       │  exchange_rates│                  │                  │
│                       └────────────────┘                  │                  │
│                                                           │                  │
│  ┌────────────────────────────────────────────────────────┴───────────────┐ │
│  │                    SCHEDULER (Daily at 6 AM)                           │ │
│  │                    ScheduledExchangeRateUpdater                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BUNDESBANK API (External)                           │
│                 https://api.statistiken.bundesbank.de/rest/data/BBEX3       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Startup    │────▶│  DataLoader  │────▶│ Bundesbank   │────▶│   Database   │
│              │     │              │     │ Client       │     │   (H2)       │
│              │     │              │     │ (@Retryable) │     │              │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                                                                      │
┌──────────────┐     ┌──────────────┐     ┌──────────────┐            │
│   Client     │────▶│  Controller  │────▶│   Service    │◀───────────┘
│   Request    │     │  (Validated) │     │  (Interface) │
└──────────────┘     └──────────────┘     └──────────────┘
                            │                     │
                            ▼                     ▼
                     ┌──────────────┐     ┌──────────────┐
                     │   Response   │     │   Mapper     │
                     │   (JSON)     │     │   (DTO↔Ent)  │
                     └──────────────┘     └──────────────┘
```

### Key Design Decisions

- **CQRS-lite**: Read operations (`ExchangeRateQueryService`) separated from write operations (`ExchangeRateSyncService`)
- **Interface Segregation**: All services expose interfaces (`ICurrencyService`, `IExchangeRateQueryService`, etc.)
- **Immutable DTOs**: All DTOs are Java records for thread-safety and clarity
- **Entity Best Practices**: Manual `equals`/`hashCode` based on business keys (not `@Data`)
- **Resilience**: `@Retryable` with exponential backoff on external API calls, `@Recover` fallback
- **Inverse Rate Fallback**: If EUR/USD is not found, calculates from USD/EUR automatically

---

## API Endpoints & Examples

> **Note:** On application startup, the system automatically loads the **last 100 available exchange rate records** for each currency from the Bundesbank API.

### Available Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/currencies` | Get all available currencies |
| `GET /api/forex-rates/{base}` | Get all EUR-FX exchange rates (all dates) |
| `GET /api/forex-rates/{base}?date={YYYY-MM-DD}` | Get EUR-FX exchange rate at a particular day |
| `GET /api/forex-rates/{base}/history` | Get exchange rate history for base currency |
| `GET /api/forex-rates/{base}/history/paginated` | Get paginated exchange rate history |
| `GET /api/forex-rates/{base}/{target}` | Get specific currency pair rate (latest) |
| `GET /api/forex-rates/{base}/{target}?date={YYYY-MM-DD}` | Get specific currency pair rate at a date |
| `GET /api/forex-rates/{base}/{target}/history` | Get currency pair history |
| `GET /api/forex-rates/{base}/{target}/history/paginated` | Get paginated currency pair history |
| `GET /api/forex-rates/convert` | Convert currency amount |

---

### 1. Get All Available Currencies

**Endpoint:** `GET /api/currencies`

**Description:** Returns a list of all available currencies supported by the system.

**Request:**
```bash
curl -X GET http://localhost:8080/api/currencies
```

**Response:**
```json
[
  {
    "code": "EUR",
    "name": "Euro"
  },
  {
    "code": "USD",
    "name": "United States Dollar"
  },
  {
    "code": "GBP",
    "name": "British Pound Sterling"
  }
]
```

---

### 2. Get All EUR-FX Exchange Rates (All Available Dates)

**Endpoint:** `GET /api/forex-rates/{base}/history`

**Description:** Returns all EUR-FX exchange rates at all available dates as a collection, grouped by date.

**Request:**
```bash
curl -X GET http://localhost:8080/api/forex-rates/EUR/history
```

**Response:**
```json
[
  {
    "date": "2026-03-14",
    "baseCurrency": "EUR",
    "rates": {
      "USD": 1.0876,
      "GBP": 0.8392,
      "JPY": 161.45
    }
  },
  {
    "date": "2026-03-13",
    "baseCurrency": "EUR",
    "rates": {
      "USD": 1.0912,
      "GBP": 0.8401,
      "JPY": 162.10
    }
  }
]
```

---

### 3. Get EUR-FX Exchange Rate at a Particular Day

**Endpoint:** `GET /api/forex-rates/{base}?date={YYYY-MM-DD}`

**Description:** Returns the EUR-FX exchange rates for a specific date.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/EUR?date=2026-03-13"
```

**Response:**
```json
[
  {
    "date": "2026-03-01",
    "baseCurrency": "EUR",
    "rates": {
      "USD": 1.0823,
      "GBP": 0.8356,
      "JPY": 130.50
    }
  }
]
```

---

### 4. Get Foreign Exchange Amount Converted to EUR

**Endpoint:** `GET /api/forex-rates/convert?from={currency}&to={currency}&amount={value}&date={YYYY-MM-DD}`

**Description:** Converts a foreign exchange amount from one currency to another on a particular day.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/convert?from=USD&to=EUR&amount=100&date=2026-03-13"
```

**Response:**
```json
{
  "fromCurrency": "USD",
  "fromAmount": 100.00,
  "toCurrency": "EUR",
  "toAmount": 92.40,
  "date": "2026-03-13",
  "exchangeRate": 0.9240
}
```

---

### 5. Get Specific Currency Pair Exchange Rate

**Endpoint:** `GET /api/forex-rates/{base}/{target}?date={YYYY-MM-DD}`

**Description:** Returns the exchange rate for a specific currency pair on a particular day.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/EUR/USD?date=2026-03-13"
```

**Response:**
```json
{
  "pair": "EUR/USD",
  "baseCurrency": "EUR",
  "targetCurrency": "USD",
  "date": "2026-03-13",
  "rate": 1.147600,
  "description": "1 EUR = 1.147600 USD"
}
```

---

### Error Responses

**400 Bad Request - Invalid Parameter:**
```json
{
  "timestamp": "2026-03-14T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "'from' currency is required"
}
```

**404 Not Found - Resource Not Found:**
```json
{
  "timestamp": "2026-03-14T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Exchange rate not found for EUR to XXX"
}
```

**503 Service Unavailable - External API Failure:**
```json
{
  "timestamp": "2026-03-14T12:00:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Failed to fetch exchange rates from Bundesbank API after retries"
}
```

---

## Technology Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.5.11 |
| **Build Tool** | Maven | 3.x |
| **Database** | H2 (In-Memory) | 2.3.232 |
| **ORM** | Spring Data JPA / Hibernate | 6.6.x |
| **API Documentation** | SpringDoc OpenAPI (Swagger) | 2.8.9 |
| **Resilience** | Spring Retry | (managed by Spring Boot) |
| **Code Coverage** | JaCoCo | 0.8.11 |
| **Utility** | Lombok | (managed by Spring Boot) |
| **Testing** | JUnit 5, Mockito, MockMvc | (managed by Spring Boot) |
| **External API** | Bundesbank SDMX REST API | - |

---

## Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.x**
- Internet connection (for fetching exchange rates from Bundesbank API)

### Installation & Running

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd forex
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application:**
   - API Base URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`
   - H2 Console: `http://localhost:8080/h2-console`

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn test jacoco:report
```

---

## API Documentation (Swagger)

The API is documented using **OpenAPI 3.0** specification with **Swagger UI**.

### Access Swagger UI

Once the application is running, access the interactive API documentation at:

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON Specification

```
http://localhost:8080/v3/api-docs
```

### Features

- **Interactive Testing:** Try out API endpoints directly from the browser
- **Request/Response Examples:** View sample payloads and responses
- **Schema Documentation:** Detailed model definitions
- **Parameter Descriptions:** Clear documentation for all parameters

### Swagger UI Screenshot

The Swagger UI provides:
- **Forex Rates** section with 7 endpoints
- **Currencies** section with 1 endpoint
- Expandable endpoint details with "Try it out" functionality

---

## Test Coverage

### Running Coverage Report

```bash
mvn clean test jacoco:report
```

### Viewing Coverage Report

After running tests, open the coverage report at:
```
target/site/jacoco/index.html
```

### Test Summary

| Test Type | Test Class | Tests |
|-----------|------------|-------|
| **Unit Tests** | `BundesbankClientTest` | 10 |
| **Unit Tests** | `ExchangeRateQueryServiceTest` | 8 |
| **Unit Tests** | `CurrencyServiceTest` | 3 |
| **Unit Tests** | `ExchangeRateControllerTest` | 21 |
| **Unit Tests** | `CurrencyControllerTest` | 4 |
| **Integration Tests** | `ExchangeRateApiIntegrationTest` | 13 |
| **Integration Tests** | `RepositoryIntegrationTest` | 12 |
| **Integration Tests** | `SchedulerIntegrationTest` | 4 |
| **Context Test** | `ForexApplicationTests` | 1 |
| **Total** | | **76 Tests** |

### Coverage Metrics

- **Line Coverage:** ~90%+
- **Branch Coverage:** ~85%+
- **Class Coverage:** 100%

---

## Configuration

### Application Properties

Configuration is managed through Spring profiles:

| File | Purpose |
|------|---------|
| `application.properties` | Common/default configuration |
| `application-local.properties` | Local development environment |
| `application-prod.properties` | Production environment |

### Key Configuration Properties

```properties
# Database
spring.datasource.url=jdbc:h2:mem:fxratedb

# Bundesbank API
bundesbank.api.base-url=https://api.statistiken.bundesbank.de/rest/data/BBEX3
bundesbank.api.default-observations=100

# Scheduler (Daily at 6 AM)
scheduler.exchange-rate.cron=0 0 6 * * ?

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

### Environment Profiles

```bash
# Run with local profile (default)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Project Structure

```
forex/
├── src/
│   ├── main/
│   │   ├── java/com/crewmeister/forex/
│   │   │   ├── ForexApplication.java              # Main entry (@EnableScheduling, @EnableRetry)
│   │   │   ├── client/
│   │   │   │   └── BundesbankClient.java          # External API client (@Retryable)
│   │   │   ├── config/
│   │   │   │   ├── BundesbankApiConfig.java       # API configuration properties
│   │   │   │   ├── DataLoader.java                # Startup data loader
│   │   │   │   ├── OpenApiConfig.java             # Swagger/OpenAPI configuration
│   │   │   │   ├── RestTemplateConfig.java        # HTTP client with timeouts
│   │   │   │   └── WebMvcConfig.java              # Interceptor registration
│   │   │   ├── controller/
│   │   │   │   ├── CurrencyController.java        # Currency endpoints (1 endpoint)
│   │   │   │   └── ExchangeRateController.java    # Exchange rate endpoints (7 endpoints)
│   │   │   ├── dto/
│   │   │   │   ├── ConversionDto.java             # Conversion response (record)
│   │   │   │   ├── CurrencyDto.java               # Currency response (record)
│   │   │   │   ├── ErrorResponse.java             # Standard error response
│   │   │   │   ├── ExchangeRateDataDto.java       # API data transfer (record)
│   │   │   │   ├── ExchangeRateDto.java           # Exchange rate response (record)
│   │   │   │   └── ExchangeRatesByDateDto.java    # Grouped rates response (record)
│   │   │   ├── entity/
│   │   │   │   ├── Currency.java                  # Currency JPA entity
│   │   │   │   └── ExchangeRate.java              # Exchange rate JPA entity
│   │   │   ├── exception/
│   │   │   │   ├── ExternalApiException.java      # External API failure (→ 503)
│   │   │   │   ├── GlobalExceptionHandler.java    # Centralized exception handling
│   │   │   │   ├── InvalidParameterException.java # Bad request (→ 400)
│   │   │   │   └── ResourceNotFoundException.java # Not found (→ 404)
│   │   │   ├── interceptor/
│   │   │   │   └── StrictParameterValidationInterceptor.java  # Unknown param rejection
│   │   │   ├── mapper/
│   │   │   │   ├── CurrencyMapper.java            # Currency entity ↔ DTO
│   │   │   │   └── ExchangeRateMapper.java        # ExchangeRate entity ↔ DTO
│   │   │   ├── repository/
│   │   │   │   ├── CurrencyRepository.java        # Currency data access
│   │   │   │   └── ExchangeRateRepository.java    # Exchange rate data access
│   │   │   ├── scheduler/
│   │   │   │   └── ScheduledExchangeRateUpdater.java  # Daily sync job (6 AM)
│   │   │   └── service/
│   │   │       ├── ICurrencyService.java              # Currency interface
│   │   │       ├── CurrencyService.java               # Currency implementation
│   │   │       ├── ICurrencyConversionService.java    # Conversion interface
│   │   │       ├── CurrencyConversionService.java     # Conversion implementation
│   │   │       ├── IExchangeRateQueryService.java     # Query interface (CQRS read)
│   │   │       ├── ExchangeRateQueryService.java      # Query implementation
│   │   │       ├── IExchangeRateSyncService.java      # Sync interface (CQRS write)
│   │   │       └── ExchangeRateSyncService.java       # Sync implementation
│   │   └── resources/
│   │       ├── application.properties             # Common configuration
│   │       ├── application-local.properties       # Local dev config
│   │       ├── application-prod.properties        # Production config
│   │       └── data.sql                           # Initial currency seed data (29 currencies)
│   └── test/
│       └── java/com/crewmeister/forex/
│           ├── ForexApplicationTests.java
│           ├── client/
│           │   └── BundesbankClientTest.java          # 10 tests
│           ├── controller/
│           │   ├── CurrencyControllerTest.java        # 4 tests
│           │   └── ExchangeRateControllerTest.java    # 21 tests
│           ├── integration/
│           │   ├── ExchangeRateApiIntegrationTest.java # 13 tests
│           │   ├── RepositoryIntegrationTest.java      # 12 tests
│           │   └── SchedulerIntegrationTest.java       # 4 tests
│           └── service/
│               ├── CurrencyServiceTest.java           # 3 tests
│               └── ExchangeRateQueryServiceTest.java  # 8 tests
├── pom.xml                                        # Maven configuration
└── README.md                                      # This file
```

---

## License

This project is licensed under the MIT License.

---

## Author

Forex API - Foreign Exchange Rate Service for Crewmeister
