package com.patienttracker.decorator;

import com.patienttracker.domain.Observation;

public abstract class ObservationProcessorDecorator implements ObservationProcessor {

    protected final ObservationProcessor delegate;

    protected ObservationProcessorDecorator(ObservationProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Observation process(Observation observation) {
        return delegate.process(observation);
    }
}