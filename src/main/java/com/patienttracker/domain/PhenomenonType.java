package com.patienttracker.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patienttracker.domain.enums.MeasurementKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "phenomenon_types")
@Getter @Setter @NoArgsConstructor
public class PhenomenonType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private MeasurementKind kind;

    // For QUANTITATIVE types — allowed measurement units (e.g. "kg", "mmHg")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "allowed_units", joinColumns = @JoinColumn(name = "phenomenon_type_id"))
    @Column(name = "unit")
    private Set<String> allowedUnits = new HashSet<>();

    // For QUALITATIVE types — the possible phenomena (e.g. "Blood Group A")
    @JsonIgnore   // break circular ref: PhenomenonType → Phenomenon → PhenomenonType
    @OneToMany(mappedBy = "phenomenonType", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Phenomenon> phenomena = new HashSet<>();
}