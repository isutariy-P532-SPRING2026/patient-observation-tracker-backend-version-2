package com.patienttracker.controller;

import com.patienttracker.config.CurrentUser;
import com.patienttracker.domain.CommandLogEntry;
import com.patienttracker.manager.UndoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/command-log")
public class UndoController {

    private final UndoService undoService;
    private final CurrentUser currentUser;

    public UndoController(UndoService undoService, CurrentUser currentUser) {
        this.undoService = undoService;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/undo")
    public ResponseEntity<CommandLogEntry> undo(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", defaultValue = "staff") String username) {
        currentUser.setUsername(username);
        return ResponseEntity.ok(undoService.undo(id, username));
    }
}