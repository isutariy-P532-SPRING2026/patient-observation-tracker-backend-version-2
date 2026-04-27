package com.patienttracker.manager;

import com.patienttracker.decorator.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patienttracker.command.BaseCommand;
import com.patienttracker.command.CommandLog;
import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.ObservationStatus;
import com.patienttracker.domain.enums.Presence;
import com.patienttracker.engine.DiagnosisEngine;
import com.patienttracker.event.ObservationEvent;
import com.patienttracker.factory.ObservationFactory;
import com.patienttracker.resourceaccess.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Clock;

@Service
public class ObservationManager {

    private final ObservationRepository observationRepository;
    private final PatientRepository patientRepository;
    private final PhenomenonTypeRepository phenomenonTypeRepository;
    private final PhenomenonRepository phenomenonRepository;
    private final ProtocolRepository protocolRepository;
    private final AssociativeFunctionRepository ruleRepository;
    private final ObservationFactory factory;
    private final DiagnosisEngine diagnosisEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final CommandLog commandLog;
    private final ObjectMapper objectMapper;
    private final BaseObservationProcessor baseProcessor;
    private final Clock clock;

    public ObservationManager(ObservationRepository observationRepository,
                               PatientRepository patientRepository,
                               PhenomenonTypeRepository phenomenonTypeRepository,
                               PhenomenonRepository phenomenonRepository,
                               ProtocolRepository protocolRepository,
                               AssociativeFunctionRepository ruleRepository,
                               ObservationFactory factory,
                               DiagnosisEngine diagnosisEngine,
                               ApplicationEventPublisher eventPublisher,
                               CommandLog commandLog,
                               ObjectMapper objectMapper,
                               BaseObservationProcessor baseProcessor,
                               Clock clock) {
        this.observationRepository = observationRepository;
        this.patientRepository = patientRepository;
        this.phenomenonTypeRepository = phenomenonTypeRepository;
        this.phenomenonRepository = phenomenonRepository;
        this.protocolRepository = protocolRepository;
        this.ruleRepository = ruleRepository;
        this.factory = factory;
        this.diagnosisEngine = diagnosisEngine;
        this.eventPublisher = eventPublisher;
        this.commandLog = commandLog;
        this.objectMapper = objectMapper;
        this.baseProcessor = baseProcessor;
        this.clock = clock;
    }

    public Observation recordMeasurement(Long patientId, Long phenomenonTypeId,
                                          Double amount, String unit,
                                          LocalDateTime applicabilityTime, Long protocolId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        PhenomenonType pt = phenomenonTypeRepository.findById(phenomenonTypeId).orElseThrow();
        Protocol protocol = protocolId != null
            ? protocolRepository.findById(protocolId).orElse(null) : null;

        Measurement m = factory.createMeasurement(patient, pt, amount, unit, applicabilityTime, protocol);
        buildPipeline().process(m);

        Observation[] result = {null};
        // Use Supplier<String> so payload is serialized AFTER save (id is assigned by then)
        commandLog.record(new BaseCommand(
            "RECORD_MEASUREMENT",
            () -> toJson(result[0]),
            () -> result[0] = observationRepository.save(m),
            () -> {}
        ));
        eventPublisher.publishEvent(new ObservationEvent(result[0], "CREATED"));
        return result[0];
    }

    public Observation recordCategoryObservation(Long patientId, Long phenomenonId,
                                                  Presence presence,
                                                  LocalDateTime applicabilityTime, Long protocolId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        Phenomenon phenomenon = phenomenonRepository.findById(phenomenonId).orElseThrow();
        Protocol protocol = protocolId != null
            ? protocolRepository.findById(protocolId).orElse(null) : null;

        CategoryObservation co = factory.createCategoryObservation(
            patient, phenomenon, presence, applicabilityTime, protocol);
        buildPipeline().process(co);

        Observation[] result = {null};
        // Use Supplier<String> so payload is serialized AFTER save (id is assigned by then)
        commandLog.record(new BaseCommand(
            "RECORD_CATEGORY_OBSERVATION",
            () -> toJson(result[0]),
            () -> result[0] = observationRepository.save(co),
            () -> {}
        ));
        eventPublisher.publishEvent(new ObservationEvent(result[0], "CREATED"));
        return result[0];
    }

    public Observation rejectObservation(Long observationId, String reason) {
        Observation obs = observationRepository.findById(observationId).orElseThrow();

        Observation[] result = {null};
        commandLog.record(new BaseCommand(
            "REJECT_OBSERVATION",
            "{\"id\":" + observationId + ",\"reason\":\"" + reason + "\"}",
            () -> {
                obs.setStatus(ObservationStatus.REJECTED);
                obs.setRejectionReason(reason);
                result[0] = observationRepository.save(obs);
            }
        ));
        eventPublisher.publishEvent(new ObservationEvent(result[0], "REJECTED"));
        return result[0];
    }

    public List<Observation> getObservationsForPatient(Long patientId) {
        return observationRepository.findByPatientIdOrderByRecordingTimeDesc(patientId);
    }

    public List<PhenomenonType> evaluateRules(Long patientId) {
        var observations = observationRepository.findByPatientId(patientId);
        var rules = ruleRepository.findByActiveTrue();
        return diagnosisEngine.evaluate(rules, observations);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private ObservationProcessor buildPipeline() {
        return new AuditStampingDecorator(
                new AnomalyFlaggingDecorator(
                    new UnitValidationDecorator(
                        baseProcessor)),
                clock);
    }
}