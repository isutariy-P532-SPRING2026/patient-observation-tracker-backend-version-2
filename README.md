# Patient Observation Tracker — Backend (Version 2)

[![CI](https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-backend-version-2/actions/workflows/ci.yml/badge.svg)](https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-backend-version-2/actions/workflows/ci.yml)

**Live URL:** <https://isutariy-p532-spring2026.github.io/patient-observation-tracker-frontend-version-2/>

**Backend API:** <https://patient-observation-tracker-backend-hg23.onrender.com>

**Backend repo :** <https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-backend-version-2>

**Frontend repo :** <https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-frontend-version-2>

Spring Boot REST API for the Patient Observation Tracker system. Provides endpoints for managing patients, observations, diagnostic rules, protocols, phenomenon types, and audit/command logs using a strict four-layer architecture and six design patterns.

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
mkdir data
mvn spring-boot:run
```

---

## API Endpoints

### Patients & Observations

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/patients` | List all patients |
| POST | `/api/patients` | Create patient |
| GET | `/api/patients/{id}/observations` | List observations for a patient |
| POST | `/api/observations/measurement` | Record a measurement observation |
| POST | `/api/observations/category` | Record a category observation |
| POST | `/api/observations/{id}/reject` | Reject an active observation |
| POST | `/api/patients/{id}/evaluate` | Run all active diagnostic rules |

### Catalogue

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/phenomenon-types` | List all phenomenon types |
| POST | `/api/phenomenon-types` | Create a phenomenon type (quantitative or qualitative) |
| GET | `/api/phenomenon-types/{id}/phenomena` | List phenomena for a qualitative type |
| POST | `/api/phenomenon-types/{id}/phenomena` | Add a phenomenon to a qualitative type |
| GET | `/api/protocols` | List all protocols |
| POST | `/api/protocols` | Create a protocol |
| GET | `/api/rules` | List all associative functions (diagnostic rules) |
| POST | `/api/rules` | Create a diagnostic rule |

### Logs & Undo

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/command-log` | View full command log |
| POST | `/api/command-log/{id}/undo` | Undo a recorded command |
| GET | `/api/audit-log` | View full audit log |

### Users

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users |
| POST | `/api/users` | Create a user |

---

## Architecture — Four Layers

``` .
Client (@RestController)       →  HTTP only, zero business logic
Manager (@Service)             →  Orchestrates use-case sequences
Engine (@Service)              →  Encapsulates a replaceable algorithm
ResourceAccess (@Repository)   →  Atomic data operations
```

Controllers never instantiate domain objects directly. All `Observation` subtypes are created through `ObservationFactory`.

---

## Design Patterns

### Factory — `ObservationFactory`

Constructs `Measurement` and `CategoryObservation` objects exclusively through a factory that validates: the phenomenon type's kind (QUANTITATIVE/QUALITATIVE) matches the observation kind, and the unit belongs to the type's allowed set — before any object is created.

### Strategy — `DiagnosisEngine` + `DiagnosisStrategyFactory`

The rule-evaluation algorithm is injected as a `DiagnosisStrategy` interface. `DiagnosisStrategyFactory` selects the strategy at runtime based on the rule's `strategyHint`:

- **`SimpleConjunctiveStrategy`** — fires when all argument observation types are present in a patient's active observations (Week 1).
- **`WeightedScoringStrategy`** *(Week 2)* — fires when the weighted sum of present argument types meets or exceeds the rule's configurable `threshold`. Each argument type carries an individual `ArgumentWeight`.

### Observer — Spring `ApplicationEventPublisher`

`ObservationManager` publishes an `ObservationEvent` via Spring's event bus on every create or reject. Independent listeners handle side-effects with zero coupling to the manager:

- **`AuditLogListener`** — appends an audit log entry.
- **`RuleEvaluationListener`** — re-evaluates all active diagnostic rules.
- **`PropagationListener`** *(Week 2)* — propagates category observations up (ancestors) when PRESENT and down (descendants) when ABSENT, maintaining consistency in the Pain Level concept hierarchy.

### Command — `BaseCommand` + `CommandLog`

Every state-changing action is wrapped in a `BaseCommand` with a lazy `Supplier<String>` payload evaluated **after** `save()` completes (so the entity id is populated before serialization). `CommandLog.record()` executes the command and immediately persists a `CommandLogEntry` with the resolved payload, user, and timestamp.

### Decorator — Observation Processing Pipeline *(Week 2)*

Before an observation is saved, it passes through a chain of `ObservationProcessor` decorators:

1. **`UnitValidationDecorator`** — rejects units not in the phenomenon type's allowed set.
2. **`AnomalyFlaggingDecorator`** — sets `anomalyFlag = true` when a measurement falls outside `normalMin`/`normalMax`.
3. **`AuditStampingDecorator`** — stamps `recordingTime` if not already set.
4. **`BaseObservationProcessor`** — terminal processor; returns the observation unchanged.

New processing steps can be added without modifying existing decorators.

### Undo — `UndoService` *(Week 2)*

`POST /api/command-log/{id}/undo` delegates to `UndoService`, which reads the stored command payload, looks up the affected observation, reverts its status (ACTIVE ↔ REJECTED), and writes an undo entry to the audit log. The command log entry is marked `undone = true`.

---

## Week 2 Changes

