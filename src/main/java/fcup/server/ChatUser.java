package fcup.server;

import java.nio.channels.SocketChannel;
import java.util.Objects;

class ChatUser implements Comparable<ChatUser> {
    private String nick;
    private UserState userState;
    private final SocketChannel socketChannel;
    private ChatRoom room;

    public ChatUser(final SocketChannel socketChannel) {
        this.userState = UserState.INIT;
        this.socketChannel = socketChannel;
        this.nick = "";
        this.room = null;
    }

    @Override
    public int compareTo(final ChatUser a) {
        return this.nick.compareTo(a.nick);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ChatUser chatUser = (ChatUser) o;
        return Objects.equals(nick, chatUser.nick);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nick);
    }

    public UserState getState() {
        return this.userState;
    }

    public void setState(final UserState newState) {
        this.userState = newState;
    }

    public String getNick() {
        return this.nick;
    }

    public void setNick(final String newNick) {
        this.nick = newNick;
    }

    public ChatRoom getRoom() {
        return this.room;
    }

    public void leftRoom() {
        this.room = null;
    }

    public void joinRoom(final ChatRoom newRoom) {
        this.room = newRoom;
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

}
