package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.ServerCommands;
import wit.pap.multidraw.shared.communication.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class User {
    private static final Logger log = LogManager.getLogger(User.class.getName());
    private final Socket socket;
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

        ObjectInputStream tempIn = null;
        ObjectOutputStream tempOut = null;

        try {
            tempOut = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
            tempOut.flush();
            tempIn = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
        } catch (IOException e) {
            log.error(e);
            markAsDead();
        }

        in = tempIn;
        out = tempOut;

        if (room != null) {
            try {
                setRoom(room);
            } catch (DuplicateNicknameException e) {
                this.isDead.set(true);
            }
        }
    }

    public ClientMessage receiveMessage() throws SocketException {
        synchronized (isDead) {
            if (isDead.get()) {
                log.warn(new StringBuilder("User ").append(this).append(" is dead, ignoring message receive order."));
                return null;
            }
        }

        try {
            synchronized (in) {
                ClientMessage msg = (ClientMessage) in.readObject();
                log.info(new StringBuilder("[").append(this).append("] Message received: ").append(msg));
                return msg;
            }
        } catch (SocketTimeoutException e) {

        } catch (IOException | ClassNotFoundException e) {
            log.error(e);
            this.isDead.set(true);
        }

        return null;
    }

    public void sendMessage(ServerMessage message) throws SocketException {
        if (isDead.get()) {
            log.warn(new StringBuilder("User ").append(this).append(" is dead, ignoring message send order."));
            return;
        }

        try {
            synchronized (out) {
                if (message == null) return;

                out.writeObject(message);
                out.flush();
            }
            log.info(new StringBuilder("[").append(this).append("] Sent message: ").append(message));
        } catch (IOException e) {
            log.error(e);
            this.isDead.set(true);
        }
    }

    public void close() throws IOException {
        markAsDead();

        synchronized (this) {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (SocketException ignored) { }
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
            try {
                sendMessage(new ServerMessage(ServerCommands.REJECT_FROM_ROOM, e.getMessage().getBytes()));
            } catch (SocketException ex) {
                log.error(ex);
                markAsDead();
            }
            throw e;
        } catch (IOException e) {
            log.error(e);
            markAsDead();
        }
        this.room = room;
    }

    public boolean getIsDead() {
        synchronized (isDead) {
            return this.isDead.get();
        }
    }

    public void markAsDead() {
        synchronized (isDead) {
            isDead.set(true);
        }

        log.info(new StringBuilder(this.toString()).append(" has been marked as dead!"));
    }
}
