# AI Usage Documentation

This document describes how AI tools were used during the development of the Foreign Exchange Rate API project, including the prompts, responses, and reasoning behind accepting or modifying suggestions.

---

## AI Tool Used

| Tool | Purpose |
|------|---------|
| **Windsurf Cascade (Claude)** | Code implementation, debugging, testing, and documentation |

---

## Development Approach

The development followed a **human-driven, AI-assisted** approach where:

- **I (the developer) made all design decisions** and provided stage-by-stage requirements
- **AI implemented the code** based on my specifications and guidance
- **I reviewed, tested, and approved** each implementation before proceeding
- **I directed debugging efforts** and decided on fixes when issues arose

---

## Key Prompts & AI Interactions

### 1. Initial API Design - Exchange Rate Endpoints

**My Prompt:**
> "I need REST endpoints for exchange rates with the following requirements:
> - `GET /api/forex-rates/{base}` - Get all exchange rates from a base currency (EUR)
> - `GET /api/forex-rates/{base}/{target}` - Get specific currency pair rate
> - Support optional `date` parameter for historical rates (format: YYYY-MM-DD)
> - Support `allDates=true` parameter to fetch rates for all available dates
> - Handle inverse rate calculation when base currency is not EUR
> - Return rates grouped by date when fetching multiple dates
> - Use proper DTOs for response structure"

**AI Response:**
- Created `ExchangeRateController.java` with both endpoints
- Implemented `ExchangeRateService` with business logic
- Added support for date filtering and `allDates` parameter
- Implemented inverse rate calculation for non-EUR base currencies
- Created `ExchangeRateDto` and `ExchangeRatesByDateDto` for responses

**My Decision:** ✅ **Accepted**
- Endpoints followed RESTful conventions as I specified
- Service layer properly separated from controller
- All my requirements for date handling and grouping were implemented

---

### 2. DataLoader - Startup Data Synchronization

**My Prompt:**
> "I need data to be available immediately when the application starts. Requirements:
> - Fetch exchange rates from Bundesbank API on application startup
> - Use `ApplicationRunner` to trigger the sync after Spring context is ready
> - Log the sync status (start, completion, any errors)
> - This ensures users don't have to wait for the first scheduled job to run"

**AI Response:**
- Created `DataLoader.java` implementing `ApplicationRunner`
- Calls `exchangeRateService.syncExchangeRates()` on startup
- Added logging for sync status

**My Decision:** ✅ **Accepted** - This was my design decision
- I specified the `ApplicationRunner` pattern
- Data availability on startup was my requirement
- AI implemented exactly as I specified

---

### 3. Scheduled Job Implementation

**My Prompt:**
> "Implement a scheduled job with the following specifications:
> - Fetch latest exchange rates from Bundesbank API daily
> - Schedule execution at 6 AM every day using cron expression
> - Externalize the cron expression to `application.properties` for configurability
> - Use upsert logic - insert new rates, update existing ones
> - Add proper logging for job start, completion, and error handling
> - Handle API failures gracefully without crashing the application"

**AI Response:**
- Created `ScheduledExchangeRateUpdater.java` with `@Scheduled` annotation
- Implemented cron expression for daily 6 AM execution (`0 0 6 * * ?`)
- Externalized cron to `scheduler.exchange-rate.cron` property
- Added comprehensive logging and error handling

**My Decision:** ✅ **Accepted**
- All my specifications were implemented
- Cron externalization allows easy configuration changes without code modification

---

### 4. Currency Conversion Endpoint

**My Prompt:**
> "Add a currency conversion endpoint with the following requirements:
> - Endpoint: `GET /api/forex-rates/convert`
> - Parameters: `from` (source currency), `to` (target currency), `amount`, optional `date`
> - Validation rules:
>   - `from` and `to` are required and must be valid 3-letter currency codes
>   - `amount` must be positive and greater than zero
>   - `from` and `to` must be different currencies
>   - `date` cannot be a future date
> - Return a `ConversionDto` with source amount, converted amount, exchange rate used, and date
> - Handle missing required parameters with proper 400 Bad Request responses"

**AI Response:**
- Created `GET /api/forex-rates/convert` endpoint
- Added `convertCurrency()` method in `ExchangeRateService`
- Implemented `validateConversionParams()` with all specified validation rules
- Added `MissingServletRequestParameterException` handler in `GlobalExceptionHandler`

**My Decision:** ✅ **Accepted**
- All my validation rules were implemented exactly as specified
- Error responses follow consistent format across the API

---

### 5. Test Implementation

