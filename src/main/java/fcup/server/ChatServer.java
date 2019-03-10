package fcup.server;

import fcup.common.ChatMessage;
import fcup.common.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ChatServer {

    // Buffer for the received data
    static private final ByteBuffer inBuffer = ByteBuffer.allocate(16384);

    // Decoder/Encoder for text transmission
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetEncoder encoder = charset.newEncoder();
    static private final CharsetDecoder decoder = charset.newDecoder();

    // Users + Rooms vars
    static private Map<SocketChannel, ChatUser> users = new HashMap<>();
    static private Map<String, ChatUser> nicks = new HashMap<>();
    static private Map<String, ChatRoom> rooms = new HashMap<>();

    static private String incompleteMessage = "";
    static private boolean incomplete = false;

    private static void closeClient(SocketChannel socketChannel) throws IOException {

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
            removeUserFromRoom(user);
        }

        nicks.remove(user.getNick());
        users.remove(socketChannel);
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: chatServer <server port>");
            return;
        }

        String portStr = args[0];

        int port = Integer.parseInt(portStr);

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
            ex.printStackTrace();
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

        switch (msgParts[0]) {
            case "/nick":
                nickCommand(sender, msgParts);
                break;
            case "/join":
                joinCommand(sender, msgParts);
                break;
            case "/leave":
                leaveCommand(sender);
                break;
            case "/sala":
                salaCommand(sender);
                break;
            case "/bye":
                byeCommand(sender);
                break;
            case "/priv":
                privCommand(sender, msgParts);
                break;
            default:
                sendError(sender, "Invalid command!");
                break;
        }

    }

    private static void privCommand(ChatUser sender, String[] msgParts) throws IOException {
        if (msgParts.length < 2) {
            sendError(sender, "Uso inválido deste comando! Precisas de indicar um nick de destinatário");
            return;
        }

        String toNick = msgParts[1];
        if (!nicks.containsKey(toNick)) {
            sendError(sender, "O nick que indicaste não existe");
            return;
        }

        StringBuilder finalMessage = new StringBuilder();
        for (int i = 2; i < msgParts.length; i++) {
            if (i > 2)
                finalMessage.append(" ");
            finalMessage.append(msgParts[i]);
        }
        ChatMessage chatMessage = new ChatMessage(MessageType.PRIVATE, sender.getNick(), finalMessage.toString());
        sendMessage(nicks.get(toNick).getSocketChannel(), chatMessage);
        sendOk(sender);
    }

    private static void byeCommand(ChatUser sender) throws IOException {
        sendBye(sender);
        closeClient(sender.getSocketChannel());
    }

    private static void salaCommand(ChatUser sender) throws IOException {
        if (sender.getState() == UserState.INSIDE) {
            ChatRoom senderRoom = sender.getRoom();
            String roomName = senderRoom.getName();

            ChatMessage chatMessage = new ChatMessage(MessageType.SALA, sender.getNick(), roomName);

            sendMessage(sender.getSocketChannel(), chatMessage);
        } else {
            sendError(sender, "Não estás dentro de uma sala!");
        }
    }

    private static void leaveCommand(ChatUser sender) throws IOException {
        if (sender.getState() == UserState.INSIDE) {
            removeUserFromRoom(sender);
            sendOk(sender);
        } else {
            sendError(sender, "Precisas de estar dentro de uma sala para sair de uma sala!");
        }
    }

    private static void joinCommand(ChatUser sender, String[] msgParts) throws IOException {
        if (msgParts.length != 2) {
            sendError(sender, "Uso inválido deste comando! Precisas de indicar um nome de uma sala");
            return;
        }

        if (sender.getState() == UserState.INIT) {
            sendError(sender, "Precisas de ter um nick antes de entrar numa sala!");
            return;
        }

        if (sender.getState() == UserState.INSIDE) {
            removeUserFromRoom(sender);
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
    }

    private static void removeUserFromRoom(ChatUser user) throws IOException {
        ChatRoom senderRoom = user.getRoom();
        senderRoom.userLeft(user);

        ChatUser[] usersSameRoom = senderRoom.getUsers();
        ChatMessage chatMessage = new ChatMessage(MessageType.LEFT, user.getNick(), "");

        for (ChatUser to : usersSameRoom)
            sendMessage(to.getSocketChannel(), chatMessage);

        if (usersSameRoom.length == 0)
            rooms.remove(senderRoom.getName());

        user.leftRoom();
        user.setState(UserState.OUTSIDE);
    }

    private static void nickCommand(ChatUser sender, String[] msgParts) throws IOException {
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
                if (sender.equals(to))
                    sendMessage(to.getSocketChannel(), chatMessage);
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
            incompleteMessage += message;
            incomplete = true;
        } else if (incomplete) {
            incompleteMessage += message;
            message = incompleteMessage;
            incomplete = false;
            incompleteMessage = "";
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
