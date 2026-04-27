package com.patienttracker.strategy;

import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.ObservationStatus;
import com.patienttracker.domain.enums.ObservationSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fires a rule only when ALL argument phenomenon-types are present
 * in the patient's ACTIVE observations (conjunctive = AND logic).
 */
@Component
public class SimpleConjunctiveStrategy implements DiagnosisStrategy {

    @Override
    public boolean evaluate(AssociativeFunction rule, List<Observation> patientObservations) {
        // Collect phenomenon-type IDs present in ACTIVE observations
        Set<Long> activeTypeIds = patientObservations.stream()
            .filter(o -> o.getStatus() == ObservationStatus.ACTIVE
                    && o.getSource() == ObservationSource.MANUAL)
            .map(o -> {
                if (o instanceof Measurement m) return m.getPhenomenonType().getId();
                if (o instanceof CategoryObservation c) return c.getPhenomenon().getPhenomenonType().getId();
                return null;
            })
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        // Rule fires only if every argument concept is present
        return rule.getArgumentConcepts().stream()
            .allMatch(pt -> activeTypeIds.contains(pt.getId()));
    }
}