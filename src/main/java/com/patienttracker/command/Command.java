package com.patienttracker.command;

public interface Command {
    void execute();
    void undo();          // new
    String getCommandType();
    String getPayload();
}