package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.ServerCommands;
import wit.pap.multidraw.shared.communication.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class User {
    private static final Logger log = LogManager.getLogger(User.class.getName());
    private Socket socket;
    private String nickname;
    private Room room;

    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final AtomicBoolean isDead;

    public User(Socket socket, String nickname, Room room) {
        this.socket = socket;
        try {
            this.socket.setSoTimeout(400);
        } catch (SocketException e) {
            log.error(e);
        }
        this.nickname = nickname;
        this.isDead = new AtomicBoolean(false);

        try {
            out = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
            out.flush();
            in = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (room != null) {
            try {
                setRoom(room);
            } catch (DuplicateNicknameException e) {
                this.isDead.set(true);
            }
        }
    }

    public ClientMessage receiveMessage() {
        try {
            synchronized (in) {
                ClientMessage msg = (ClientMessage) in.readObject();
                log.info(new StringBuilder("[").append(this).append("] Message received: ").append(msg));
                return msg;
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error(e);
            this.isDead.set(true);
        }

        return null;
    }

    public void sendMessage(ServerMessage message) {
        if (message == null || out == null) return;

        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
            log.info(new StringBuilder("[").append(this).append("] Sent message: ").append(message));
        } catch (IOException e) {
            log.error(e);
            this.isDead.set(true);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("USER {")
                .append(room == null ? "<NO ROOM>" : room.getName())
                .append("/")
                .append(nickname == null ? "<NO NICKNAME | " + socket.getInetAddress() + ">" : nickname)
                .append("}")
                .toString();
    }

    // Getters & Setters

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) throws DuplicateNicknameException {
        try {
            room.addUser(this);
            synchronized (out) {
                out.reset();
            }
        } catch (DuplicateNicknameException e) {
            log.error(e);
            sendMessage(new ServerMessage(ServerCommands.REJECT_FROM_ROOM, e.getMessage().getBytes()));
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.room = room;
    }

    public boolean isDead() {
        return this.isDead.get();
    }
}
