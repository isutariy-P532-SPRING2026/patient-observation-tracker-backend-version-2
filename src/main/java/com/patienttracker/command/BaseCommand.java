package com.patienttracker.command;

/**.
 * Action is injected as a Runnable so managers can create commands inline.
 */
public class BaseCommand implements Command {

    private final String commandType;
    private final String payload;
    private final Runnable action;

    public BaseCommand(String commandType, String payload, Runnable action) {
        this.commandType = commandType;
        this.payload = payload;
        this.action = action;
    }

    @Override
    public void execute() {
        action.run();
    }

    @Override
    public String getCommandType() { return commandType; }

    @Override
    public String getPayload() { return payload; }
}