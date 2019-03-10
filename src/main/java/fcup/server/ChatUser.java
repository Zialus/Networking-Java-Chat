package fcup.server;

import java.nio.channels.SocketChannel;

class ChatUser implements Comparable<ChatUser> {
    private String nick;
    private UserState userState;
    private SocketChannel socketChannel;
    private ChatRoom room;

    public ChatUser(SocketChannel _socketChannel) {
        this.userState = UserState.INIT;
        this.socketChannel = _socketChannel;
        this.nick = "";
        this.room = null;
    }

    @Override
    public int compareTo(ChatUser a) {
        return this.nick.compareTo(a.nick);
    }

    public UserState getState() {
        return this.userState;
    }

    public String getNick() {
        return this.nick;
    }

    public ChatRoom getRoom() {
        return this.room;
    }

    public void leftRoom() {
        this.room = null;
    }

    public void joinRoom(ChatRoom newRoom) {
        this.room = newRoom;
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public void setState(UserState newState) {
        this.userState = newState;
    }

    public void setNick(String newNick) {
        this.nick = newNick;
    }

}
