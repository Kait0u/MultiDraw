package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.ServerCommands;
import wit.pap.multidraw.shared.communication.ServerMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class User {
    private static final Logger log = LogManager.getLogger(User.class.getName());
    private Socket socket;
    private String nickname;
    private Room room;

    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public User(Socket socket, String nickname, Room room) {
        this.socket = socket;
        this.nickname = nickname;

        try {
            out = new ObjectOutputStream(this.socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (room != null) {
            try {
                setRoom(room);
            } catch (DuplicateNicknameException e) {

            }

        }
    }

    public ClientMessage receiveMessage() {
        try {
            return (ClientMessage) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error(e);
        }

        return null;
    }

    public void sendMessage(ServerMessage message) {
        if (message == null) return;

        try {
            out.writeObject(message);
            log.info(new StringBuilder("Sent message ").append(message));
        } catch (IOException e) {
            log.error(e);
        }
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
        } catch (DuplicateNicknameException e) {
            log.error(e);
            sendMessage(new ServerMessage(ServerCommands.REJECT_FROM_ROOM, e.getMessage().getBytes()));
            throw e;
        }
        this.room = room;
    }
}