| Area | Change |
|------|--------|
| **Undo** | `UndoService` + `UndoController` — reverts any recorded command and writes audit trail |
| **Weighted strategy** | `WeightedScoringStrategy` + `ArgumentWeight` entity — threshold-based rule firing with per-argument weights |
| **Decorator pipeline** | `UnitValidationDecorator`, `AnomalyFlaggingDecorator`, `AuditStampingDecorator` wrap `BaseObservationProcessor` |
| **Hierarchy propagation** | `PropagationListener` — PRESENT/ABSENT propagates through `Phenomenon.parentConcept` self-reference |
| **Anomaly detection** | `PhenomenonType.normalMin` / `normalMax` — `AnomalyFlaggingDecorator` sets `anomalyFlag` on out-of-range measurements |
| **Multi-user** | `AppUser` entity with `UserRole` (ADMIN/CLINICIAN); `UserInterceptor` reads `X-Username` header into `CurrentUser` request-scoped bean |
| **Undo audit log** | `UndoService` directly writes `AuditLogEntry` on every undo operation |
| **Lazy payload fix** | `BaseCommand` uses `Supplier<String> payloadFn` evaluated post-save so entity id is non-null in stored JSON |
| **Per-name rule seeding** | `DataInitializer.seedRules()` uses per-name guards so new rules are seeded on any restart without duplicating existing ones |
| **Pain Level hierarchy** | `seedPainLevelIfMissing()` re-seeds Pain Level phenomena if the type exists but has no phenomena in the DB |

---

## Project Structure

``` .
src/main/java/com/patienttracker/
├── config/          AppConfig, DataInitializer, CurrentUser, UserInterceptor
├── domain/          JPA entities (Observation, Measurement, CategoryObservation,
│                    PhenomenonType, Phenomenon, Protocol, AssociativeFunction,
│                    ArgumentWeight, CommandLogEntry, AuditLogEntry, AppUser) + enums
├── resourceaccess/  Spring Data repositories
├── factory/         ObservationFactory
├── decorator/       ObservationProcessor, ObservationProcessorDecorator,
│                    BaseObservationProcessor, UnitValidationDecorator,
│                    AnomalyFlaggingDecorator, AuditStampingDecorator
├── strategy/        DiagnosisStrategy, SimpleConjunctiveStrategy,
│                    WeightedScoringStrategy, DiagnosisStrategyFactory
├── command/         Command, BaseCommand, CommandLog
├── event/           ObservationEvent, AuditLogListener,
│                    RuleEvaluationListener, PropagationListener
├── engine/          DiagnosisEngine
├── manager/         PatientManager, ObservationManager,
│                    CatalogueManager, LogManager, UndoService
└── controller/      PatientController, ObservationController,
                     CatalogueController, LogController, UndoController, UserController

src/main/resources/static/   Frontend HTML pages (index, patient, catalogue, logs)
src/test/java/com/patienttracker/   Unit tests
```

---

## Running Tests

```bash
mvn test
```

Unit tests cover: `ObservationFactory` validation, `SimpleConjunctiveStrategy` and `WeightedScoringStrategy` logic, `BaseCommand` execution, `CommandLog` persistence, and `AuditLogListener` event handling. All tests use `@ExtendWith(MockitoExtension.class)` — no `@SpringBootTest`.

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

On startup, `DataInitializer` seeds the database (count-guarded for types/protocols/users; name-guarded for rules and Pain Level phenomena):

**Quantitative phenomenon types** (9) with normal ranges and allowed units:

- Body Temperature (36.1–37.2 °C), Blood Glucose (70–140 mg/dL), Systolic BP (90–120 mmHg), Diastolic BP (60–80 mmHg), Heart Rate (60–100 bpm), Oxygen Saturation (95–100 %), Respiratory Rate (12–20 breaths/min), Body Weight, Body Height

**Qualitative phenomenon types** (5) with phenomena:

- Blood Group (A+/A−/B+/B−/AB+/AB−/O+/O−)
- Structural Condition (Excellent/Good/Fair/Poor/Critical)
- Pain Level (hierarchy: None; Any Pain → Mild, Moderate, Severe Pain → Extreme)
- Level of Consciousness (Alert/Verbal/Pain/Unresponsive)
- Mobility Status (Independent/Assisted/Dependent/Bedbound)

**Protocols** (10): Standard BP, Fasting Glucose, Oral Temperature, Pulse Oximetry, Manual Heart Rate, Random Glucose, Body Weight, Tympanic Temperature, Visual Pain Assessment, Standard Observation

**Diagnostic rules** (3):

- *Systemic Inflammatory Response* — CONJUNCTIVE: Body Temperature + Heart Rate → Systolic BP
- *Metabolic Risk* — WEIGHTED threshold 1.0: Blood Glucose (w=0.7) + Body Weight (w=0.5) → Systolic BP
- *Respiratory Compromise* — CONJUNCTIVE: Oxygen Saturation + Heart Rate → Body Temperature

**Users** (4): admin (ADMIN), alice (CLINICIAN), bob (CLINICIAN), staff (CLINICIAN)

---

## Related Repository

Frontend (GitHub Pages): [isutariy-P532-SPRING2026/patient-observation-tracker-frontend-version-2](https://github.com/isutariy-P532-SPRING2026/patient-observation-tracker-frontend-version-2)
