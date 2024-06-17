package wit.pap.multidraw.shared;

import java.io.Serializable;

public class Message implements Serializable {
    private byte commandCode;
    private int length;
    private byte[] payload;

    public Message(byte commandCode, byte[] payload, int length) {
        this.commandCode = commandCode;
        if (payload != null) {
            this.payload = payload;
            this.length = length;
        } else {
            this.length = 0;
            this.payload = new byte[this.length];
        }

    }

    public Message(byte commandCode, byte[] payload) {
        this(commandCode, payload, payload.length);
    }

    // Getters & Setters


    public byte getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(byte commandCode) {
        this.commandCode = commandCode;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}
