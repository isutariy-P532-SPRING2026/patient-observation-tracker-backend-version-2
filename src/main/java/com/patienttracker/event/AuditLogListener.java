package com.patienttracker.event;

import com.patienttracker.domain.AuditLogEntry;
import com.patienttracker.resourceaccess.AuditLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 *  Listener 1: appends an entry to the audit log whenever an observation event fires.
 */
@Component
public class AuditLogListener {

    private final AuditLogRepository auditLogRepository;

    public AuditLogListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @EventListener
    public void onObservationEvent(ObservationEvent event) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setEvent(event.getEventType() + ": observation #" + event.getObservation().getId());
        entry.setObservationId(event.getObservation().getId());
        entry.setPatientId(event.getObservation().getPatient().getId());
        entry.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(entry);
    }
}