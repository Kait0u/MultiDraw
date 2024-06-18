package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MultiDrawServer {
    public static final Logger log = LogManager.getLogger(MultiDrawServer.class.getName());

    private final int port;
    private final ServerSocket serverSocket;
    private boolean isRunning = false;

    private final Set<User> users;
    private final Set<Room> rooms;
    private final Map<String, Room> nameRoomMap;
    private final Map<Room, Thread> roomThreadMap;
    private final Queue<Socket> toBeUsers;

    public MultiDrawServer(int port) {
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.rooms = new HashSet<>();
        this.nameRoomMap = new HashMap<>();
        this.roomThreadMap = new HashMap<>();
        this.users = new HashSet<>();
        this.toBeUsers = new ConcurrentLinkedQueue<>();
    }

    public void start() {
        isRunning = true;
        log.info(new StringBuilder("Server started on port ").append(port));
        run();
    }

    public void run() {
        while (isRunning) {
            waitForUser();
            assignUser();
        }
    }


    private void waitForUser() {
        try {
            Socket userSocket = serverSocket.accept();
            log.info(new StringBuilder("Accepted connection from ").append(userSocket.getInetAddress()));
            toBeUsers.add(userSocket);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void assignUser() {
        if (!toBeUsers.isEmpty()) {
            Socket socket = toBeUsers.poll();

            User user = new User(socket, null, null);
            log.info(new StringBuilder("Created initial user for ").append(socket.getInetAddress()));

            String nickname = null, roomName = null;

            while (nickname == null || roomName == null) {
                ClientMessage message = user.receiveMessage();
                logClientMessage(socket.getInetAddress(), message);

                switch (message.getClientCommand()) {
                    case SET_NICKNAME -> nickname = new String(message.getPayload());
                    case JOIN_CREATE_ROOM -> roomName = new String(message.getPayload());
                }
            }

            Room room = null;
            synchronized (nameRoomMap) {
                room = nameRoomMap.getOrDefault(roomName, null);
            }

            if (room == null) {
                room = createRoom(roomName);
            }

            try {
                user.setNickname(nickname);
                user.setRoom(room);
                synchronized (users) {
                    users.add(user);
                }
                log.info(new StringBuilder(socket.getInetAddress().toString())
                        .append(" became User ").append(nickname).append(" in room ").append(roomName));
            } catch (DuplicateNicknameException e) {
                log.error(
                        new StringBuilder("User ").append(nickname).append(" could not be added to room ")
                                .append(roomName).append(". Rejecting...")
                );
            }
        }
    }

    private Room createRoom(String name) {
        Room room = new Room(name);

        synchronized (rooms) {
            rooms.add(room);
        }
        synchronized (nameRoomMap) {
            nameRoomMap.put(name, room);
        }

        log.info(new StringBuilder("Room ").append(name).append(" has been created"));

        Thread roomThread = new Thread(room);
        synchronized (roomThreadMap) {
            roomThreadMap.put(room, roomThread);
        }

        roomThread.start();
        log.info(new StringBuilder("Room ").append(name).append(" has started execution!"));

        return room;
    }

    private void logClientMessage(ClientMessage message) {
        log.info(new StringBuilder("Message received: ").append(message.getClientCommand().name()).append(" ")
                .append(new String(message.getPayload())));
    }

    private void logClientMessage(InetAddress address, ClientMessage message) {
        log.info(new StringBuilder("[").append(address.toString()).append("] Message received: ").append(message.getClientCommand().name()).append(" ")
                .append(new String(message.getPayload())));
    }

}
