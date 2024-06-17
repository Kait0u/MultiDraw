package wit.pap.multidraw.server;

import java.util.LinkedHashSet;
import java.util.Set;

public class Room {
    private Set<User> users;

    public Room() {
        this.users = new LinkedHashSet<>();
    }

    public synchronized void addUser(User user) {
        users.add(user);
    }

    public synchronized void removeUser(User user) {
        users.remove(user);
    }



}
