package fcup.server;

import java.util.Set;
import java.util.TreeSet;

class ChatRoom {
    private final String name;
    private final Set<ChatUser> users;

    public ChatRoom(final String name) {
        this.name = name;
        this.users = new TreeSet<>();
    }

    public ChatUser[] getUsers() {
        return this.users.toArray(new ChatUser[0]);
    }

    public String getName() {
        return this.name;
    }

    public void userJoin(final ChatUser user) {
        this.users.add(user);
    }

    public void userLeft(final ChatUser user) {
        this.users.remove(user);
    }

}
