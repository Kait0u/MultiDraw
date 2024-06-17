package wit.pap.multidraw.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class User implements Runnable {
    private Socket socket;
    private Room room;

    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public User(Socket socket, Room room) {
        this.socket = socket;
        this.room = room;

        try {
            out = new ObjectOutputStream(this.socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run() {

    }
}
