package com.patienttracker.domain;

import com.patienttracker.domain.enums.Presence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CATEGORY")
@Getter @Setter @NoArgsConstructor
public class CategoryObservation extends Observation {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "phenomenon_id")
    private Phenomenon phenomenon;

    @Enumerated(EnumType.STRING)
    private Presence presence;
}