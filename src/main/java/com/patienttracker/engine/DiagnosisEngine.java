package com.patienttracker.engine;

import com.patienttracker.domain.AssociativeFunction;
import com.patienttracker.domain.Observation;
import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.strategy.DiagnosisStrategy;
import com.patienttracker.strategy.DiagnosisStrategyFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DiagnosisEngine {

    private final DiagnosisStrategyFactory strategyFactory;

    public DiagnosisEngine(DiagnosisStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public List<PhenomenonType> evaluate(List<AssociativeFunction> rules,
                                          List<Observation> observations) {
        return rules.stream()
            .filter(rule -> rule.isActive()
                && strategyFactory.forHint(rule.getStrategyHint())
                                  .evaluate(rule, observations))
            .map(AssociativeFunction::getProductConcept)
            .toList();
    }
}