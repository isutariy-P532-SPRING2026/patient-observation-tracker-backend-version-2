package com.patienttracker.command;

import com.patienttracker.config.CurrentUser;
import com.patienttracker.domain.CommandLogEntry;
import com.patienttracker.resourceaccess.AppUserRepository;
import com.patienttracker.resourceaccess.CommandLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommandLog {

    private final CommandLogRepository repository;
    private final AppUserRepository userRepository;
    private final CurrentUser currentUser;

    public CommandLog(CommandLogRepository repository,
                      AppUserRepository userRepository,
                      CurrentUser currentUser) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    public CommandLogEntry record(Command command) {
        command.execute();

        String username = currentUser.getUsername();

        CommandLogEntry entry = new CommandLogEntry();
        entry.setCommandType(command.getCommandType());
        entry.setPayload(command.getPayload());   // now populated post-execute()
        entry.setExecutedAt(LocalDateTime.now());
        entry.setUser(username);
        entry.setUndone(false);

        // resolve userId if user exists in DB
        userRepository.findByUsername(username)
            .ifPresent(u -> entry.setUserId(u.getId()));

        return repository.save(entry);  // return saved entry (id now set)
    }
}