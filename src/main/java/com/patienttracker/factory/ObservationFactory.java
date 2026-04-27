package com.patienttracker.factory;

import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.MeasurementKind;
import com.patienttracker.domain.enums.ObservationStatus;
import com.patienttracker.domain.enums.ObservationSource;
import com.patienttracker.domain.enums.Presence;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Validates phenomenon-type compatibility and unit membership before constructing.
 */
@Component
public class ObservationFactory {

    private final Clock clock;

    public ObservationFactory(Clock clock) {
        this.clock = clock;
    }

    public Measurement createMeasurement(Patient patient,
                                        PhenomenonType phenomenonType,
                                        Double amount,
                                        String unit,
                                        LocalDateTime applicabilityTime,
                                        Protocol protocol,
                                        ObservationSource source) {
        if (phenomenonType.getKind() != MeasurementKind.QUANTITATIVE) {
            throw new IllegalArgumentException(
                "PhenomenonType '" + phenomenonType.getName() + "' is not QUANTITATIVE");
        }
        if (!phenomenonType.getAllowedUnits().contains(unit)) {
            throw new IllegalArgumentException(
                "Unit '" + unit + "' is not allowed for '" + phenomenonType.getName() + "'");
        }

        Measurement m = new Measurement();
        m.setPatient(patient);
        m.setPhenomenonType(phenomenonType);
        m.setAmount(amount);
        m.setUnit(unit);
        m.setRecordingTime(LocalDateTime.now(clock));
        m.setApplicabilityTime(applicabilityTime != null ? applicabilityTime : LocalDateTime.now(clock));
        m.setProtocol(protocol);
        m.setStatus(ObservationStatus.ACTIVE);
        m.setSource(source);
        return m;
    }

    /** Convenience overload — defaults to MANUAL. Existing callers unchanged. */
    public Measurement createMeasurement(Patient patient, PhenomenonType phenomenonType,
                                        Double amount, String unit,
                                        LocalDateTime applicabilityTime, Protocol protocol) {
        return createMeasurement(patient, phenomenonType, amount, unit,
                                applicabilityTime, protocol, ObservationSource.MANUAL);
    }

    public CategoryObservation createCategoryObservation(Patient patient,
                                                        Phenomenon phenomenon,
                                                        Presence presence,
                                                        LocalDateTime applicabilityTime,
                                                        Protocol protocol,
                                                        ObservationSource source) {
        if (phenomenon.getPhenomenonType().getKind() != MeasurementKind.QUALITATIVE) {
            throw new IllegalArgumentException(
                "PhenomenonType for '" + phenomenon.getName() + "' is not QUALITATIVE");
        }

        CategoryObservation co = new CategoryObservation();
        co.setPatient(patient);
        co.setPhenomenon(phenomenon);
        co.setPresence(presence);
        co.setRecordingTime(LocalDateTime.now(clock));
        co.setApplicabilityTime(applicabilityTime != null ? applicabilityTime : LocalDateTime.now(clock));
        co.setProtocol(protocol);
        co.setStatus(ObservationStatus.ACTIVE);
        co.setSource(source);
        return co;
    }

    /** Convenience overload — defaults to MANUAL. Existing callers unchanged. */
    public CategoryObservation createCategoryObservation(Patient patient, Phenomenon phenomenon,
                                                        Presence presence,
                                                        LocalDateTime applicabilityTime,
                                                        Protocol protocol) {
        return createCategoryObservation(patient, phenomenon, presence,
                                        applicabilityTime, protocol, ObservationSource.MANUAL);
    }
}