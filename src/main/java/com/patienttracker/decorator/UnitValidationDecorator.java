package com.patienttracker.decorator;

import com.patienttracker.domain.Measurement;
import com.patienttracker.domain.Observation;

/**
 * Validates that a Measurement's unit belongs to the phenomenonType's allowed set.
 * CategoryObservations pass through unchanged (unit constraint is n/a).
 * Note: ObservationFactory already guards this at construction time;
 * this decorator re-checks so the pipeline is self-contained and
 * catches any observation that bypasses the factory in future.
 */
public class UnitValidationDecorator extends ObservationProcessorDecorator {

    public UnitValidationDecorator(ObservationProcessor delegate) {
        super(delegate);
    }

    @Override
    public Observation process(Observation observation) {
        if (observation instanceof Measurement m) {
            String unit = m.getUnit();
            var allowed = m.getPhenomenonType().getAllowedUnits();
            if (allowed != null && !allowed.isEmpty() && !allowed.contains(unit)) {
                throw new IllegalArgumentException(
                    "Unit '" + unit + "' not allowed for '" +
                    m.getPhenomenonType().getName() + "'");
            }
        }
        return delegate.process(observation);
    }
}