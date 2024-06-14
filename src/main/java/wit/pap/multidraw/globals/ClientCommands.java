package wit.pap.multidraw.globals;

public enum ClientCommands {
    JOIN_ROOM(0, 4);
    private int idx, lengthBytes;

    private ClientCommands(int idx, int argsLength) {
        this.idx = idx;
        this.argsLength = argsLength;
    }
}
