package com.patienttracker.command;

import java.util.function.Supplier;

public class BaseCommand implements Command {

    private final String commandType;
    private final Supplier<String> payloadFn;  
    private String resolvedPayload;
    private final Runnable action;
    private final Runnable undoAction;         

    
    public BaseCommand(String commandType, String payload, Runnable action) {
        this(commandType, () -> payload, action, () -> {});
    }

    
    public BaseCommand(String commandType, Supplier<String> payloadFn,
                       Runnable action, Runnable undoAction) {
        this.commandType = commandType;
        this.payloadFn = payloadFn;
        this.action = action;
        this.undoAction = undoAction;
    }

    @Override
    public void execute() {
        action.run();
        resolvedPayload = payloadFn.get();  
    }

    @Override
    public void undo() {
        undoAction.run();
    }

    @Override
    public String getCommandType() { return commandType; }

    @Override
    public String getPayload() { return resolvedPayload; }
}