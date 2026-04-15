package com.patienttracker.controller;

import com.patienttracker.domain.Patient;
import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.manager.ObservationManager;
import com.patienttracker.manager.PatientManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientManager patientManager;
    private final ObservationManager observationManager;

    public PatientController(PatientManager patientManager,
                              ObservationManager observationManager) {
        this.patientManager = patientManager;
        this.observationManager = observationManager;
    }

    @GetMapping
    public List<Patient> getAllPatients() {
        return patientManager.getAllPatients();
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
        return ResponseEntity.ok(patientManager.createPatient(patient));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatient(@PathVariable Long id) {
        return ResponseEntity.ok(patientManager.getPatient(id));
    }

    @GetMapping("/{id}/observations")
    public ResponseEntity<?> getObservations(@PathVariable Long id) {
        return ResponseEntity.ok(observationManager.getObservationsForPatient(id));
    }

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<List<PhenomenonType>> evaluateRules(@PathVariable Long id) {
        return ResponseEntity.ok(observationManager.evaluateRules(id));
    }
}