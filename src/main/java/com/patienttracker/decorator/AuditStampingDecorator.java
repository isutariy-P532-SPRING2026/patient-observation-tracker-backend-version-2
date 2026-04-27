package com.patienttracker.decorator;

import com.patienttracker.domain.Observation;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Ensures recordingTime is set (stamps now if missing).
 * Acts as a safety net — ObservationFactory already sets it,
 * but the decorator makes the pipeline self-sufficient.
 */
public class AuditStampingDecorator extends ObservationProcessorDecorator {

    private final Clock clock;

    public AuditStampingDecorator(ObservationProcessor delegate, Clock clock) {
        super(delegate);
        this.clock = clock;
    }

    @Override
    public Observation process(Observation observation) {
        if (observation.getRecordingTime() == null) {
            observation.setRecordingTime(LocalDateTime.now(clock));
        }
        return delegate.process(observation);
    }
}