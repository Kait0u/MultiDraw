package wit.pap.multidraw.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.jmx.Server;
import wit.pap.multidraw.shared.BgraImage;
import wit.pap.multidraw.shared.Utilities;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.ServerCommands;
import wit.pap.multidraw.shared.communication.ServerMessage;
import wit.pap.multidraw.shared.globals.Globals;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Room implements Runnable {
    private static final Logger log = LogManager.getLogger(Room.class.getName());
    private final String name;
    private final Set<User> users;
    private final Queue<ClientMessage> messagesToHandle;
    private final Map<ClientMessage, User> messageSenders;
    private final Queue<ServerMessage> messagesToSend;
    private final Map<ServerMessage, User> messageRecipients;
    private final Map<User, BgraImage> userImages;
    private final AtomicBoolean isRunning;

    private Instant lastUserRemoval, lastDeadUserCheck, lastImageMerge;

    public Room(String name) {
        if (name == null)
            throw new InvalidParameterException("Name cannot be null!");

        this.name = name;
        this.users = new LinkedHashSet<>();
        this.messagesToHandle = new ConcurrentLinkedQueue<>();
        this.messageSenders = new ConcurrentHashMap<>();
        this.messagesToSend = new ConcurrentLinkedQueue<>();
        this.messageRecipients = new ConcurrentHashMap<>();
        this.userImages = new ConcurrentHashMap<>();
        this.isRunning = new AtomicBoolean(false);
        this.lastDeadUserCheck = Instant.now();
        this.lastUserRemoval = Instant.now();
        this.lastImageMerge = Instant.now();
    }

    @Override
    public void run() {
        isRunning.set(true);
        while (isRunning.get()) {
            try {
                catchDeadUsers();

                boolean performActivities = false;
                synchronized (users) {
                    performActivities = !users.isEmpty();
                }

                if (performActivities) {
                    receiveMessages();
                    prepareMiddleGrounds();
                    handleMessages();
                    sendMessages();
                }

                preventLinger(false);
            }
            catch (Exception e) {
                log.error(e);
                preventLinger(true);
            }

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
                synchronized (userImages) {
                    userImages.put(user, BgraImage.createTransparent(Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT));
                }
                user.sendMessage(new ServerMessage(ServerCommands.ACCEPT_INT0_ROOM, null));
            }

        }

    }

    public void removeUser(User user) {
        synchronized (users) {
            users.remove(user);
            synchronized (userImages) {
                userImages.remove(user);
            }

            synchronized (messageRecipients) {
                List<ServerMessage> messagesToRemove = messageRecipients
                        .entrySet()
                        .parallelStream()
                        .filter(pair -> pair.getValue() == user)
                        .map(Map.Entry::getKey)
                        .toList();

                for (ServerMessage msg: messagesToRemove) {
                    messageRecipients.remove(msg);
                }
            }

            try {
                user.close();
            } catch (IOException e) {
                log.error(e);
            }

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
        if (users.isEmpty()) {
            log.info(new StringBuilder("Room \"").append(name).append("\" has no users to receive messages from!"));
            return;
        }
        Set<ClientMessage> toAdd = new HashSet<>();
        Map<ClientMessage, User> senders = new HashMap<>();
        synchronized (users) {
            for (User u: users) {
                ClientMessage message = u.receiveMessage();
                if (message != null) {
                    toAdd.add(message);
                    senders.put(message, u);
                }
            }
        }
        synchronized (messagesToHandle) {
            messagesToHandle.addAll(toAdd);
        }
        synchronized (messageSenders) {
            messageSenders.putAll(senders);
        }

        if (!toAdd.isEmpty()) {
            log.info(new StringBuilder("Room \"").append(name).append("\": messages received!"));
        }
    }

    private void prepareMiddleGrounds() {
        if (Duration.between(lastImageMerge, Instant.now()).toSeconds() < Globals.MIDDLEGROUND_CREATION_INTERVAL_SECONDS)
            return;

        if (users.isEmpty()) {
            log.info(new StringBuilder("Room \"").append(name).append("\" has no users to prepare middleGrounds for!"));
            return;
        }

        Map<User, BgraImage> middleGrounds = new HashMap<>();

        synchronized (userImages) {
            synchronized (users) {
                for (User destinationUser: users) {
                    List<BgraImage> sourceImages = userImages.entrySet()
                            .stream()
                            .filter(pair -> pair.getKey() != destinationUser)
                            .map(Map.Entry::getValue)
                            .filter(Objects::nonNull)
                            .toList()
                            .reversed();
                    BgraImage[] sourceImagesArr = sourceImages.toArray(new BgraImage[0]);
                    BgraImage middleGround = BgraImage.overlayAll(sourceImagesArr);
                    middleGrounds.put(destinationUser, middleGround);
                }
            }
        }

        log.info(new StringBuilder("Room \"").append(name).append("\" merged images into middlegrounds"));

        synchronized (messagesToSend) {
            synchronized (messageRecipients) {
                for (Map.Entry<User, BgraImage> pair: middleGrounds.entrySet()) {
                    User user = pair.getKey();
                    BgraImage mgImage = pair.getValue();


                    try {
                        byte[] mgImageBytes = Utilities.serializeIntoBytes(mgImage);
                        ServerMessage message = new ServerMessage(ServerCommands.SEND_MIDDLEGROUND, mgImageBytes);
                        messagesToSend.add(message);
                        messageRecipients.put(message, user);
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }

        log.info(new StringBuilder("Room \"").append(name).append("\" prepared middlegrounds for sending."));
    }

    private void sendMessages() {
        if (users.isEmpty()) {
            log.info(new StringBuilder("Room \"").append(name).append("\" has no users to send messages to!"));
            return;
        }

        log.info(new StringBuilder("Room \"").append(name).append("\": sending messages..."));
        synchronized (messagesToSend) {
            synchronized (messageRecipients) {
                while (!messagesToSend.isEmpty()) {
                    ServerMessage message = messagesToSend.poll();
                    User recipient = messageRecipients.get(message);

                    recipient.sendMessage(message);
                    messageRecipients.remove(message);
                }
            }
        }

        log.info(new StringBuilder("Room \"").append(name).append("\": sending complete!"));
        lastImageMerge = Instant.now();
    }

    private void handleMessage(User sender, ClientMessage message) {
        if (message == null) return;

        switch (message.getClientCommand()) {
            case POKE, SET_NICKNAME, JOIN_CREATE_ROOM -> {}
            case SEND_IMAGE -> {handleSendImage(sender, message);}
            case DISCONNECT -> removeUser(sender);
        }
    }

    private void handleMessages() {
        synchronized (messagesToHandle) {
            while (!messagesToHandle.isEmpty()) {
                ClientMessage message = messagesToHandle.poll();
                User sender = messageSenders.get(message);

                handleMessage(sender, message);
                messageSenders.remove(message);
            }
        }
    }

    private void preventLinger(boolean force) {
        Instant now = Instant.now();
        Duration timeSinceLastRemoval = Duration.between(lastUserRemoval, now);

        if (force || (users.isEmpty() && timeSinceLastRemoval.toMinutes() >= Globals.MAX_ROOM_LINGER_MINUTES)) {
            isRunning.set(false);
            log.info(new StringBuilder("Room \"").append(name).append("\" set to stop"));
        }

    }

    // Message handlers

    private void handleSendImage(User sender, ClientMessage message) {
        byte[] imageBytes = message.getPayload();
        try {
            BgraImage image = (BgraImage) Utilities.deserializeIntoObject(imageBytes);
            userImages.put(sender, image);
        } catch (IOException | ClassNotFoundException e) {
            log.error(e);
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
