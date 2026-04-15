package com.patienttracker.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patienttracker.command.BaseCommand;
import com.patienttracker.command.CommandLog;
import com.patienttracker.domain.Patient;
import com.patienttracker.resourceaccess.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientManager {

    private final PatientRepository patientRepository;
    private final CommandLog commandLog;
    private final ObjectMapper objectMapper;

    public PatientManager(PatientRepository patientRepository,
                           CommandLog commandLog,
                           ObjectMapper objectMapper) {
        this.patientRepository = patientRepository;
        this.commandLog = commandLog;
        this.objectMapper = objectMapper;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatient(Long id) {
        return patientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + id));
    }

    public Patient createPatient(Patient patient) {
        Patient[] result = {null};
        commandLog.record(new BaseCommand(
            "CREATE_PATIENT",
            toJson(patient),
            () -> result[0] = patientRepository.save(patient)
        ));
        return result[0];
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}