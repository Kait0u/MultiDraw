package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.Message;
import wit.pap.multidraw.shared.communication.ClientCommands;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MultiDrawServer {
    public static final Logger log = LogManager.getLogger(MultiDrawServer.class.getName());

    private final int port;
    private final ServerSocket serverSocket;
    private boolean isRunning = false;

    private Set<User> users;
    private Queue<Socket> toBeUsers;

    public MultiDrawServer(int port) {
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.users = new LinkedHashSet<>();
        this.toBeUsers = new ConcurrentLinkedQueue<>();
    }

    public void start() {
        isRunning = true;
        log.info(new StringBuilder("Server started on port ").append(port));
        while (isRunning) {
            waitForUser();
            assignUsers();
        }
    }


    private void waitForUser() {
        try {
            Socket userSocket = serverSocket.accept();
            log.info(new StringBuilder("Accepted connection from ").append(serverSocket.getInetAddress()));
            toBeUsers.add(userSocket);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void assignUsers() {
        if (!toBeUsers.isEmpty()) {
            try {
                Socket socket = toBeUsers.poll();
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ClientMessage message = (ClientMessage) ois.readObject();
                log.info(new StringBuilder("Message received: ").append(message.getClientCommand().name()).append(" ")
                        .append(new String(message.getPayload())));

            } catch (IOException | ClassNotFoundException e) {
                log.error(e);
            }
        }
    }
}
