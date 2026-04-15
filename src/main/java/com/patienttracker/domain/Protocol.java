package com.patienttracker.domain;

import com.patienttracker.domain.enums.AccuracyRating;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "protocols")
@Getter @Setter @NoArgsConstructor
public class Protocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private AccuracyRating accuracyRating;
}