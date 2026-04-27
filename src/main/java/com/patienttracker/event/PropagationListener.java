package com.patienttracker.event;

import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.ObservationSource;
import com.patienttracker.domain.enums.ObservationStatus;
import com.patienttracker.domain.enums.Presence;
import com.patienttracker.factory.ObservationFactory;
import com.patienttracker.resourceaccess.ObservationRepository;
import com.patienttracker.resourceaccess.PhenomenonRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class PropagationListener {

    private final ObservationRepository observationRepository;
    private final PhenomenonRepository phenomenonRepository;
    private final ObservationFactory factory;

    public PropagationListener(ObservationRepository observationRepository,
                                PhenomenonRepository phenomenonRepository,
                                ObservationFactory factory) {
        this.observationRepository = observationRepository;
        this.phenomenonRepository = phenomenonRepository;
        this.factory = factory;
    }

    @EventListener
    public void onObservationEvent(ObservationEvent event) {
        
        Observation obs = event.getObservation();
        if (!(obs instanceof CategoryObservation co)) return;
        if (obs.getSource() != ObservationSource.MANUAL) return;  
        if (obs.getStatus() != ObservationStatus.ACTIVE) return;

        Patient patient = obs.getPatient();
        List<Observation> existing = observationRepository.findByPatientId(patient.getId());

        if (co.getPresence() == Presence.PRESENT) {
            propagatePresent(patient, co.getPhenomenon(), existing);
        } else {
            propagateAbsent(patient, co.getPhenomenon(), existing);
        }
    }


    private void propagatePresent(Patient patient, Phenomenon phenomenon,
                                   List<Observation> existing) {
        Set<Long> alreadyPresent = existingPresentIds(existing);
        List<Phenomenon> ancestors = getAncestors(phenomenon);

        for (Phenomenon ancestor : ancestors) {
            if (!alreadyPresent.contains(ancestor.getId())) {
                CategoryObservation inferred = factory.createCategoryObservation(
                    patient, ancestor, Presence.PRESENT, null, null,
                    ObservationSource.INFERRED);
                observationRepository.save(inferred);
                alreadyPresent.add(ancestor.getId()); 
            }
        }
    }


    private void propagateAbsent(Patient patient, Phenomenon phenomenon,
                                  List<Observation> existing) {
        Set<Long> alreadyAbsent = existingAbsentIds(existing);
        List<Phenomenon> descendants = getDescendants(phenomenon);

        for (Phenomenon descendant : descendants) {
            if (!alreadyAbsent.contains(descendant.getId())) {
                CategoryObservation inferred = factory.createCategoryObservation(
                    patient, descendant, Presence.ABSENT, null, null,
                    ObservationSource.INFERRED);
                observationRepository.save(inferred);
                alreadyAbsent.add(descendant.getId());
            }
        }
    }

    private List<Phenomenon> getAncestors(Phenomenon p) {
        List<Phenomenon> ancestors = new ArrayList<>();
        Phenomenon cursor = p.getParentConcept();
        while (cursor != null) {
            ancestors.add(cursor);
            cursor = cursor.getParentConcept();
        }
        return ancestors;
    }

    private List<Phenomenon> getDescendants(Phenomenon p) {
        List<Phenomenon> result = new ArrayList<>();
        collectDescendants(p, result);
        return result;
    }

    private void collectDescendants(Phenomenon p, List<Phenomenon> acc) {
        List<Phenomenon> children = phenomenonRepository.findByParentConceptId(p.getId());
        for (Phenomenon child : children) {
            acc.add(child);
            collectDescendants(child, acc);
        }
    }


    private Set<Long> existingPresentIds(List<Observation> existing) {
        return existing.stream()
            .filter(o -> o instanceof CategoryObservation co
                      && co.getPresence() == Presence.PRESENT
                      && o.getStatus() == ObservationStatus.ACTIVE)
            .map(o -> ((CategoryObservation) o).getPhenomenon().getId())
            .collect(Collectors.toSet());
    }

    private Set<Long> existingAbsentIds(List<Observation> existing) {
        return existing.stream()
            .filter(o -> o instanceof CategoryObservation co
                      && co.getPresence() == Presence.ABSENT
                      && o.getStatus() == ObservationStatus.ACTIVE)
            .map(o -> ((CategoryObservation) o).getPhenomenon().getId())
            .collect(Collectors.toSet());
    }
}