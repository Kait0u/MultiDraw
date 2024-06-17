package wit.pap.multidraw.shared.communication;

public class ServerMessage extends Message {
    ServerCommands serverCommand;


    public ServerMessage(ServerCommands command, byte[] payload) {
        super(payload);
        this.serverCommand = command;
    }

    // Getters & Setters


    public ServerCommands getServerCommand() {
        return serverCommand;
    }

    public void setServerCommand(ServerCommands serverCommand) {
        this.serverCommand = serverCommand;
    }
}
