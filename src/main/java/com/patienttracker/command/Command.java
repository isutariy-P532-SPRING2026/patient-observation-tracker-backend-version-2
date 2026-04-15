package com.patienttracker.command;

public interface Command {
    void execute();
    String getCommandType();
    String getPayload();
}