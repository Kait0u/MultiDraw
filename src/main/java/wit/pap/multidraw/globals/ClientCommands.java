package wit.pap.multidraw.globals;

public enum ClientCommands {
    JOIN_ROOM(0, 4);
    private int idx, lengthBytes;

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
