package wit.pap.multidraw.globals;

public enum ClientCommands {
    PASS(0),
    JOIN_CREATE_ROOM(4),
    START_IMAGE_SEND(Globals.MAX_DATAGRAM_LENGTH_B),
    CONTINUE_IMAGE_SEND(Globals.MAX_DATAGRAM_LENGTH_B),
    END_IMAGE_SEND(Globals.MAX_DATAGRAM_LENGTH_B);

    private static class Index {
        private static int nextIdx = 0;

        private static synchronized int getNextIdx() {
            return nextIdx++;
        }
    }

    private final int idx, lengthBytes;

    private ClientCommands(int lengthBytes) {
        this.idx = Index.getNextIdx();
        this.lengthBytes = lengthBytes;
    }

    public int getIdx() {
        return idx;
    }

    public int getLengthBytes() {
        return lengthBytes;
    }
}
