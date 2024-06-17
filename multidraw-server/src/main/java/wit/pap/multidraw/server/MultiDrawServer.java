package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public static final Logger log = LogManager.getLogger(MultiDrawServer.class.getName());

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
        log.info(new StringBuilder("Server started on port ").append(Integer.toString(port)));
        while (isRunning) {
            try {
                Socket userSocket = socket.accept();
                log.info(new StringBuilder("Accepted connection from ").append(socket.getInetAddress()));
                users.add(userSocket);
                ObjectOutputStream oos = new ObjectOutputStream(userSocket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(userSocket.getInputStream());
                Message message = (Message) ois.readObject();
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