**My Prompt:**
> "Add comprehensive tests for the application:
> - Unit tests for all service classes (ExchangeRateService, CurrencyService)
> - Unit tests for controllers using MockMvc
> - Unit tests for BundesbankClient with mocked RestTemplate
> - Integration tests for API endpoints with full Spring context
> - Integration tests for scheduler execution
> - Integration tests for repository operations
> - Add JaCoCo plugin for code coverage reporting"

**AI Response:**
- Created `ExchangeRateQueryServiceTest` (8 tests)
- Created `CurrencyServiceTest` (3 tests)
- Created `ExchangeRateControllerTest` (21 tests)
- Created `CurrencyControllerTest` (4 tests)
- Created `BundesbankClientTest` (10 tests)
- Created `ExchangeRateApiIntegrationTest` (13 tests)
- Created `SchedulerIntegrationTest` (4 tests)
- Created `RepositoryIntegrationTest` (12 tests)
- Added JaCoCo plugin to pom.xml

**My Decision:** ✅ **Accepted**
- Comprehensive test coverage (76 tests total)
- All layers properly tested as I requested

---

### 6. Test Failures - Database Constraint Violations

**My Prompt:**
> "Tests are failing with this error:
> `Tests run: 12, Failures: 0, Errors: 2, Skipped: 0`
> `Unique index or primary key violation: CURRENCIES(CODE) VALUES ('EUR')`
> Please investigate and fix the test isolation issue."

**AI Response (after multiple iterations):**
- First attempt: `@DirtiesContext` - didn't work
- Second attempt: `@AutoConfigureTestDatabase` - partially worked
- Final solution:
  - Added `@TestPropertySource(properties = {"spring.sql.init.mode=never"})` to disable `data.sql`
  - Added `@MockBean DataLoader` to prevent startup data loading in tests

**My Decision:** ✅ **Accepted after guiding the debugging**
- I provided detailed error messages to help identify the root cause
- AI iterated through solutions until finding the correct approach
- Final solution properly isolates test data from production data

---

### 7. Swagger/OpenAPI Documentation

**My Prompt:**
> "Check if OpenAPI documentation is properly configured. I want to:
> - Access Swagger UI to test endpoints interactively
> - View the OpenAPI JSON specification
> - Ensure all endpoints are documented with descriptions and examples"

**AI Response:**
- Confirmed existing `@Operation`, `@ApiResponse`, `@Parameter` annotations in controllers
- Provided Swagger UI URL: `http://localhost:8080/swagger-ui/index.html`
- Provided OpenAPI JSON URL: `http://localhost:8080/v3/api-docs`

**Follow-up Issue:**
> "Swagger UI shows 500 error: `java.lang.NoSuchMethodError: ControllerAdviceBean.<init>(Object)`
> Please fix the compatibility issue."

**AI Response:**
- Researched the issue and found SpringDoc 2.3.0 incompatible with Spring Boot 3.5.11
- Upgraded SpringDoc to version 2.8.9 which supports Spring Framework 6.2.x
- Created `OpenApiConfig.java` with API metadata

**My Decision:** ✅ **Accepted**
- AI correctly identified the root cause through web search
- Version upgrade resolved the compatibility issue

---

### 8. Configuration Best Practices

**My Prompt:**
> "Is it good design to have URL hardcoded in the class like `localServer.setUrl("http://localhost:8080")`?
> I think this should be externalized for different environments."

**AI Response:**
- Agreed with my assessment that hardcoding is not a good practice
- Externalized to `application.properties` using `@Value("${server.url}")`

**My Decision:** ✅ **Accepted** - AI implemented my design decision

**Follow-up:**
> "Should this property be in `application.properties` or `application-local.properties`?
> I want environment-specific configuration separated from common settings."

**AI Response:**
- Created `application-local.properties` for local environment settings
- Moved `server.url` to the local profile
- Added `spring.profiles.active=local` to main properties
- Explained how to create additional profiles for staging/production

**My Decision:** ✅ **Accepted** - This follows Spring Boot best practices for profile-based configuration

---

### 9. Deprecated Annotation Warnings

**My Prompt:**
> "Tests are showing deprecation warnings:
> `@MockBean in org.springframework.boot.test.mock.mockito has been deprecated`
> `@SpyBean in org.springframework.boot.test.mock.mockito has been deprecated`
> Please update to the recommended annotations for Spring Boot 3.5.x"

**AI Response:**
- Replaced `@MockBean` with `@MockitoBean`
- Replaced `@SpyBean` with `@MockitoSpyBean`
- Updated imports to use `org.springframework.test.context.bean.override.mockito`

