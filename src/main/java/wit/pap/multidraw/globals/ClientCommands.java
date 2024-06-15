package wit.pap.multidraw.globals;

public enum ClientCommands {
    PASS(0, 0),
    JOIN_ROOM(1, 4);

    private final int idx, lengthBytes;

    private ClientCommands(int idx, int lengthBytes) {
        this.idx = idx;
        this.lengthBytes = lengthBytes;
    }

    public int getIdx() {
        return idx;
    }

    public int getLengthBytes() {
        return lengthBytes;
    }
}
