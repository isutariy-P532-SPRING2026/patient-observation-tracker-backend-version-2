package com.patienttracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "associative_functions")
@Getter @Setter @NoArgsConstructor
public class AssociativeFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // The phenomenon types that must ALL be present to fire this rule
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rule_arguments",
        joinColumns = @JoinColumn(name = "rule_id"),
        inverseJoinColumns = @JoinColumn(name = "phenomenon_type_id")
    )
    private Set<PhenomenonType> argumentConcepts = new HashSet<>();

    // The inferred phenomenon type when the rule fires
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_concept_id")
    private PhenomenonType productConcept;

    private boolean active = true;
}