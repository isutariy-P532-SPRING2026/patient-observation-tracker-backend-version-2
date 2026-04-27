package com.patienttracker.decorator;

import com.patienttracker.domain.Observation;
import org.springframework.stereotype.Component;


@Component
public class BaseObservationProcessor implements ObservationProcessor {

    @Override
    public Observation process(Observation observation) {
        return observation;
    }
}