**My Decision:** ✅ **Accepted**
- AI correctly identified the Spring Boot 3.5.x migration requirements
- Tests now run without deprecation warnings

---

### 10. README Documentation

**My Prompt:**
> "Create a comprehensive README.md with the following sections:
> 1. Design flow diagram (architecture overview)
> 2. API endpoint examples with curl commands and JSON responses for:
>    - GET /api/currencies
>    - GET /api/forex-rates/EUR (all dates)
>    - GET /api/forex-rates/EUR?date=2026-03-01
>    - GET /api/forex-rates/EUR/USD
>    - GET /api/forex-rates/convert
> 3. Technology stack with versions (Java, Spring Boot, H2, etc.)
> 4. Test coverage summary
> 5. Swagger/OpenAPI documentation details
> 6. Getting started instructions
> 7. Project structure
> Order these sections based on importance and logical flow."

**AI Response:**
- Created comprehensive README.md with all requested sections
- Added ASCII architecture and data flow diagrams
- Included curl examples with realistic JSON responses
- Created technology stack table with exact versions
- Documented test coverage (76 tests across 9 test classes)
- Added project structure tree
- Organized sections in logical order

**My Decision:** ✅ **Accepted**
- All my requirements were addressed
- Documentation is comprehensive and well-organized

---

### 11. Convert DTOs to Immutable Java Records

**My Prompt:**
> "Convert CurrencyDto to an immutable Java record instead of using Lombok @Data."

**AI Response:**
- Converted `CurrencyDto` from mutable class with `@Data` to immutable Java record
- Updated `CurrencyServiceTest` to use record accessor style (`code()` instead of `getCode()`)

**My Decision:** ✅ **Accepted**
- Records are thread-safe and more concise
- Better expresses immutable data transfer objects

**Follow-up:**
> "Apply the same pattern to all other DTOs: ExchangeRateDto, ConversionDto, ExchangeRatesByDateDto, ExchangeRateDataDto"

**AI Response:**
- Converted all remaining DTOs to Java records

**My Decision:** ✅ **Accepted**
- Consistent DTO pattern across the codebase

---

### 12. Fix JPA Entity equals/hashCode Issues

**My Prompt:**
> "The Currency and ExchangeRate entities use Lombok @Data which generates equals/hashCode based on all fields including ID. This can cause issues with JPA. Fix this by using manual equals/hashCode based on business keys."

**AI Response:**
- Replaced `@Data` with `@Getter`, `@Setter`, `@ToString` on `Currency` and `ExchangeRate` entities
- Added manual `equals()` and `hashCode()` methods:
  - `Currency`: based on `code` (business key)
  - `ExchangeRate`: based on `sourceCurrency`, `targetCurrency`, `date` (composite business key)

**My Decision:** ✅ **Accepted**
- Avoids JPA issues with entity equality based on mutable ID
- Follows JPA best practices for entity design

---

### 13. Extract Service Interfaces

**My Prompt:**
> "Extract interfaces for all service classes to follow Dependency Inversion Principle and improve testability."

**AI Response:**
- Created `ICurrencyService` interface with `getAllCurrencies()` method
- Updated `CurrencyService` to implement the interface
- Updated `CurrencyController` and `CurrencyControllerTest` to inject the interface instead of concrete class

**My Decision:** ✅ **Accepted**
- Controllers depend on abstractions, not concrete implementations
- Easier to mock in tests

**Follow-up:**
> "Apply the same pattern to ExchangeRateService and CurrencyConversionService"

**AI Response:**
- Created `IExchangeRateQueryService`, `ICurrencyConversionService`, `IExchangeRateSyncService`
- Updated controllers and tests to inject interfaces

**My Decision:** ✅ **Accepted**
- Consistent interface-based design across all services

---

### 14. Implement CQRS-lite Pattern

**My Prompt:**
> "Split ExchangeRateService into separate services for read and write operations to follow CQRS pattern."

**AI Response:**
- Created `ExchangeRateQueryService` for all read operations (queries)
- Created `ExchangeRateSyncService` for all write operations (data synchronization)
- Updated controllers to inject the appropriate service based on operation type

**My Decision:** ✅ **Accepted**
- Clearer separation of concerns
- Read and write operations can scale independently if needed

---

### 15. Extract Mapper Components

**My Prompt:**
> "Create dedicated mapper components for entity-to-DTO conversions instead of inline mapping logic."

