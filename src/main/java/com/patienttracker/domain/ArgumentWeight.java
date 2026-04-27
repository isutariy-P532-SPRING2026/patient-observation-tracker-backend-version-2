package com.patienttracker.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "argument_weights")
@Getter @Setter @NoArgsConstructor
public class ArgumentWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore  // break circular ref: AssociativeFunction → ArgumentWeight → AssociativeFunction
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id")
    private AssociativeFunction rule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "phenomenon_type_id")
    private PhenomenonType phenomenonType;

    private Double weight = 1.0;
}