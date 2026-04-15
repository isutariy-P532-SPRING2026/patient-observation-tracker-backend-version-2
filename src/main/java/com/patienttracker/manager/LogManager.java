package com.patienttracker.manager;

import com.patienttracker.domain.AuditLogEntry;
import com.patienttracker.domain.CommandLogEntry;
import com.patienttracker.resourceaccess.AuditLogRepository;
import com.patienttracker.resourceaccess.CommandLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogManager {

    private final CommandLogRepository commandLogRepository;
    private final AuditLogRepository auditLogRepository;

    public LogManager(CommandLogRepository commandLogRepository,
                       AuditLogRepository auditLogRepository) {
        this.commandLogRepository = commandLogRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<CommandLogEntry> getCommandLog() {
        return commandLogRepository.findAll();
    }

    public List<AuditLogEntry> getAuditLog() {
        return auditLogRepository.findAll();
    }
}