# Patient Observation Tracker

[![CI](https://github.com/YOUR_USERNAME/patient-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/patient-tracker/actions/workflows/ci.yml)

**Live URL:** https://YOUR-APP.onrender.com

## Quick Start with Docker

```bash
docker build -t tracker .
docker run -p 8080:8080 -v $(pwd)/data:/app/data tracker
```

Then open http://localhost:8080

## Design Patterns

**Factory (`ObservationFactory`):** All `Measurement` and `CategoryObservation` objects are created exclusively through `ObservationFactory`. It validates that the phenomenon type's kind (QUANTITATIVE/QUALITATIVE) matches the observation kind, and that the unit is in the allowed set before constructing any object.

**Strategy (`DiagnosisEngine` + `SimpleConjunctiveStrategy`):** The rule-evaluation algorithm is injected into `DiagnosisEngine` as a `DiagnosisStrategy` interface. `SimpleConjunctiveStrategy` fires a rule only when all argument observation concepts are present in active observations. A new strategy (e.g. `WeightedScoringStrategy`) can be swapped in Week 2 without changing the engine.

**Observer (Spring `ApplicationEventPublisher`):** `ObservationManager` publishes an `ObservationEvent` whenever an observation is created or rejected. `AuditLogListener` appends an audit entry and `RuleEvaluationListener` re-evaluates rules — both are completely decoupled from the manager.

**Command (`BaseCommand` + `CommandLog`):** Every state-changing action (create patient, record observation, reject observation) is wrapped in a `BaseCommand` with an `execute()` method. `CommandLog.record()` executes the command then persists a `CommandLogEntry` with a JSON payload and timestamp.