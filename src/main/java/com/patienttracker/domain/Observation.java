package com.patienttracker.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.patienttracker.domain.enums.ObservationSource;
import com.patienttracker.domain.enums.ObservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "observations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "obs_type", discriminatorType = DiscriminatorType.STRING)
// Tell Jackson to include a "type" field in JSON so the frontend knows which kind it is
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Measurement.class, name = "MEASUREMENT"),
    @JsonSubTypes.Type(value = CategoryObservation.class, name = "CATEGORY")
})
@Getter @Setter @NoArgsConstructor
public abstract class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private LocalDateTime recordingTime;
    private LocalDateTime applicabilityTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "protocol_id")
    private Protocol protocol;

    @Enumerated(EnumType.STRING)
    private ObservationStatus status = ObservationStatus.ACTIVE;

    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private ObservationSource source = ObservationSource.MANUAL;

    private boolean anomalyFlag = false;
}