**AI Response:**
- Created `CurrencyMapper` as `@Component` with `toDto()` method
- Created `ExchangeRateMapper` as `@Component` with `toDto()`, `toEntity()`, and `updateEntity()` methods
- Updated services to inject and use mappers
- Updated tests to use `@Spy` for mappers so real mapping logic is tested

**My Decision:** ✅ **Accepted**
- Centralized mapping logic
- Consistent with Spring's component-based architecture
- Easier to test and maintain

---

### 16. Add API Resilience with Spring Retry

**My Prompt:**
> "Add retry logic to external API calls in BundesbankClient to handle transient failures like network glitches."

**AI Response:**
- Added `spring-retry` and `spring-aspects` dependencies to `pom.xml`
- Added `@EnableRetry` to `ForexApplication.java`
- Added `@Retryable` annotations to `fetchAllExchangeRates()` and `fetchLatestExchangeRates()`
- Configured: retry on `RestClientException`, max 3 attempts, exponential backoff (1s → 2s → 4s)

**Follow-up Issue:**
> "The @Retryable won't trigger because the try-catch block catches RestClientException before Spring Retry can intercept it."

**AI Response:**
- Removed `try-catch` wrapping around `RestClientException`
- Added `@Recover` method as final fallback that throws `ExternalApiException`
- Updated unit tests to expect `RestClientException` propagation (unit tests don't have Spring AOP proxy)
- Removed unused `RoundingMode` import

**My Decision:** ✅ **Accepted**
- Retry mechanism now works correctly
- Transient failures are handled gracefully with exponential backoff
- Final failure returns HTTP 503 Service Unavailable

---

### 17. Simplify README Documentation

**My Prompt:**
> "Simplify README: show only 4 core endpoints with examples, list all other endpoints in a table without examples. Highlight that we load last 100 records per currency at startup."

**AI Response:**
- Added note about loading 100 records per currency at startup
- Created "Available Endpoints" table listing all 10 endpoints
- Kept detailed examples for only 4 core use cases:
  1. Get all currencies
  2. Get all EUR-FX rates (all dates)
  3. Get EUR-FX rate at particular day
  4. Convert currency amount
- Removed verbose examples for history/pagination endpoints

**My Decision:** ✅ **Accepted**
- README is now cleaner and focused on core use cases
- All endpoints still documented in the table

---

## Rejected/Modified AI Suggestions

### 1. Initial Test Isolation Approach

**AI Suggestion:** Use `@DirtiesContext` annotation

**My Decision:** ❌ **Rejected** (didn't work)
- This approach didn't resolve the database constraint violations
- AI then tried `@AutoConfigureTestDatabase` which also didn't fully work
- Final solution required combining multiple approaches

### 2. SpringDoc Version Selection

**AI Initial Suggestion:** Upgrade to SpringDoc 2.6.0

**My Decision:** ❌ **Rejected** (still had errors)
- Version 2.6.0 still had the `ControllerAdviceBean` compatibility issue
- AI then researched and found version 2.8.9 was required for Spring Boot 3.5.x

---

## Summary of AI Contributions

| Area | AI Contribution | My Contribution |
|------|-----------------|-----------------|
| **Architecture** | Implemented code structure | Designed the overall architecture |
| **API Design** | Implemented endpoints | Specified endpoint paths, parameters, responses |
| **Validation** | Implemented validation logic | Defined validation rules |
| **Testing** | Wrote test cases | Directed test requirements, reviewed results |
| **Debugging** | Proposed solutions | Provided error messages, decided on fixes |
| **Configuration** | Implemented property injection | Decided on externalization approach |
| **Code Quality** | Implemented refactorings | Identified improvement areas through manual review |
| **Documentation** | Generated README content | Specified sections and requirements |

---

## Lessons Learned

1. **Iterative Debugging:** Some issues required multiple attempts to resolve (e.g., test isolation)
2. **Version Compatibility:** AI needed to research current compatibility requirements for newer Spring Boot versions
3. **Human Oversight:** My design decisions and stage-by-stage guidance were essential for a coherent solution
4. **Code Review:** Manual code review identified areas for improvement (DTOs, entities, service interfaces, CQRS, mappers)
5. **AI as Implementation Partner:** AI excels at implementing well-specified requirements but benefits from human architectural guidance

---

## Conclusion

The AI tool was used as an **implementation assistant** while I maintained control over:
- Design decisions
- Architecture choices
- Validation rules
- Configuration approaches
- Code quality improvements (identified through manual review)
- Final approval of all changes

This collaborative approach resulted in a well-structured, tested, and documented application that meets all requirements.
