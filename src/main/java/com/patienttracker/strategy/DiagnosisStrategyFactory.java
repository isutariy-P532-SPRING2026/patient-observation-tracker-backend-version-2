package com.patienttracker.strategy;

import org.springframework.stereotype.Component;

@Component
public class DiagnosisStrategyFactory {

    private final SimpleConjunctiveStrategy conjunctive;
    private final WeightedScoringStrategy weighted;

    public DiagnosisStrategyFactory(SimpleConjunctiveStrategy conjunctive,
                                     WeightedScoringStrategy weighted) {
        this.conjunctive = conjunctive;
        this.weighted = weighted;
    }

    public DiagnosisStrategy forHint(String hint) {
        if ("WEIGHTED".equalsIgnoreCase(hint)) return weighted;
        return conjunctive; // default
    }
}