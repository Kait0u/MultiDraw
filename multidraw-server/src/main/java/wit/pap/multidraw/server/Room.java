package wit.pap.multidraw.server;

import java.security.InvalidParameterException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Room implements Runnable {
    private final String name;
    private final Set<User> users;
    private final AtomicBoolean isRunning;

    public Room(String name) {
        if (name == null)
            throw new InvalidParameterException("Name cannot be null!");

        this.name = name;
        this.users = new LinkedHashSet<>();
        this.isRunning = new AtomicBoolean(false);
    }

    public synchronized void addUser(User user) throws DuplicateNicknameException {
        synchronized (users) {
            if (this.users.stream().anyMatch(connectedUser -> connectedUser.getNickname().equals(user.getNickname())))
                throw new DuplicateNicknameException(
                        (new StringBuilder("User called \""))
                                .append(user.getNickname())
                                .append("\" already exists!").toString()
                );
            else
                users.add(user);
        }

    }

    public void removeUser(User user) {
        synchronized (users) {
            users.remove(user);
        }
    }

    @Override
    public void run() {
        isRunning.set(true);
        while (isRunning.get()) {

        }
    }

    public String getName() {
        return name;
    }
}
