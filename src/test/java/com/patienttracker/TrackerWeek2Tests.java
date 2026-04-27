package com.patienttracker;

import com.patienttracker.command.BaseCommand;
import com.patienttracker.command.CommandLog;
import com.patienttracker.decorator.*;
import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.*;
import com.patienttracker.engine.DiagnosisEngine;
import com.patienttracker.event.ObservationEvent;
import com.patienttracker.event.PropagationListener;
import com.patienttracker.factory.ObservationFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patienttracker.manager.UndoService;
import com.patienttracker.resourceaccess.*;
import com.patienttracker.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackerWeek2Tests {

    private final Clock fixedClock =
        Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC"));

    // =========================================================
    // Test 1: WeightedScoringStrategy — score meets threshold → true
    // =========================================================

    @Test
    void weightedScoring_scoresMeetThreshold_returnsTrue() {
        // Arrange
        WeightedScoringStrategy strategy = new WeightedScoringStrategy();

        PhenomenonType ptA = quantType(1L);
        PhenomenonType ptB = quantType(2L);

        ArgumentWeight wA = weight(ptA, 0.6);
        ArgumentWeight wB = weight(ptB, 0.5);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentWeights(List.of(wA, wB));
        rule.setThreshold(1.0);
        rule.setArgumentConcepts(Set.of(ptA, ptB));

        Measurement obsA = activeMeasurement(ptA);
        Measurement obsB = activeMeasurement(ptB);

        // Act
        boolean result = strategy.evaluate(rule, List.of(obsA, obsB));

        // Assert
        assertTrue(result);
    }

    // =========================================================
    // Test 2: WeightedScoringStrategy — score below threshold → false
    // =========================================================

    @Test
    void weightedScoring_scoreBelowThreshold_returnsFalse() {
        // Arrange
        WeightedScoringStrategy strategy = new WeightedScoringStrategy();

        PhenomenonType ptA = quantType(1L);

        ArgumentWeight wA = weight(ptA, 0.4);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentWeights(List.of(wA));
        rule.setThreshold(1.0);
        rule.setArgumentConcepts(Set.of(ptA));

        Measurement obsA = activeMeasurement(ptA);

        // Act
        boolean result = strategy.evaluate(rule, List.of(obsA));

        // Assert
        assertFalse(result);
    }

    // =========================================================
    // Test 3: WeightedScoringStrategy — INFERRED observations excluded
    // =========================================================

    @Test
    void weightedScoring_inferredObservationsIgnored_returnsFalse() {
        // Arrange
        WeightedScoringStrategy strategy = new WeightedScoringStrategy();

        PhenomenonType pt = quantType(1L);
        ArgumentWeight w = weight(pt, 2.0);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentWeights(List.of(w));
        rule.setThreshold(1.0);
        rule.setArgumentConcepts(Set.of(pt));

        Measurement obs = activeMeasurement(pt);
        obs.setSource(ObservationSource.INFERRED);  // inferred — must be excluded

        // Act
        boolean result = strategy.evaluate(rule, List.of(obs));

        // Assert
        assertFalse(result);
    }

    // =========================================================
    // Test 4: DiagnosisEngine — routes WEIGHTED hint to WeightedScoringStrategy
    // =========================================================

    @Test
    void diagnosisEngine_weightedHint_usesWeightedStrategy() {
        // Arrange
        PhenomenonType ptA = quantType(1L);
        PhenomenonType product = quantType(99L);
        product.setName("Risk Concept");

        ArgumentWeight w = weight(ptA, 1.0);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setStrategyHint("WEIGHTED");
        rule.setThreshold(1.0);
        rule.setArgumentWeights(List.of(w));
        rule.setArgumentConcepts(Set.of(ptA));
        rule.setProductConcept(product);
        rule.setActive(true);

        Measurement obs = activeMeasurement(ptA);

        DiagnosisStrategyFactory factory =
            new DiagnosisStrategyFactory(
                new SimpleConjunctiveStrategy(),
                new WeightedScoringStrategy());
        DiagnosisEngine engine = new DiagnosisEngine(factory);

        // Act
        List<PhenomenonType> results = engine.evaluate(List.of(rule), List.of(obs));

        // Assert
        assertEquals(1, results.size());
        assertEquals("Risk Concept", results.get(0).getName());
    }

    // =========================================================
    // Test 5: UnitValidationDecorator — invalid unit throws
    // =========================================================

    @Test
    void unitValidationDecorator_invalidUnit_throwsException() {
        // Arrange
        ObservationProcessor base = new BaseObservationProcessor();
        UnitValidationDecorator decorator = new UnitValidationDecorator(base);

        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(Set.of("kg"));

        Measurement m = new Measurement();
        m.setPhenomenonType(pt);
        m.setUnit("lbs");  // not in allowed set
        m.setStatus(ObservationStatus.ACTIVE);
        m.setSource(ObservationSource.MANUAL);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> decorator.process(m));
    }

    // =========================================================
    // Test 6: AnomalyFlaggingDecorator — out-of-range sets flag
    // =========================================================

    @Test
    void anomalyFlaggingDecorator_outOfRange_setsFlagTrue() {
        // Arrange
        ObservationProcessor base = new BaseObservationProcessor();
        AnomalyFlaggingDecorator decorator = new AnomalyFlaggingDecorator(base);

        PhenomenonType pt = new PhenomenonType();
        pt.setNormalMin(36.0);
        pt.setNormalMax(37.5);

        Measurement m = new Measurement();
        m.setPhenomenonType(pt);
        m.setAmount(39.0);   // fever — out of range
        m.setStatus(ObservationStatus.ACTIVE);
        m.setSource(ObservationSource.MANUAL);

        // Act
        decorator.process(m);

        // Assert
        assertTrue(m.isAnomalyFlag());
    }

    // =========================================================
    // Test 7: AnomalyFlaggingDecorator — in-range leaves flag false
    // =========================================================

    @Test
    void anomalyFlaggingDecorator_inRange_flagRemainsFalse() {
        // Arrange
        ObservationProcessor base = new BaseObservationProcessor();
        AnomalyFlaggingDecorator decorator = new AnomalyFlaggingDecorator(base);

        PhenomenonType pt = new PhenomenonType();
        pt.setNormalMin(36.0);
        pt.setNormalMax(37.5);

        Measurement m = new Measurement();
        m.setPhenomenonType(pt);
        m.setAmount(37.0);   // normal
        m.setStatus(ObservationStatus.ACTIVE);
        m.setSource(ObservationSource.MANUAL);

        // Act
        decorator.process(m);

        // Assert
        assertFalse(m.isAnomalyFlag());
    }

    // =========================================================
    // Test 8: AuditStampingDecorator — null recordingTime gets stamped
    // =========================================================

    @Test
    void auditStampingDecorator_nullRecordingTime_stampsNow() {
        // Arrange
        ObservationProcessor base = new BaseObservationProcessor();
        AuditStampingDecorator decorator = new AuditStampingDecorator(base, fixedClock);

        Measurement m = new Measurement();
        m.setRecordingTime(null);
        m.setStatus(ObservationStatus.ACTIVE);
        m.setSource(ObservationSource.MANUAL);

        // Act
        decorator.process(m);

        // Assert
        assertNotNull(m.getRecordingTime());
    }

    // =========================================================
    // Test 9: BaseCommand — lazy payload resolved after execute()
    // =========================================================

    @Test
    void baseCommand_lazyPayload_resolvedAfterExecute() {
        // Arrange
        String[] capturedId = {"not-yet"};
        BaseCommand cmd = new BaseCommand(
            "RECORD_MEASUREMENT",
            () -> "{\"id\":" + capturedId[0] + "}",   // lazy supplier
            () -> capturedId[0] = "42",                // execute sets the id
            () -> {}
        );

        // Act
        cmd.execute();

        // Assert — payload contains the post-execute value
        assertEquals("{\"id\":42}", cmd.getPayload());
    }

    // =========================================================
    // Test 10: BaseCommand — undo() runs undoAction
    // =========================================================

    @Test
    void baseCommand_undo_runsUndoAction() {
        // Arrange
        boolean[] undone = {false};
        BaseCommand cmd = new BaseCommand(
            "RECORD_MEASUREMENT",
            () -> "{}",
            () -> {},
            () -> undone[0] = true   // undo action
        );

        // Act
        cmd.undo();

        // Assert
        assertTrue(undone[0]);
    }

    // =========================================================
    // Test 11: UndoService — already undone throws IllegalStateException
    // =========================================================

    @Mock
    private CommandLogRepository commandLogRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void undoService_alreadyUndone_throwsIllegalState() {
        // Arrange
        UndoService undoService =
            new UndoService(commandLogRepository, observationRepository, auditLogRepository, objectMapper);

        CommandLogEntry entry = new CommandLogEntry();
        entry.setUser("alice");
        entry.setCommandType("RECORD_MEASUREMENT");
        entry.setPayload("{\"id\":1}");
        entry.setUndone(true);   // already undone

        when(commandLogRepository.findById(1L))
            .thenReturn(java.util.Optional.of(entry));

        // Act & Assert
        assertThrows(IllegalStateException.class,
            () -> undoService.undo(1L, "alice"));
    }

    // =========================================================
    // Test 12: UndoService — wrong user throws SecurityException
    // =========================================================

    @Test
    void undoService_wrongUser_throwsSecurityException() {
        // Arrange
        UndoService undoService =
            new UndoService(commandLogRepository, observationRepository, auditLogRepository, objectMapper);

        CommandLogEntry entry = new CommandLogEntry();
        entry.setUser("alice");
        entry.setCommandType("RECORD_MEASUREMENT");
        entry.setPayload("{\"id\":1}");
        entry.setUndone(false);

        when(commandLogRepository.findById(1L))
            .thenReturn(java.util.Optional.of(entry));

        // Act & Assert — bob cannot undo alice's command
        assertThrows(SecurityException.class,
            () -> undoService.undo(1L, "bob"));
    }

    // =========================================================
    // Test 13: PropagationListener — PRESENT creates inferred ancestor
    // =========================================================

    @Mock
    private PhenomenonRepository phenomenonRepository;

    @Test
    void propagationListener_presentObservation_createsInferredAncestor() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);

        PropagationListener listener = new PropagationListener(
            observationRepository, phenomenonRepository, factory);

        Patient patient = new Patient();
        patient.setId(1L);

        // parent ← child hierarchy
        PhenomenonType qualType = new PhenomenonType();
        qualType.setId(10L);
        qualType.setKind(MeasurementKind.QUALITATIVE);

        Phenomenon parent = new Phenomenon();
        parent.setId(100L);
        parent.setName("Symptom");
        parent.setPhenomenonType(qualType);
        parent.setParentConcept(null);   // root

        Phenomenon child = new Phenomenon();
        child.setId(101L);
        child.setName("Fever");
        child.setPhenomenonType(qualType);
        child.setParentConcept(parent);  // child → parent

        CategoryObservation co = new CategoryObservation();
        co.setId(1L);
        co.setPatient(patient);
        co.setPhenomenon(child);
        co.setPresence(Presence.PRESENT);
        co.setStatus(ObservationStatus.ACTIVE);
        co.setSource(ObservationSource.MANUAL);

        when(observationRepository.findByPatientId(1L))
            .thenReturn(List.of(co));

        // Act
        listener.onObservationEvent(new ObservationEvent(co, "CREATED"));

        // Assert — inferred PRESENT saved for parent
        ArgumentCaptor<CategoryObservation> captor =
            ArgumentCaptor.forClass(CategoryObservation.class);
        verify(observationRepository, atLeastOnce()).save(captor.capture());

        CategoryObservation inferred = captor.getAllValues().stream()
            .filter(o -> o.getSource() == ObservationSource.INFERRED)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No inferred observation saved"));

        assertEquals(Presence.PRESENT, inferred.getPresence());
        assertEquals(ObservationSource.INFERRED, inferred.getSource());
        assertEquals(parent, inferred.getPhenomenon());
    }

    // =========================================================
    // Test 14: PropagationListener — INFERRED observation not re-propagated
    // =========================================================

    @Test
    void propagationListener_inferredObservation_doesNotPropagate() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        PropagationListener listener = new PropagationListener(
            observationRepository, phenomenonRepository, factory);

        Patient patient = new Patient();
        patient.setId(1L);

        PhenomenonType qualType = new PhenomenonType();
        qualType.setId(10L);
        qualType.setKind(MeasurementKind.QUALITATIVE);

        Phenomenon ph = new Phenomenon();
        ph.setId(100L);
        ph.setPhenomenonType(qualType);
        ph.setParentConcept(null);

        CategoryObservation inferred = new CategoryObservation();
        inferred.setPatient(patient);
        inferred.setPhenomenon(ph);
        inferred.setPresence(Presence.PRESENT);
        inferred.setStatus(ObservationStatus.ACTIVE);
        inferred.setSource(ObservationSource.INFERRED);  // already inferred

        // Act
        listener.onObservationEvent(new ObservationEvent(inferred, "CREATED"));

        // Assert — no further saves triggered
        verify(observationRepository, never()).save(any());
    }

    // =========================================================
    // Helper factory methods
    // =========================================================

    private PhenomenonType quantType(Long id) {
        PhenomenonType pt = new PhenomenonType();
        pt.setId(id);
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(Set.of("u"));
        return pt;
    }

    private Measurement activeMeasurement(PhenomenonType pt) {
        Measurement m = new Measurement();
        m.setPhenomenonType(pt);
        m.setUnit("u");
        m.setAmount(1.0);
        m.setStatus(ObservationStatus.ACTIVE);
        m.setSource(ObservationSource.MANUAL);
        return m;
    }

    private ArgumentWeight weight(PhenomenonType pt, double w) {
        ArgumentWeight aw = new ArgumentWeight();
        aw.setPhenomenonType(pt);
        aw.setWeight(w);
        return aw;
    }
}