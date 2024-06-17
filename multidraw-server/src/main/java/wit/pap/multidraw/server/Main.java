package wit.pap.multidraw.server;

public class Main {
    public static void main(String[] args) {
        MultiDrawServer server = new MultiDrawServer(12345);
        server.start();
    }
}
