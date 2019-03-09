package fcup.Server;

import fcup.common.ChatMessage;
import fcup.common.MessageType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;


public class ChatServer {

    // Buffer for the received data
    static private final ByteBuffer inBuffer = ByteBuffer.allocate(16384);

    // Decoder/Encoder for text transmission
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetEncoder encoder = charset.newEncoder();
    static private final CharsetDecoder decoder = charset.newDecoder();

    // Users + Rooms vars
    static private HashMap<SocketChannel, ChatUser> users = new HashMap<>();
    static private HashMap<String, ChatUser> nicks = new HashMap<>();
    static private HashMap<String, ChatRoom> rooms = new HashMap<>();

    static private String incomplete_message = new String("");
    static private boolean incomplete = false;

    public static void closeClient(SocketChannel socketChannel) throws IOException {

        Socket socket = socketChannel.socket();

        try {
            System.out.println("Closing connection to " + socket);
            socketChannel.close();
        } catch (IOException ex) {
            System.err.println("Error closing socket " + socket + "! (" + ex + ")");
        }

        if (!users.containsKey(socketChannel))
            return;

        ChatUser user = users.get(socketChannel);

        if (user.getState() == UserState.INSIDE) {

            ChatRoom userRoom = user.getRoom();
            userRoom.userLeft(user);
            ChatUser[] usersSameRoom = userRoom.getUsers();
            ChatMessage chatMessage = new ChatMessage(MessageType.LEFT, user.getNick(), "");
            for (ChatUser to : usersSameRoom) {
                sendMessage(to.getSocketChannel(), chatMessage);
            }
            if (usersSameRoom.length == 0) {
                rooms.remove(userRoom.getName());
            }
        }

        nicks.remove(user.getNick());
        users.remove(socketChannel);
    }

