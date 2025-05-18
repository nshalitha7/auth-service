# ChargePoint Authorization Service

Java 21 + Spring Boot 3 application, that simulates an asynchronous RFID-based eDriver authorization system
using Kafka.

## Features

- **REST API** to authorize driver identifiers(token)
- **In-memory whitelist** for simple e-driver verification
- **Kafka async flow** with correlation ID support
- **Timeout & error fallback** responses
- **Unit & integration tests** with coverage reports
- **Docker Compose** for Kafka, Zookeeper, and service orchestration
- **Swagger/OpenAPI** for API documentation
- **JaCoCo** test coverage reporting

## Tech Stack

- **Language** - Java 21
- **Framework** - Spring Boot 3.4.5
- **Messaging** - Apache Kafka + Spring Kafka |
- **REST Docs** - SpringDoc OpenAPI (Swagger UI)
- **Testing** - JUnit 5, Mockito, Embedded Kafka
- **Build Tool** - Maven
- **DevOps** - Docker, Docker Compose
- **Code Coverage** - JaCoCo

## Run the Project

#### Using Docker

```bash
  docker-compose up --build
```

## Run Tests

Unit & Integration Tests

```bash
  mvn clean verify
```

- Unit tests use JUnit 5 & Mockito
- Integration tests use @SpringBootTest + @EmbeddedKafka

## Test Coverage Report

After running tests,

```bash
  open target/site/jacoco/index.html
```

## API Documentation (Swagger UI)

Once the service is running, visit,

```bash
  http://localhost:8080/swagger-ui.html
```

## Docker Containers

```bash
  docker ps
```

Expected services:

- auth-service — Spring Boot app
- kafka — Kafka broker
- zookeeper — Coordination for Kafka

## Sample Kafka Topics

- auth-requests — Produced by REST endpoint
- auth-responses — Consumed to fulfill async responses

## REST Endpoint

### POST `/api/authorize`

Asynchronously authorizes a driver identifier

#### Request Body

```bash
    {
      "stationUuid": "550e8400-e29b-41d4-a716-446655440000",
      "driverIdentifier": {
        "id": "id12345678901234567890"
      }
    }
```

#### Sample Responses

```bash
    { "authorizationStatus": "Accepted" }
    { "authorizationStatus": "Rejected" }
    { "authorizationStatus": "Timeout" }
    { "authorizationStatus": "InternalError" }
```

## Authorization Flow

#### 1. Client Sends Authorization Request

- A charging station makes an HTTP POST request to the API

```bash
    {
      "stationUuid": "550e8400-e29b-41d4-a716-446655440000",
      "driverIdentifier": {
        "id": "id12345678901234567890"
      }
    }
```

#### 2. Validation

The controller immediately validates the driverIdentifier.id and
must be between 20 and 80 characters. If invalid, responds with
status "Invalid".

#### 3. Produce Kafka Message

If valid, a unique correlation ID is generated (UUID). A message is
published to the Kafka topic auth-requests with the correlation ID
and request details.

#### 4. Await Response Asynchronously

Controller waits up to 3 seconds for a matching Kafka response on the
auth-responses topic. During this time ResponseManager uses a
CompletableFuture mapped to the correlation ID.

#### 5. Backend Simulated Service

Kafka consumer processes the auth-requests topic and responds with an
authorization decision by publishing to auth-responses.

#### 6. Consume Response

The AuthorizationConsumer listens to auth-responses. It uses
the correlation ID and completes the corresponding CompletableFuture.
The REST controller receives response and responds.

```bash
    {
      "authorizationStatus": "Accepted"
    }
```

#### 7. Timeout & Error Handling

If no response arrives in time, returns status "Timeout" with
HTTP 504. Other Possible exceptions are "Interrupted" or
"InternalError" with HTTP 500.

### Future Enhancements

- Replace in-memory whitelist with persistent DB(PostgreSQL, Redis)
- Retry Mechanism for Kafka Failures
- Distributed Tracing
- Response Cache Expiry & Cleanup mechanism
- Secure endpoints with OAuth2/JWT using Spring Security
- API rate limiting
- Docker healthcheck
- Add Prometheus/Grafana monitoring with metrics