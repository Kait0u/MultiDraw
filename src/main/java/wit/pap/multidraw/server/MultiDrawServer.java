package wit.pap.multidraw.server;

import wit.pap.multidraw.shared.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class MultiDrawServer {

    private final int port;
    private final ServerSocket socket;
    private boolean isRunning = false;

    private Set<Socket> users;

    public MultiDrawServer(int port) {
        this.port = port;
        try {
            this.socket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.users = new LinkedHashSet<>();
    }

    public void start() {
        isRunning = true;
        while (isRunning) {
            try {
                Socket userSocket = socket.accept();
                users.add(userSocket);
                System.out.println("Got something!");
                ObjectOutputStream oos = new ObjectOutputStream(userSocket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(userSocket.getInputStream());
                System.out.println("Got here!");
                Message message = (Message) ois.readObject();
                System.out.println("Hi!");
                System.out.println(message.getCommandCode());
                System.out.println(Arrays.toString(message.getPayload()));
                System.out.println(new String(message.getPayload()));
            } catch (IOException e) {

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
