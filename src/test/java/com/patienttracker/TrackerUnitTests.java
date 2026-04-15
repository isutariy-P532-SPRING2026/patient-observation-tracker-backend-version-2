package com.patienttracker;

import com.patienttracker.command.BaseCommand;
import com.patienttracker.command.CommandLog;
import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.MeasurementKind;
import com.patienttracker.domain.enums.ObservationStatus;
import com.patienttracker.domain.enums.Presence;
import com.patienttracker.engine.DiagnosisEngine;
import com.patienttracker.event.AuditLogListener;
import com.patienttracker.event.ObservationEvent;
import com.patienttracker.factory.ObservationFactory;
import com.patienttracker.resourceaccess.AuditLogRepository;
import com.patienttracker.resourceaccess.CommandLogRepository;
import com.patienttracker.strategy.SimpleConjunctiveStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackerUnitTests {

    // Fixed clock for deterministic timestamps
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC"));

    // =========================================================
    // ObservationFactory tests
    // =========================================================

    @Test
    void createMeasurement_validInput_returnsMeasurement() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        Patient patient = new Patient();
        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(Set.of("°C", "°F"));

        // Act
        Measurement m = factory.createMeasurement(patient, pt, 37.5, "°C", null, null);

        // Assert
        assertEquals(37.5, m.getAmount());
        assertEquals("°C", m.getUnit());
        assertEquals(ObservationStatus.ACTIVE, m.getStatus());
        assertNotNull(m.getRecordingTime());
    }

    @Test
    void createMeasurement_wrongKind_throwsException() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        Patient patient = new Patient();
        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUALITATIVE);
        pt.setAllowedUnits(Set.of());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> factory.createMeasurement(patient, pt, 1.0, "unit", null, null));
    }

    @Test
    void createMeasurement_invalidUnit_throwsException() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        Patient patient = new Patient();
        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(Set.of("kg"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> factory.createMeasurement(patient, pt, 70.0, "lbs", null, null));
    }

    @Test
    void createMeasurement_applicabilityTimeDefaults_toNow() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        Patient patient = new Patient();
        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(Set.of("kg"));

        // Act
        Measurement m = factory.createMeasurement(patient, pt, 70.0, "kg", null, null);

        // Assert — applicability time defaults to fixedClock's now
        assertEquals(LocalDateTime.now(fixedClock), m.getApplicabilityTime());
    }

    @Test
    void createCategoryObservation_validInput_returnsCategoryObs() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        Patient patient = new Patient();
        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUALITATIVE);
        Phenomenon phenomenon = new Phenomenon();
        phenomenon.setPhenomenonType(pt);

        // Act
        CategoryObservation co = factory.createCategoryObservation(patient, phenomenon, Presence.PRESENT, null, null);

        // Assert
        assertEquals(Presence.PRESENT, co.getPresence());
        assertEquals(ObservationStatus.ACTIVE, co.getStatus());
    }

    @Test
    void createCategoryObservation_wrongKind_throwsException() {
        // Arrange
        ObservationFactory factory = new ObservationFactory(fixedClock);
        Patient patient = new Patient();
        PhenomenonType pt = new PhenomenonType();
        pt.setKind(MeasurementKind.QUANTITATIVE);  // wrong kind
        Phenomenon phenomenon = new Phenomenon();
        phenomenon.setPhenomenonType(pt);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> factory.createCategoryObservation(patient, phenomenon, Presence.PRESENT, null, null));
    }

    // =========================================================
    // SimpleConjunctiveStrategy tests
    // =========================================================

    @Test
    void simpleConjunctive_allPhenomenonTypesPresent_returnsTrue() {
        // Arrange
        SimpleConjunctiveStrategy strategy = new SimpleConjunctiveStrategy();

        PhenomenonType ptA = new PhenomenonType(); ptA.setId(1L); ptA.setKind(MeasurementKind.QUANTITATIVE); ptA.setAllowedUnits(Set.of("u"));
        PhenomenonType ptB = new PhenomenonType(); ptB.setId(2L); ptB.setKind(MeasurementKind.QUANTITATIVE); ptB.setAllowedUnits(Set.of("u"));

        Measurement obsA = new Measurement(); obsA.setPhenomenonType(ptA); obsA.setStatus(ObservationStatus.ACTIVE); obsA.setUnit("u"); obsA.setAmount(1.0);
        Measurement obsB = new Measurement(); obsB.setPhenomenonType(ptB); obsB.setStatus(ObservationStatus.ACTIVE); obsB.setUnit("u"); obsB.setAmount(1.0);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentConcepts(Set.of(ptA, ptB));

        // Act
        boolean result = strategy.evaluate(rule, List.of(obsA, obsB));

        // Assert
        assertTrue(result);
    }

    @Test
    void simpleConjunctive_missingOnePhenomenonType_returnsFalse() {
        // Arrange
        SimpleConjunctiveStrategy strategy = new SimpleConjunctiveStrategy();

        PhenomenonType ptA = new PhenomenonType(); ptA.setId(1L); ptA.setKind(MeasurementKind.QUANTITATIVE); ptA.setAllowedUnits(Set.of("u"));
        PhenomenonType ptB = new PhenomenonType(); ptB.setId(2L); ptB.setKind(MeasurementKind.QUANTITATIVE); ptB.setAllowedUnits(Set.of("u"));

        Measurement obsA = new Measurement(); obsA.setPhenomenonType(ptA); obsA.setStatus(ObservationStatus.ACTIVE); obsA.setUnit("u"); obsA.setAmount(1.0);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentConcepts(Set.of(ptA, ptB));

        // Act
        boolean result = strategy.evaluate(rule, List.of(obsA));

        // Assert
        assertFalse(result);
    }

    @Test
    void simpleConjunctive_rejectedObservationsIgnored_returnsFalse() {
        // Arrange
        SimpleConjunctiveStrategy strategy = new SimpleConjunctiveStrategy();

        PhenomenonType pt = new PhenomenonType(); pt.setId(1L); pt.setKind(MeasurementKind.QUANTITATIVE); pt.setAllowedUnits(Set.of("u"));
        Measurement obs = new Measurement(); obs.setPhenomenonType(pt); obs.setStatus(ObservationStatus.REJECTED); obs.setUnit("u"); obs.setAmount(1.0);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentConcepts(Set.of(pt));

        // Act
        boolean result = strategy.evaluate(rule, List.of(obs));

        // Assert — REJECTED observation does not count
        assertFalse(result);
    }

    // =========================================================
    // Command + CommandLog tests
    // =========================================================

    @Mock
    private CommandLogRepository commandLogRepository;

    @Test
    void baseCommand_execute_runsAction() {
        // Arrange
        boolean[] ran = {false};
        BaseCommand cmd = new BaseCommand("TEST", "{}", () -> ran[0] = true);

        // Act
        cmd.execute();

        // Assert
        assertTrue(ran[0]);
    }

    @Test
    void commandLog_record_savesEntry() {
        // Arrange
        CommandLog commandLog = new CommandLog(commandLogRepository);
        BaseCommand cmd = new BaseCommand("CREATE_PATIENT", "{\"name\":\"Alice\"}", () -> {});

        // Act
        commandLog.record(cmd);

        // Assert
        ArgumentCaptor<com.patienttracker.domain.CommandLogEntry> captor =
            ArgumentCaptor.forClass(com.patienttracker.domain.CommandLogEntry.class);
        verify(commandLogRepository).save(captor.capture());
        assertEquals("CREATE_PATIENT", captor.getValue().getCommandType());
        assertEquals("staff", captor.getValue().getUser());
    }

    @Test
    void commandLog_record_executesCommandBeforeSaving() {
        // Arrange
        CommandLog commandLog = new CommandLog(commandLogRepository);
        boolean[] executed = {false};
        BaseCommand cmd = new BaseCommand("OP", "{}", () -> executed[0] = true);

        // Act
        commandLog.record(cmd);

        // Assert
        assertTrue(executed[0]);
        verify(commandLogRepository).save(any());
    }

    // =========================================================
    // Observer listener tests
    // =========================================================

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void auditLogListener_onCreatedEvent_savesEntry() {
        // Arrange
        AuditLogListener listener = new AuditLogListener(auditLogRepository);
        Patient patient = new Patient(); patient.setId(10L);
        Measurement obs = new Measurement(); obs.setId(5L); obs.setPatient(patient);
        obs.setStatus(ObservationStatus.ACTIVE);
        ObservationEvent event = new ObservationEvent(obs, "CREATED");

        // Act
        listener.onObservationEvent(event);

        // Assert
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getEvent().contains("CREATED"));
        assertEquals(5L, captor.getValue().getObservationId());
        assertEquals(10L, captor.getValue().getPatientId());
    }

    @Test
    void auditLogListener_onRejectedEvent_savesEntryWithRejectedEvent() {
        // Arrange
        AuditLogListener listener = new AuditLogListener(auditLogRepository);
        Patient patient = new Patient(); patient.setId(1L);
        Measurement obs = new Measurement(); obs.setId(2L); obs.setPatient(patient);
        obs.setStatus(ObservationStatus.REJECTED);
        ObservationEvent event = new ObservationEvent(obs, "REJECTED");

        // Act
        listener.onObservationEvent(event);

        // Assert
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getEvent().contains("REJECTED"));
    }

    // =========================================================
    // DiagnosisEngine test
    // =========================================================

    @Test
    void diagnosisEngine_ruleMatches_returnsProductConcept() {
        // Arrange
        SimpleConjunctiveStrategy strategy = new SimpleConjunctiveStrategy();
        DiagnosisEngine engine = new DiagnosisEngine(strategy);

        PhenomenonType ptA = new PhenomenonType(); ptA.setId(1L); ptA.setKind(MeasurementKind.QUANTITATIVE); ptA.setAllowedUnits(Set.of("u"));
        PhenomenonType product = new PhenomenonType(); product.setId(99L); product.setName("Diabetes Risk");

        Measurement obs = new Measurement(); obs.setPhenomenonType(ptA); obs.setStatus(ObservationStatus.ACTIVE); obs.setUnit("u"); obs.setAmount(1.0);

        AssociativeFunction rule = new AssociativeFunction();
        rule.setArgumentConcepts(Set.of(ptA));
        rule.setProductConcept(product);
        rule.setActive(true);

        // Act
        List<PhenomenonType> results = engine.evaluate(List.of(rule), List.of(obs));

        // Assert
        assertEquals(1, results.size());
        assertEquals("Diabetes Risk", results.get(0).getName());
    }
}