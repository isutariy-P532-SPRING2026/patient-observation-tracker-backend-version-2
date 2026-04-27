package com.patienttracker.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patienttracker.domain.AuditLogEntry;
import com.patienttracker.domain.CommandLogEntry;
import com.patienttracker.domain.enums.ObservationStatus;
import com.patienttracker.resourceaccess.AuditLogRepository;
import com.patienttracker.resourceaccess.CommandLogRepository;
import com.patienttracker.resourceaccess.ObservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UndoService {

    private final CommandLogRepository commandLogRepository;
    private final ObservationRepository observationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public UndoService(CommandLogRepository commandLogRepository,
                       ObservationRepository observationRepository,
                       AuditLogRepository auditLogRepository,
                       ObjectMapper objectMapper) {
        this.commandLogRepository = commandLogRepository;
        this.observationRepository = observationRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public CommandLogEntry undo(Long entryId, String requestingUsername) {
        CommandLogEntry entry = commandLogRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Command log entry not found: " + entryId));

        if (entry.isUndone()) {
            throw new IllegalStateException("Command " + entryId + " has already been undone.");
        }
        if (!entry.getUser().equals(requestingUsername)) {
            throw new SecurityException("Only the original user may undo this command.");
        }

        switch (entry.getCommandType()) {
            case "RECORD_MEASUREMENT", "RECORD_CATEGORY_OBSERVATION" -> {
                Long obsId = extractObservationId(entry.getPayload());
                observationRepository.findById(obsId).ifPresent(obs -> {
                    obs.setStatus(ObservationStatus.REJECTED);
                    obs.setRejectionReason("Undone by user");
                    observationRepository.save(obs);
                    writeAuditEntry("UNDONE: observation #" + obsId,
                            obsId, obs.getPatient().getId());
                });
            }
            case "REJECT_OBSERVATION" -> {
                Long obsId = extractIdField(entry.getPayload(), "id");
                observationRepository.findById(obsId).ifPresent(obs -> {
                    obs.setStatus(ObservationStatus.ACTIVE);
                    obs.setRejectionReason(null);
                    observationRepository.save(obs);
                    writeAuditEntry("UNDO_REJECT: observation #" + obsId,
                            obsId, obs.getPatient().getId());
                });
            }
            case "CREATE_PATIENT" ->
                throw new UnsupportedOperationException("Undo is not supported for CREATE_PATIENT.");
            default ->
                throw new IllegalArgumentException("Unknown command type: " + entry.getCommandType());
        }

        entry.setUndone(true);
        return commandLogRepository.save(entry);
    }

    private void writeAuditEntry(String event, Long observationId, Long patientId) {
        AuditLogEntry audit = new AuditLogEntry();
        audit.setEvent(event);
        audit.setObservationId(observationId);
        audit.setPatientId(patientId);
        audit.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(audit);
    }

    private Long extractObservationId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return root.get("id").asLong();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse observation id from payload: " + payload, e);
        }
    }

    private Long extractIdField(String payload, String field) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return root.get(field).asLong();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse field '" + field + "' from payload: " + payload, e);
        }
    }
}