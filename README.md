# Foreign Exchange Rate API

A Spring Boot REST API service that fetches and serves foreign exchange rates from the Bundesbank API. The service provides endpoints for retrieving currencies, exchange rates, and currency conversion.

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
│  │                           SERVICES                                    │  │
│  │  ┌─────────────────────────┐    ┌──────────────────────────────────┐  │  │
│  │  │    CurrencyService      │    │    ExchangeRateService           │  │  │
│  │  │    - getAllCurrencies   │    │    - getExchangeRates            │  │  │
│  │  │                         │    │    - getExchangeRate             │  │  │
│  │  │                         │    │    - convertCurrency             │  │  │
│  │  │                         │    │    - syncExchangeRates           │  │  │
│  │  └─────────────────────────┘    └──────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                       │                                      │
│                    ┌──────────────────┴──────────────────┐                   │
│                    ▼                                     ▼                   │
│  ┌─────────────────────────────────┐    ┌─────────────────────────────────┐ │
│  │        REPOSITORIES             │    │      BUNDESBANK CLIENT          │ │
│  │  ┌───────────────────────────┐  │    │  ┌───────────────────────────┐  │ │
│  │  │   CurrencyRepository      │  │    │  │   BundesbankClient        │  │ │
│  │  │   ExchangeRateRepository  │  │    │  │   - fetchExchangeRates    │  │ │
│  │  └───────────────────────────┘  │    │  │   - fetchLatestRates      │  │ │
│  └─────────────────────────────────┘    │  └───────────────────────────┘  │ │
│                    │                     └─────────────────────────────────┘ │
│                    ▼                                     │                   │
│  ┌─────────────────────────────────┐                     │                   │
│  │         H2 DATABASE             │                     │                   │
│  │  ┌───────────────────────────┐  │                     │                   │
│  │  │   currencies              │  │                     │                   │
│  │  │   exchange_rates          │  │                     │                   │
│  │  └───────────────────────────┘  │                     │                   │
│  └─────────────────────────────────┘                     │                   │
│                                                          │                   │
│  ┌───────────────────────────────────────────────────────┴────────────────┐ │
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
│              │     │              │     │    Client    │     │   (H2)       │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                                                                      │
┌──────────────┐     ┌──────────────┐     ┌──────────────┐            │
│   Client     │────▶│  Controller  │────▶│   Service    │◀───────────┘
│   Request    │     │              │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │   Response   │
                     │   (JSON)     │
                     └──────────────┘
```

---

## API Endpoints & Examples

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
    "name": "Euro",
    "symbol": "€",
    "countryCode": "EU",
    "countryName": "European Union"
  },
  {
    "code": "USD",
    "name": "United States Dollar",
    "symbol": "$",
    "countryCode": "USA",
    "countryName": "United States"
  },
  {
    "code": "GBP",
    "name": "British Pound Sterling",
    "symbol": "£",
    "countryCode": "GBR",
    "countryName": "United Kingdom"
  }
]
```

---

### 2. Get All EUR-FX Exchange Rates (All Dates)

**Endpoint:** `GET /api/forex-rates/{base}?allDates=true`

**Description:** Returns all EUR-FX exchange rates at all available dates as a collection.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/EUR?allDates=true"
```

**Response:**
```json
[
  {
    "date": "2026-03-14",
    "baseCurrency": "EUR",
    "rates": [
      {
        "pair": "EUR/USD",
        "baseCurrency": "EUR",
        "targetCurrency": "USD",
        "rate": 1.0876,
        "date": "2026-03-14"
      },
      {
        "pair": "EUR/GBP",
        "baseCurrency": "EUR",
        "targetCurrency": "GBP",
        "rate": 0.8392,
        "date": "2026-03-14"
      }
    ]
  },
  {
    "date": "2026-03-13",
    "baseCurrency": "EUR",
    "rates": [
      {
        "pair": "EUR/USD",
        "baseCurrency": "EUR",
        "targetCurrency": "USD",
        "rate": 1.0912,
        "date": "2026-03-13"
      }
    ]
  }
]
```

---

### 3. Get EUR-FX Exchange Rate at a Particular Day

**Endpoint:** `GET /api/forex-rates/{base}?date={YYYY-MM-DD}`

**Description:** Returns the EUR-FX exchange rates for a specific date.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/EUR?date=2026-03-01"
```

**Response:**
```json
[
  {
    "date": "2026-03-01",
    "baseCurrency": "EUR",
    "rates": [
      {
        "pair": "EUR/USD",
        "baseCurrency": "EUR",
        "targetCurrency": "USD",
        "rate": 1.0823,
        "date": "2026-03-01"
      },
      {
        "pair": "EUR/GBP",
        "baseCurrency": "EUR",
        "targetCurrency": "GBP",
        "rate": 0.8356,
        "date": "2026-03-01"
      }
    ]
  }
]
```

