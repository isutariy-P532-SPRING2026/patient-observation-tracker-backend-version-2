# Patient Observation Tracker — Backend

[![CI](https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-backend/actions/workflows/ci.yml)

**Live URL:** https://patient-observation-tracker-backend-cad5.onrender.com

Spring Boot REST API for the Patient Observation Tracker system. Provides endpoints for managing patients, observations, diagnostic rules, protocols, phenomenon types, and audit/command logs using a strict four-layer architecture and four design patterns.

---

## Tech Stack

- Java 21 · Spring Boot 3.2.5 · Spring Data JPA
- SQLite (via `sqlite-jdbc` + Hibernate Community Dialects)
- Maven · Docker · GitHub Actions CI/CD · Render.com deployment

---

## Quick Start with Docker

```bash
# Build the image
docker build -t tracker .

# Run (SQLite data persists in ./data/)
docker run -p 8080:8080 -v $(pwd)/data:/app/data tracker
```

Open [http://localhost:8080](http://localhost:8080) — the frontend is served from `src/main/resources/static/`.

---

## Run Locally (without Docker)

```bash
# Create the data directory
mkdir data

# Run the app
mvn spring-boot:run
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/patients` | List all patients |
| POST | `/api/patients` | Create patient |
| GET | `/api/patients/{id}/observations` | List observations |
| POST | `/api/observations/measurement` | Record measurement |
| POST | `/api/observations/category` | Record category observation |
| POST | `/api/observations/{id}/reject` | Reject observation |
| POST | `/api/patients/{id}/evaluate` | Run diagnostic rules |
| GET | `/api/phenomenon-types` | List phenomenon types |
| POST | `/api/phenomenon-types` | Create phenomenon type |
| GET | `/api/phenomenon-types/{id}/phenomena` | List phenomena for a type |
| POST | `/api/phenomenon-types/{id}/phenomena` | Add phenomenon to a type |
| GET | `/api/protocols` | List protocols |
| POST | `/api/protocols` | Create protocol |
| GET | `/api/rules` | List associative functions (rules) |
| POST | `/api/rules` | Create a diagnostic rule |
| GET | `/api/command-log` | View command log |
| GET | `/api/audit-log` | View audit log |

---

## Architecture — Four Layers

```
Client (@RestController)  →  HTTP only, zero business logic
Manager (@Service)         →  Orchestrates use-case sequences
Engine (@Service)          →  Encapsulates a replaceable algorithm
ResourceAccess (@Repository) →  Atomic data operations
```

Controllers never instantiate domain objects directly. All `Observation` subtypes are created through `ObservationFactory`.

---

## Design Patterns

**Factory (`ObservationFactory`):**
All `Measurement` and `CategoryObservation` objects are constructed exclusively through `ObservationFactory`, which validates that the phenomenon type's kind (QUANTITATIVE/QUALITATIVE) matches the observation kind and that the unit belongs to the allowed set — before any object is created. Lives in `com.tracker.factory`.

**Strategy (`DiagnosisEngine` + `SimpleConjunctiveStrategy`):**
The rule-evaluation algorithm is injected into `DiagnosisEngine` as a `DiagnosisStrategy` interface. `SimpleConjunctiveStrategy` fires a rule only when all argument observation concepts are present in a patient's active observations. A new strategy (e.g. `WeightedScoringStrategy`) can be swapped in for Week 2 without modifying the engine. Lives in `com.tracker.strategy` and `com.tracker.engine`.

**Observer (Spring `ApplicationEventPublisher`):**
`ObservationManager` publishes an `ObservationEvent` via Spring's event bus whenever an observation is created or rejected. Two listeners handle side-effects independently: `AuditLogListener` appends an audit entry, and `RuleEvaluationListener` re-evaluates all active diagnostic rules. Adding more listeners in Week 2 requires zero changes to existing code. Lives in `com.tracker.event`.

**Command (`BaseCommand` + `CommandLog`):**
Every state-changing action (create patient, record observation, reject observation) is wrapped in a `BaseCommand` carrying an `execute()` method and a JSON payload snapshot. `CommandLog.record()` executes the command then immediately persists a `CommandLogEntry` (with user `"staff"` and timestamp) to the database. Lives in `com.tracker.command`.

---

## Project Structure

```
src/main/java/com/tracker/
├── config/          AppConfig, DataInitializer
├── domain/          JPA entities + enums
├── resourceaccess/  Spring Data repositories
├── factory/         ObservationFactory
├── strategy/        DiagnosisStrategy, SimpleConjunctiveStrategy
├── command/         Command, BaseCommand, CommandLog
├── event/           ObservationEvent, AuditLogListener, RuleEvaluationListener
├── engine/          DiagnosisEngine
├── manager/         PatientManager, ObservationManager, CatalogueManager, LogManager
└── controller/      PatientController, ObservationController, CatalogueController, LogController

src/main/resources/static/   Frontend HTML pages
src/test/java/com/tracker/   Unit tests (≥15, no @SpringBootTest)
```

---

## Running Tests

```bash
mvn test
```

Unit tests cover: `ObservationFactory` validation, `SimpleConjunctiveStrategy` logic, `BaseCommand` execution, `CommandLog` persistence, and `AuditLogListener` event handling. All tests use `@ExtendWith(MockitoExtension.class)` — no `@SpringBootTest`.

---

## CI/CD Pipeline

GitHub Actions (`.github/workflows/ci.yml`) runs three jobs:

1. **test** — compiles and runs all unit tests, uploads Surefire report
2. **build** — packages the JAR and builds the Docker image
3. **deploy** — triggers Render.com deploy hook (main branch only)

---

## Render.com Deployment

1. Web Service → Docker environment → port 8080
2. Disk: 100 MB mounted at `/app/data` (SQLite persistence)
3. Deploy hook stored as GitHub secret `RENDER_DEPLOY_HOOK`

---

## Seed Data

On first startup, `DataInitializer` auto-seeds the database with:
- **9 quantitative** phenomenon types (Body Temperature, Blood Glucose, Systolic/Diastolic BP, Heart Rate, Body Weight, Body Height, Oxygen Saturation, Respiratory Rate)
- **5 qualitative** phenomenon types with phenomena (Blood Group, Structural Condition, Pain Level, Level of Consciousness, Mobility Status)
- **10 protocols** (Standard BP, Fasting Glucose, Oral Temperature, Pulse Oximetry, etc.)
- **3 diagnostic rules** (Systemic Inflammatory Response, Metabolic Risk, Respiratory Compromise)
