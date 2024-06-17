package wit.pap.multidraw.server;

import java.net.Socket;

public class User implements Runnable {
    private Socket socket;
    private Room room;

    public User(Socket socket, Room room) {
        this.socket = socket;
        this.room = room;

        // Configure streams later
    }


    @Override
    public void run() {

    }
}
