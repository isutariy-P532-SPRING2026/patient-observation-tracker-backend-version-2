package com.patienttracker.command;

import com.patienttracker.domain.CommandLogEntry;
import com.patienttracker.resourceaccess.CommandLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Executes a command and immediately persists a log entry.
 * Hard-codes user = "staff" for Week 1.
 */
@Component
public class CommandLog {

    private final CommandLogRepository repository;

    public CommandLog(CommandLogRepository repository) {
        this.repository = repository;
    }

    public void record(Command command) {
        command.execute();   // do the work first

        CommandLogEntry entry = new CommandLogEntry();
        entry.setCommandType(command.getCommandType());
        entry.setPayload(command.getPayload());
        entry.setExecutedAt(LocalDateTime.now());
        entry.setUser("staff");
        repository.save(entry);
    }
}