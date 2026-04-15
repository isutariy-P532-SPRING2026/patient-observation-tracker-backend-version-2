package com.patienttracker.strategy;

import com.patienttracker.domain.AssociativeFunction;
import com.patienttracker.domain.Observation;

import java.util.List;

public interface DiagnosisStrategy {
    boolean evaluate(AssociativeFunction rule, List<Observation> patientObservations);
}