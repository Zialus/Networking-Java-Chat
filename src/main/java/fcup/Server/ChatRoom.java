package fcup.Server;

import java.util.Set;
import java.util.TreeSet;

class ChatRoom {
    private String name;
    private Set<ChatUser> users;

    public ChatRoom(String _name) {
        this.name = _name;
        this.users = new TreeSet<>();
    }

    public ChatUser[] getUsers() {
        return this.users.toArray(new ChatUser[0]);
    }

    public String getName() {
        return this.name;
    }

    public void userJoin(ChatUser user) {
        this.users.add(user);
    }

    public void userLeft(ChatUser user) {
        this.users.remove(user);
    }

}
