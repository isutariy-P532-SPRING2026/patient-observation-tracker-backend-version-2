package com.patienttracker.strategy;

import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.ObservationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fires a rule when the sum of weights for present ACTIVE MANUAL observations
 * meets or exceeds the rule's threshold.
 */
@Component
public class WeightedScoringStrategy implements DiagnosisStrategy {

    @Override
    public boolean evaluate(AssociativeFunction rule, List<Observation> patientObservations) {
        Set<Long> activeManualTypeIds = patientObservations.stream()
            .filter(o -> o.getStatus() == ObservationStatus.ACTIVE
                      && o.getSource() == com.patienttracker.domain.enums.ObservationSource.MANUAL)
            .map(o -> {
                if (o instanceof Measurement m) return m.getPhenomenonType().getId();
                if (o instanceof CategoryObservation c) return c.getPhenomenon().getPhenomenonType().getId();
                return null;
            })
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        // Build a map: phenomenonTypeId -> weight from the rule's argumentWeights
        Map<Long, Double> weightMap = rule.getArgumentWeights().stream()
            .collect(Collectors.toMap(
                aw -> aw.getPhenomenonType().getId(),
                aw -> aw.getWeight()
            ));

        double score = weightMap.entrySet().stream()
            .filter(e -> activeManualTypeIds.contains(e.getKey()))
            .mapToDouble(Map.Entry::getValue)
            .sum();

        double threshold = rule.getThreshold() != null ? rule.getThreshold() : 1.0;
        return score >= threshold;
    }
}