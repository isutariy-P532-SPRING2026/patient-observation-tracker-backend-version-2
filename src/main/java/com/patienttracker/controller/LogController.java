package com.patienttracker.controller;

import com.patienttracker.domain.AuditLogEntry;
import com.patienttracker.domain.CommandLogEntry;
import com.patienttracker.manager.LogManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LogController {

    private final LogManager logManager;

    public LogController(LogManager logManager) {
        this.logManager = logManager;
    }

    @GetMapping("/command-log")
    public List<CommandLogEntry> getCommandLog() {
        return logManager.getCommandLog();
    }

    @GetMapping("/audit-log")
    public List<AuditLogEntry> getAuditLog() {
        return logManager.getAuditLog();
    }
}