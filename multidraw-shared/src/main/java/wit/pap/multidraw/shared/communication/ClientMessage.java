package wit.pap.multidraw.shared.communication;

public class ClientMessage extends Message {
    private ClientCommands clientCommand;

    public ClientMessage(ClientCommands command, byte[] payload) {
        super(payload);
        this.clientCommand = command;
    }

    // Getters & Setters

    public ClientCommands getClientCommand() {
        return clientCommand;
    }

    public void setClientCommand(ClientCommands clientCommand) {
        this.clientCommand = clientCommand;
    }
}
