package network_structures;

import java.util.Arrays;

public class BaseMessage {

    private final String command;
    private final String[] args; //Optional
    private final Object data;
    private final long communicationIdentifier;

    public BaseMessage(String command, String[] args, Object data, long communicationIdentifier) {
        this.communicationIdentifier = communicationIdentifier;
        this.command = (command != null ? command : "");
        this.args = (args != null ? args : new String[0]);
        this.data = data;
    }

    public BaseMessage(String command, String[] args, Object data) {
        this(command, args, data, 0L);
    }

    public BaseMessage(String command, String[] args) {
        this(command, args, null);
    }

    public BaseMessage(String command, Object data) {
        this(command, null, data);
    }

    public BaseMessage(String command, long communicationIdentifier) {
        this(command, null, null, communicationIdentifier);
    }

    public BaseMessage(String command) {
        this(command, null, null);
    }

    public String getCommand() {
        return command;
    }

    public String[] getArgs() {
        return Arrays.copyOf(args, args.length);
    }

    public Object getData() {
        return data;
    }

    public long getCommunicationIdentifier() {
        return communicationIdentifier;
    }

    @Override
    public String toString() {
        String retVal = "[message]: {\n\tcommand: " + command + "\n\targs: ";
        if (args != null) {
            for (String arg : args) {
                retVal += arg + ", ";
            }
        }
        if (data != null) {
            retVal += "\n\t" + data.toString() + "\n}";
        }
        return retVal;
    }
}