    public static void main(String args[]) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: chatServer <server port>");
            return;
        }

        String portStr = args[0];

        Integer port = Integer.parseInt(portStr);

        try {
            // Setup server
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress isa = new InetSocketAddress(port);
            serverSocket.bind(isa);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server listening on port " + port);

            while (true) {
                int num = selector.select();

                if (num == 0)
                    continue;

                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {

                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

                        // Received a new incoming connection
                        Socket socket = serverSocket.accept();
                        System.out.println("Got connection from " + socket);

                        SocketChannel socketChannel = socket.getChannel();
                        socketChannel.configureBlocking(false);

                        socketChannel.register(selector, SelectionKey.OP_READ);
                        users.put(socketChannel, new ChatUser(socketChannel));

                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                        SocketChannel socketChannel = null;

                        try {

                            // Reveived data on a connection
                            socketChannel = (SocketChannel) key.channel();
                            boolean ok = processInput(socketChannel);

                            // If the connection is dead, remove it from the selector and close it, and remove user also
                            if (!ok) {
                                key.cancel();
                                closeClient(socketChannel);
                            }

                        } catch (IOException ex) {

                            // On exception, remove this channel from the selector and remove user
                            key.cancel();
                            closeClient(socketChannel);
                        }
                    }
                }

                keys.clear();
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private static void sendMessage(SocketChannel socketChannel, ChatMessage message) throws IOException {
        socketChannel.write(encoder.encode(CharBuffer.wrap(message.toString(false))));
    }

    private static void sendError(ChatUser to, String message) throws IOException {
        ChatMessage chatMessage = new ChatMessage(MessageType.ERROR, message, "");
        sendMessage(to.getSocketChannel(), chatMessage);
    }

    private static void sendOk(ChatUser to) throws IOException {
        ChatMessage chatMessage = new ChatMessage(MessageType.OK, "", "");
        sendMessage(to.getSocketChannel(), chatMessage);
    }

    private static void sendBye(ChatUser to) throws IOException {
        ChatMessage chatMessage = new ChatMessage(MessageType.BYE, "", "");
        sendMessage(to.getSocketChannel(), chatMessage);
    }

    private static void processCommand(String message, ChatUser sender) throws IOException {
        String[] msgParts = message.split(" ");

        if (msgParts[0].equals("/nick")) {
            //apenas tem 2 palavras
            if (msgParts.length != 2) {
                sendError(sender, "Uso inválido deste comando! Precisas de indicar um nick!");
                return;
            }

            String newNick = msgParts[1];

            //nick já tá a ser utilizado?
            if (nicks.containsKey(newNick)) {
                sendError(sender, "Este nick já está a ser utilizado!");
                return;
            }

            String oldNick = sender.getNick();
            nicks.remove(oldNick);
            sender.setNick(newNick);
            nicks.put(newNick, sender);
            sendOk(sender);

            if (sender.getState() == UserState.INIT)
                sender.setState(UserState.OUTSIDE);
            else if (sender.getState() == UserState.INSIDE) {
                ChatUser[] usersSameRoom = sender.getRoom().getUsers();
                ChatMessage chatMessage = new ChatMessage(MessageType.NEWNICK, oldNick, newNick);

                for (ChatUser to : usersSameRoom)
                    if (sender != to)
                        sendMessage(to.getSocketChannel(), chatMessage);
            }

        } else if (msgParts[0].equals("/join")) {

            if (msgParts.length != 2) {
                sendError(sender, "Uso inválido deste comando! Precisas de indicar um nome de uma sala");
                return;
            }

            if (sender.getState() == UserState.INIT) {
                sendError(sender, "Precisas de ter um nick antes de poderes entrar numa sala!");
                return;
            }

            if (sender.getState() == UserState.INSIDE) {
                ChatRoom senderRoom = sender.getRoom();
                senderRoom.userLeft(sender);

                ChatUser[] usersSameRoom = senderRoom.getUsers();
                ChatMessage chatMessage = new ChatMessage(MessageType.LEFT, sender.getNick(), "");

                for (ChatUser to : usersSameRoom)
                    sendMessage(to.getSocketChannel(), chatMessage);

                if (usersSameRoom.length == 0)
                    rooms.remove(senderRoom.getName());

                sender.leftRoom();
                sender.setState(UserState.OUTSIDE);
            }

            String roomName = msgParts[1];

            if (!rooms.containsKey(roomName))
                rooms.put(roomName, new ChatRoom(roomName));

            ChatRoom senderRoom = rooms.get(roomName);

            ChatMessage chatMessage = new ChatMessage(MessageType.JOINED, sender.getNick(), "");
            ChatUser[] usersSameRoom = senderRoom.getUsers();
            for (ChatUser to : usersSameRoom)
                sendMessage(to.getSocketChannel(), chatMessage);

            sender.joinRoom(senderRoom);
            senderRoom.userJoin(sender);
            sender.setState(UserState.INSIDE);
            sendOk(sender);

        } else if (msgParts[0].equals("/leave")) {

            if (sender.getState() == UserState.INSIDE) {
                ChatRoom senderRoom = sender.getRoom();
                senderRoom.userLeft(sender);

                ChatUser[] usersSameRoom = senderRoom.getUsers();
                ChatMessage chatMessage = new ChatMessage(MessageType.LEFT, sender.getNick(), "");

                for (ChatUser to : usersSameRoom)
                    sendMessage(to.getSocketChannel(), chatMessage);

                if (usersSameRoom.length == 0)
                    rooms.remove(senderRoom.getName());

                sender.leftRoom();
                sender.setState(UserState.OUTSIDE);
                sendOk(sender);

            } else {
                sendError(sender, "Precisas de estar dentro de uma sala para enviar uma mensagem!");
            }

        } else if (msgParts[0].equals("/sala")) {
            if (sender.getState() == UserState.INSIDE) {
                ChatRoom senderRoom = sender.getRoom();
                String roomName = senderRoom.getName();

                ChatMessage chatMessage = new ChatMessage(MessageType.SALA, sender.getNick(), roomName);

                sendMessage(sender.getSocketChannel(), chatMessage);
            } else {
                sendError(sender, "Não estás dentro de uma sala!");
            }
        } else if (msgParts[0].equals("/bye")) {

            sendBye(sender);
            closeClient(sender.getSocketChannel());

        } else if (msgParts[0].equals("/priv")) {

            if (msgParts.length < 2) {
                sendError(sender, "Uso inválido deste comando! Precisas de indicar um nick de destinatário");
                return;
            }

            String toNick = msgParts[1];
            if (!nicks.containsKey(toNick)) {
                sendError(sender, "O nick que indicaste não existe");
                return;
            }

            String finalMessage = "";
            for (int i = 2; i < msgParts.length; i++) {
                if (i > 2)
                    finalMessage += " ";
                finalMessage += msgParts[i];
            }
            ChatMessage chatMessage = new ChatMessage(MessageType.PRIVATE, sender.getNick(), finalMessage);
            sendMessage(nicks.get(toNick).getSocketChannel(), chatMessage);
            sendOk(sender);

        } else {
            sendError(sender, "Invalid command!");
        }

    }

    private static void processMessage(String message, ChatUser sender) throws IOException {
        if (sender.getState() == UserState.INSIDE) {
            ChatRoom senderRoom = sender.getRoom();
            ChatUser[] usersSameRoom = senderRoom.getUsers();
            for (ChatUser to : usersSameRoom) {
                ChatMessage chatMessage = new ChatMessage(MessageType.MESSAGE, sender.getNick(), message);
                sendMessage(to.getSocketChannel(), chatMessage);
            }
        } else
            sendError(sender, "É preciso estares dentro de uma sala para poderes enviar uma mensagem");
    }

    private static boolean processInput(SocketChannel socketChannel) throws IOException {

        inBuffer.clear();
        socketChannel.read(inBuffer);
        inBuffer.flip();

        if (inBuffer.limit() == 0)
            return false;

        // Decode message
        String message = decoder.decode(inBuffer).toString();
        ChatUser sender = users.get(socketChannel);

        if (!message.contains("\n")) {
            incomplete_message += message;
            incomplete = true;
        } else if (incomplete) {
            incomplete_message += message;
            message = incomplete_message;
            incomplete = false;
            incomplete_message = "";
        }

        if (!incomplete) {
            String[] split = message.split("\n+");
            for (String msg : split) {
                if (msg.length() > 0 && msg.charAt(0) == '/') {
                    if (msg.length() > 1 && msg.charAt(1) == '/')
                        processMessage(msg.substring(1), sender);
                    else {
                        processCommand(msg, sender);
                    }
                } else if (msg.length() > 0)
                    processMessage(msg, sender);
            }
        }
        return true;
    }
}


enum UserState {INIT, OUTSIDE, INSIDE}

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
