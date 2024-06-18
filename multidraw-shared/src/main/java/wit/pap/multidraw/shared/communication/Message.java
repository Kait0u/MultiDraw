package wit.pap.multidraw.shared.communication;

import java.io.Serializable;

public abstract class Message implements Serializable {
    protected int length;
    protected byte[] payload;

    public Message(byte[] payload, int length) {
        if (payload != null) {
            this.payload = payload;
            this.length = length;
        } else {
            this.length = 0;
            this.payload = new byte[this.length];
        }

    }

    public Message(byte[] payload) {
        this(
                payload == null ? new byte[0] : payload,
                payload == null ? 0 : payload.length
        );
    }

    // Getters & Setters

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
