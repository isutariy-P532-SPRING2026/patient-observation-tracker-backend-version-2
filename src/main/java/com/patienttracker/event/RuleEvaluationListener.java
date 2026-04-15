package com.patienttracker.event;

import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.engine.DiagnosisEngine;
import com.patienttracker.resourceaccess.AssociativeFunctionRepository;
import com.patienttracker.resourceaccess.ObservationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listener 2: re-evaluates diagnostic rules after any observation change
 * and logs inferred concepts to the console.
 */
@Component
public class RuleEvaluationListener {

    private final DiagnosisEngine diagnosisEngine;
    private final AssociativeFunctionRepository ruleRepository;
    private final ObservationRepository observationRepository;

    public RuleEvaluationListener(DiagnosisEngine diagnosisEngine,
                                   AssociativeFunctionRepository ruleRepository,
                                   ObservationRepository observationRepository) {
        this.diagnosisEngine = diagnosisEngine;
        this.ruleRepository = ruleRepository;
        this.observationRepository = observationRepository;
    }

    @EventListener
    public void onObservationEvent(ObservationEvent event) {
        Long patientId = event.getObservation().getPatient().getId();
        var observations = observationRepository.findByPatientId(patientId);
        var rules = ruleRepository.findByActiveTrue();
        List<PhenomenonType> inferences = diagnosisEngine.evaluate(rules, observations);

        if (!inferences.isEmpty()) {
            System.out.printf("[RuleEval] Patient %d — inferred: %s%n",
                patientId,
                inferences.stream().map(PhenomenonType::getName).toList());
        }
    }
}