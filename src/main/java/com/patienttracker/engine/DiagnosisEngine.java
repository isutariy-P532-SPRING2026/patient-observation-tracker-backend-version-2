package com.patienttracker.engine;

import com.patienttracker.domain.AssociativeFunction;
import com.patienttracker.domain.Observation;
import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.strategy.DiagnosisStrategy;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DiagnosisEngine {

    private final DiagnosisStrategy strategy;

    public DiagnosisEngine(DiagnosisStrategy strategy) {
        this.strategy = strategy;
    }

    public List<PhenomenonType> evaluate(List<AssociativeFunction> rules,
                                          List<Observation> observations) {
        return rules.stream()
            .filter(rule -> rule.isActive() && strategy.evaluate(rule, observations))
            .map(AssociativeFunction::getProductConcept)
            .toList();
    }
}