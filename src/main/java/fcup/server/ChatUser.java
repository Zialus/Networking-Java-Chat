package fcup.server;

import java.nio.channels.SocketChannel;

class ChatUser implements Comparable<ChatUser> {
    private String nick;
    private UserState userState;
    private SocketChannel socketChannel;
    private ChatRoom room;

    public ChatUser(SocketChannel socketChannel) {
        this.userState = UserState.INIT;
        this.socketChannel = socketChannel;
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

    public void setState(UserState newState) {
        this.userState = newState;
    }

    public String getNick() {
        return this.nick;
    }

    public void setNick(String newNick) {
        this.nick = newNick;
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

}
