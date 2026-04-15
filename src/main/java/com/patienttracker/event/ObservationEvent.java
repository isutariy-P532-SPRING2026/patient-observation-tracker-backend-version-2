package com.patienttracker.event;

import com.patienttracker.domain.Observation;

/**
 * domain event published whenever an observation is
 * created or rejected.  Listeners are decoupled via Spring's event bus.
 */
public class ObservationEvent {

    private final Observation observation;
    private final String eventType;   // "CREATED" or "REJECTED"

    public ObservationEvent(Observation observation, String eventType) {
        this.observation = observation;
        this.eventType = eventType;
    }

    public Observation getObservation() { return observation; }
    public String getEventType()        { return eventType; }
}