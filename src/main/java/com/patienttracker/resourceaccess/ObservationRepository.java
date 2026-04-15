package com.patienttracker.resourceaccess;

import com.patienttracker.domain.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long> {
    // Used by the patient detail page — reverse-chronological
    List<Observation> findByPatientIdOrderByRecordingTimeDesc(Long patientId);
    // Used by rule evaluators
    List<Observation> findByPatientId(Long patientId);
}