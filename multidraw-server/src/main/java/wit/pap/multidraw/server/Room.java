package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.ServerCommands;
import wit.pap.multidraw.shared.communication.ServerMessage;
import wit.pap.multidraw.shared.globals.Globals;

import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Room implements Runnable {
    private static final Logger log = LogManager.getLogger(Room.class.getName());
    private final String name;
    private final Set<User> users;
    private final AtomicBoolean isRunning;
    private Instant lastUserRemoval, lastDeadUserCheck;

    public Room(String name) {
        if (name == null)
            throw new InvalidParameterException("Name cannot be null!");

        this.name = name;
        this.users = new LinkedHashSet<>();
        this.isRunning = new AtomicBoolean(false);
        this.lastDeadUserCheck = Instant.now();
        this.lastUserRemoval = Instant.now();
    }

    @Override
    public void run() {
        isRunning.set(true);
        while (isRunning.get()) {
            catchDeadUsers();
            preventLinger();
        }

        log.info(new StringBuilder("Room \"").append(name).append("\" ceases to operate!"));
    }

    public void addUser(User user) throws DuplicateNicknameException {
        synchronized (users) {
            if (this.users.stream().anyMatch(connectedUser -> connectedUser.getNickname().equals(user.getNickname())))
                throw new DuplicateNicknameException(
                        (new StringBuilder("User called \""))
                                .append(user.getNickname())
                                .append("\" already exists!").toString()
                );
            else {
                users.add(user);
                log.info(
                        new StringBuilder("New user in Room ")
                                .append(name)
                                .append(". Current user count: ")
                                .append(users.size())
                );
                user.sendMessage(new ServerMessage(ServerCommands.ACCEPT_INT0_ROOM, null));
            }

        }

    }

    public void removeUser(User user) {
        synchronized (users) {
            users.remove(user);
            log.info(
                    new StringBuilder("User ")
                            .append(user.getNickname())
                            .append(" left the Room ").append(name).append(". Current user count: ")
                            .append(users.size())
            );
        }
        lastUserRemoval = Instant.now();
    }

    private void catchDeadUsers() {
        if (Duration.between(lastDeadUserCheck, Instant.now()).toSeconds() < Globals.DEAD_USERS_CHECK_INTERVAL_SECONDS)
            return;

        synchronized (users) {
            Iterator<User> uIt = users.iterator();
            while (uIt.hasNext()) {
                User u = uIt.next();
                boolean shouldDelete = u.isDead();

                if (!shouldDelete) {
                    try {
                        u.sendMessage(new ServerMessage(ServerCommands.POKE, null));
                    } catch (Exception e) {
                        log.error(e);
                        shouldDelete = true;
                    }
                }

                if (shouldDelete)
                    removeUser(u);
            }
        }

        lastDeadUserCheck = Instant.now();
    }

    private void receiveMessages() {
        synchronized (users) {
            for (User u: users) {

            }
        }
    }

    private void handleMessage(User sender, ClientMessage message) {
        if (message == null) return;

        switch (message.getClientCommand()) {
            case POKE, SET_NICKNAME, JOIN_CREATE_ROOM -> {}
            case SEND_IMAGE -> {}
            case DISCONNECT -> removeUser(sender);
        }
    }

    private void preventLinger() {
        Instant now = Instant.now();
        Duration timeSinceLastRemoval = Duration.between(lastUserRemoval, now);

        if (users.isEmpty() && timeSinceLastRemoval.toMinutes() >= Globals.MAX_ROOM_LINGER_MINUTES) {
            isRunning.set(false);
            log.info(new StringBuilder("Room \"").append(name).append("\" set to stop"));
        }

    }

    // Getters & Setters

    public String getName() {
        return name;
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
