package wit.pap.multidraw.shared.communication;

import wit.pap.multidraw.shared.globals.Globals;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ServerMessage extends Message {
    ServerCommands serverCommand;

    public ServerMessage(ServerCommands command, byte[] payload) {
        super(payload);
        this.serverCommand = command;
    }

    @Override
    public String toString() {
        String payloadAsString = new String(payload);
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(payloadAsString))
            payloadAsString = Globals.BYTESTRING_INFO;

        return (new StringBuilder(this.serverCommand.name()).append(" ").append(payloadAsString).toString());
    }

    // Getters & Setters

    public ServerCommands getServerCommand() {
        return serverCommand;
    }

    public void setServerCommand(ServerCommands serverCommand) {
        this.serverCommand = serverCommand;
    }
}
