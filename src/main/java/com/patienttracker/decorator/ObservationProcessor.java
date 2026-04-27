package com.patienttracker.decorator;

import com.patienttracker.domain.Observation;

public interface ObservationProcessor {
    Observation process(Observation observation);
}