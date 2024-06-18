package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.globals.Globals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiDrawServer {
    public static final Logger log = LogManager.getLogger(MultiDrawServer.class.getName());

    private final int port;
    private final ServerSocket serverSocket;
    private final AtomicBoolean isRunning;

    private final Thread userAssigner, deadRoomCleaner;


    private final Set<Room> rooms;
    private final Map<String, Room> nameRoomMap;
    private final Map<Room, Thread> roomThreadMap;
    private final Queue<Socket> toBeUsers;

    Instant lastDeadRoomCheck;

    public MultiDrawServer(int port) {
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.isRunning = new AtomicBoolean(false);
        this.rooms = new HashSet<>();
        this.nameRoomMap = new HashMap<>();
        this.roomThreadMap = new HashMap<>();
        this.toBeUsers = new ConcurrentLinkedQueue<>();

        this.lastDeadRoomCheck = Instant.now();

        this.userAssigner = new Thread(() -> {
            while (isRunning.get())
                assignUsers();
        });

        this.deadRoomCleaner = new Thread(() -> {
           while (isRunning.get()) {
               clearDeadRooms();
           }
        });
    }

    public void start() {
        isRunning.set(true);
        log.info(new StringBuilder("Server started on port ").append(port));
        run();
    }

    public void run() {
        userAssigner.start();
        deadRoomCleaner.start();

        while (isRunning.get()) {
            waitForUser();
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

    private void assignUsers() {
        while (!toBeUsers.isEmpty()) {
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

    private void clearDeadRooms() {
        if (Duration.between(lastDeadRoomCheck, Instant.now()).toSeconds() < Globals.DEAD_ROOM_CHECK_INTERVAL_SECONDS)
            return;

        log.info("Searching for dead rooms...");

        synchronized (rooms) {
            Iterator<Room> deadRoomIterator = rooms.iterator();

            while (deadRoomIterator.hasNext()) {
                Room r = deadRoomIterator.next();

                if (!r.isRunning()){
                    synchronized (roomThreadMap) {
                        Thread thread = roomThreadMap.getOrDefault(r, null);
                        if (thread != null) {
                            try {
                                thread.interrupt();
                                roomThreadMap.remove(r);
                            } catch (Exception e) {
                                log.error(e);
                            }
                        }
                    }

                    deadRoomIterator.remove();
                    log.info(new StringBuilder("Dead Room \"").append(r.getName()).append("\" removed!"));
                }
            }
        }
        lastDeadRoomCheck = Instant.now();
        log.info("Searching for dead rooms complete!");
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
