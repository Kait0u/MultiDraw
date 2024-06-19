package wit.pap.multidraw.shared.communication;

import wit.pap.multidraw.shared.globals.Globals;

import java.nio.charset.StandardCharsets;

public class ClientMessage extends Message {
    private ClientCommands clientCommand;

    public ClientMessage(ClientCommands command, byte[] payload) {
        super(payload);
        this.clientCommand = command;
    }

    @Override
    public String toString() {
        String payloadAsString = new String(payload);
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(payloadAsString))
            payloadAsString = Globals.BYTESTRING_INFO;

        return (new StringBuilder(this.clientCommand.name()).append(" ").append(payloadAsString).toString());
    }

    // Getters & Setters

    public ClientCommands getClientCommand() {
        return clientCommand;
    }

    public void setClientCommand(ClientCommands clientCommand) {
        this.clientCommand = clientCommand;
    }
}
