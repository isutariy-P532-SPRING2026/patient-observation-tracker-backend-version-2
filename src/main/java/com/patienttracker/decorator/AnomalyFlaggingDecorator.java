package com.patienttracker.decorator;

import com.patienttracker.domain.Measurement;
import com.patienttracker.domain.Observation;

/**
 * Sets anomalyFlag = true when a Measurement's amount falls outside
 * the phenomenonType's [normalMin, normalMax] range (if defined).
 */
public class AnomalyFlaggingDecorator extends ObservationProcessorDecorator {

    public AnomalyFlaggingDecorator(ObservationProcessor delegate) {
        super(delegate);
    }

    @Override
    public Observation process(Observation observation) {
        if (observation instanceof Measurement m) {
            Double min = m.getPhenomenonType().getNormalMin();
            Double max = m.getPhenomenonType().getNormalMax();
            if (min != null && max != null && m.getAmount() != null) {
                if (m.getAmount() < min || m.getAmount() > max) {
                    m.setAnomalyFlag(true);
                }
            }
        }
        return delegate.process(observation);
    }
}