---

### 4. Get Specific Currency Pair Exchange Rate

**Endpoint:** `GET /api/forex-rates/{base}/{target}?date={YYYY-MM-DD}`

**Description:** Returns the exchange rate for a specific currency pair on a particular day.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/EUR/USD?date=2026-03-01"
```

**Response:**
```json
{
  "pair": "EUR/USD",
  "baseCurrency": "EUR",
  "targetCurrency": "USD",
  "rate": 1.0823,
  "date": "2026-03-01"
}
```

---

### 5. Convert Currency Amount

**Endpoint:** `GET /api/forex-rates/convert?from={currency}&to={currency}&amount={value}&date={YYYY-MM-DD}`

**Description:** Converts a foreign exchange amount from one currency to another on a particular day.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/forex-rates/convert?from=USD&to=EUR&amount=100&date=2026-03-01"
```

**Response:**
```json
{
  "sourceCurrency": "USD",
  "sourceAmount": 100.00,
  "targetCurrency": "EUR",
  "convertedAmount": 92.40,
  "exchangeRate": 0.9240,
  "date": "2026-03-01"
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
  "message": "Exchange rate not found for EUR/XXX"
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
- **Forex Rates** section with 4 endpoints
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
| **Unit Tests** | `ExchangeRateServiceTest` | 18 |
| **Unit Tests** | `CurrencyServiceTest` | 3 |
| **Unit Tests** | `ExchangeRateControllerTest` | 13 |
| **Unit Tests** | `CurrencyControllerTest` | 4 |
| **Unit Tests** | `BundesbankClientTest` | 10 |
| **Integration Tests** | `ExchangeRateApiIntegrationTest` | 13 |
| **Integration Tests** | `SchedulerIntegrationTest` | 4 |
| **Integration Tests** | `RepositoryIntegrationTest` | 12 |
| **Context Test** | `ForexApplicationTests` | 1 |
| **Total** | | **78 Tests** |

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
│   │   │   ├── ForexApplication.java          # Main application entry
│   │   │   ├── client/
│   │   │   │   └── BundesbankClient.java      # External API client
│   │   │   ├── config/
│   │   │   │   ├── BundesbankApiConfig.java   # API configuration
│   │   │   │   ├── DataLoader.java            # Startup data loader
│   │   │   │   ├── OpenApiConfig.java         # Swagger configuration
│   │   │   │   └── WebMvcConfig.java          # Web MVC configuration
│   │   │   ├── controller/
│   │   │   │   ├── CurrencyController.java    # Currency endpoints
│   │   │   │   └── ExchangeRateController.java # Exchange rate endpoints
│   │   │   ├── dto/
│   │   │   │   ├── ConversionDto.java         # Conversion response
│   │   │   │   ├── CurrencyDto.java           # Currency response
│   │   │   │   ├── ExchangeRateDto.java       # Exchange rate response
│   │   │   │   └── ExchangeRatesByDateDto.java # Grouped rates response
│   │   │   ├── entity/
│   │   │   │   ├── Currency.java              # Currency entity
│   │   │   │   └── ExchangeRate.java          # Exchange rate entity
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java # Exception handling
│   │   │   │   ├── InvalidParameterException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   │   ├── CurrencyRepository.java    # Currency data access
│   │   │   │   └── ExchangeRateRepository.java # Exchange rate data access
│   │   │   ├── scheduler/
│   │   │   │   └── ScheduledExchangeRateUpdater.java # Daily sync job
│   │   │   └── service/
│   │   │       ├── CurrencyService.java       # Currency business logic
│   │   │       └── ExchangeRateService.java   # Exchange rate business logic
│   │   └── resources/
│   │       ├── application.properties         # Main configuration
│   │       ├── application-local.properties   # Local environment config
│   │       └── data.sql                       # Initial currency data
│   └── test/
│       └── java/com/crewmeister/forex/
│           ├── client/
│           │   └── BundesbankClientTest.java
│           ├── controller/
│           │   ├── CurrencyControllerTest.java
│           │   └── ExchangeRateControllerTest.java
│           ├── integration/
│           │   ├── ExchangeRateApiIntegrationTest.java
│           │   ├── RepositoryIntegrationTest.java
│           │   └── SchedulerIntegrationTest.java
│           └── service/
│               ├── CurrencyServiceTest.java
│               └── ExchangeRateServiceTest.java
├── pom.xml                                    # Maven configuration
└── README.md                                  # This file
```

---

## License

This project is licensed under the MIT License.

---

## Author

Forex API - Foreign Exchange Rate Service for Crewmeister
