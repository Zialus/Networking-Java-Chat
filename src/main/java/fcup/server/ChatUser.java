package fcup.server;

import lombok.EqualsAndHashCode;

import java.nio.channels.SocketChannel;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class ChatUser implements Comparable<ChatUser> {
    @EqualsAndHashCode.Include
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
