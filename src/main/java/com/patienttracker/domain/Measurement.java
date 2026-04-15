package com.patienttracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("MEASUREMENT")
@Getter @Setter @NoArgsConstructor
public class Measurement extends Observation {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "phenomenon_type_id")
    private PhenomenonType phenomenonType;

    private Double amount;
    private String unit;
}