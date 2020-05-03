package fcup.server;

import fcup.common.ChatMessage;
import fcup.common.MessageType;
import lombok.extern.java.Log;

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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Log
public class ChatServer {

    // Buffer for the received data
    private static final ByteBuffer inBuffer = ByteBuffer.allocate(16384);

    // Decoder/Encoder for text transmission
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final CharsetEncoder encoder = charset.newEncoder();
    private static final CharsetDecoder decoder = charset.newDecoder();

    // Users + Rooms vars
    private static final Map<SocketChannel, ChatUser> users = new HashMap<>();
    private static final Map<String, ChatUser> nicks = new HashMap<>();
    private static final Map<String, ChatRoom> rooms = new HashMap<>();

    private static String incompleteMessage = "";
    private static boolean incomplete = false;

    private static void closeClient(final SocketChannel socketChannel) throws IOException {

        final Socket socket = socketChannel.socket();

        try {
            log.info("Closing connection to " + socket);
            socketChannel.close();
        } catch (final IOException ex) {
            log.severe("Error closing socket " + socket + "! (" + ex + ")");
        }

        if (!users.containsKey(socketChannel))
            return;

        final ChatUser user = users.get(socketChannel);

        if (user.getState() == UserState.INSIDE) {
            removeUserFromRoom(user);
        }

        nicks.remove(user.getNick());
        users.remove(socketChannel);
    }

    private static int getPort(final String arg) {
        return Integer.parseInt(arg);
    }

    public static void main(final String[] args) {

        if (args.length != 1) {
            log.severe("Usage: chatServer <server port>");
            return;
        }

        final int port = getPort(args[0]);

        try (final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            run(port, serverSocketChannel);
        } catch (final IOException ex) {
            log.severe(ex.getMessage());
        }
    }

    private static void run(final int port, final ServerSocketChannel serverSocketChannel) throws IOException {
        // Setup server
        serverSocketChannel.configureBlocking(false);
        final ServerSocket serverSocket = serverSocketChannel.socket();
        final InetSocketAddress isa = new InetSocketAddress(port);
        serverSocket.bind(isa);

        final Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("Server listening on port " + port);

        serverLoop(serverSocket, selector);
    }

    private static void serverLoop(final ServerSocket serverSocket, final Selector selector) throws IOException {
        while (true) {
            final int num = selector.select();

            if (num == 0)
                continue;

            final Set<SelectionKey> keys = selector.selectedKeys();

            for (final SelectionKey key : keys) {
                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                    processNewConnection(serverSocket, selector);
                } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    processExistingConnection(key);
                }
            }

            keys.clear();
        }
    }

    private static void processExistingConnection(final SelectionKey key) throws IOException {
        SocketChannel socketChannel = null;

        try {
            // Reveived data on a connection
            socketChannel = (SocketChannel) key.channel();
            final boolean ok = processInput(socketChannel);

            // If the connection is dead, remove it from the selector and close it, and remove user also
            if (!ok) {
                key.cancel();
                closeClient(socketChannel);
            }

        } catch (final IOException ex) {
            // On exception, remove this channel from the selector and remove user
            key.cancel();
            closeClient(socketChannel);
        }
    }

    private static void processNewConnection(final ServerSocket serverSocket, final Selector selector) throws IOException {
        // Received a new incoming connection
        final Socket socket = serverSocket.accept();
        log.info("Got connection from " + socket);

        final SocketChannel socketChannel = socket.getChannel();
        socketChannel.configureBlocking(false);

        socketChannel.register(selector, SelectionKey.OP_READ);
        users.put(socketChannel, new ChatUser(socketChannel));
    }

    private static void sendMessage(final SocketChannel socketChannel, final ChatMessage message) throws IOException {
        socketChannel.write(encoder.encode(CharBuffer.wrap(message.toString(false))));
    }

    private static void sendError(final ChatUser to, final String message) throws IOException {
        final ChatMessage chatMessage = new ChatMessage(MessageType.ERROR, message, "");
        sendMessage(to.getSocketChannel(), chatMessage);
    }

    private static void sendOk(final ChatUser to) throws IOException {
        final ChatMessage chatMessage = new ChatMessage(MessageType.OK, "", "");
        sendMessage(to.getSocketChannel(), chatMessage);
    }

    private static void sendBye(final ChatUser to) throws IOException {
        final ChatMessage chatMessage = new ChatMessage(MessageType.BYE, "", "");
        sendMessage(to.getSocketChannel(), chatMessage);
    }

    private static void processCommand(final String message, final ChatUser sender) throws IOException {
        final String[] msgParts = message.split(" ");

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

    private static void privCommand(final ChatUser sender, final String[] msgParts) throws IOException {
        if (msgParts.length < 2) {
            sendError(sender, "Uso inválido deste comando! Precisas de indicar um nick de destinatário");
            return;
        }

        final String toNick = msgParts[1];
        if (!nicks.containsKey(toNick)) {
            sendError(sender, "O nick que indicaste não existe");
            return;
        }

        final StringBuilder finalMessage = new StringBuilder();
        for (int i = 2; i < msgParts.length; i++) {
            if (i > 2)
                finalMessage.append(" ");
            finalMessage.append(msgParts[i]);
        }
        final ChatMessage chatMessage = new ChatMessage(MessageType.PRIVATE, sender.getNick(), finalMessage.toString());
        sendMessage(nicks.get(toNick).getSocketChannel(), chatMessage);
        sendOk(sender);
    }

    private static void byeCommand(final ChatUser sender) throws IOException {
        sendBye(sender);
        closeClient(sender.getSocketChannel());
    }

    private static void salaCommand(final ChatUser sender) throws IOException {
        if (sender.getState() == UserState.INSIDE) {
            final ChatRoom senderRoom = sender.getRoom();
            final String roomName = senderRoom.getName();

            final ChatMessage chatMessage = new ChatMessage(MessageType.SALA, sender.getNick(), roomName);

            sendMessage(sender.getSocketChannel(), chatMessage);
        } else {
            sendError(sender, "Não estás dentro de uma sala!");
        }
    }

    private static void leaveCommand(final ChatUser sender) throws IOException {
        if (sender.getState() == UserState.INSIDE) {
            removeUserFromRoom(sender);
            sendOk(sender);
        } else {
            sendError(sender, "Precisas de estar dentro de uma sala para sair de uma sala!");
        }
    }

    private static void joinCommand(final ChatUser sender, final String[] msgParts) throws IOException {
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

        final String roomName = msgParts[1];

        if (!rooms.containsKey(roomName))
            rooms.put(roomName, new ChatRoom(roomName));

        final ChatRoom senderRoom = rooms.get(roomName);

        final ChatMessage chatMessage = new ChatMessage(MessageType.JOINED, sender.getNick(), "");
        final ChatUser[] usersSameRoom = senderRoom.getUsers();
        for (final ChatUser to : usersSameRoom)
            sendMessage(to.getSocketChannel(), chatMessage);

        sender.joinRoom(senderRoom);
        senderRoom.userJoin(sender);
        sender.setState(UserState.INSIDE);
        sendOk(sender);
    }

    private static void removeUserFromRoom(final ChatUser user) throws IOException {
        final ChatRoom senderRoom = user.getRoom();
        senderRoom.userLeft(user);

        final ChatUser[] usersSameRoom = senderRoom.getUsers();
        final ChatMessage chatMessage = new ChatMessage(MessageType.LEFT, user.getNick(), "");

        for (final ChatUser to : usersSameRoom)
            sendMessage(to.getSocketChannel(), chatMessage);

        if (usersSameRoom.length == 0)
            rooms.remove(senderRoom.getName());

        user.leftRoom();
        user.setState(UserState.OUTSIDE);
    }

    private static void nickCommand(final ChatUser sender, final String[] msgParts) throws IOException {
        //apenas tem 2 palavras
        if (msgParts.length != 2) {
            sendError(sender, "Uso inválido deste comando! Precisas de indicar um nick!");
            return;
        }

        final String newNick = msgParts[1];

        //nick já tá a ser utilizado?
        if (nicks.containsKey(newNick)) {
            sendError(sender, "Este nick já está a ser utilizado!");
            return;
        }

        final String oldNick = sender.getNick();
        nicks.remove(oldNick);
        sender.setNick(newNick);
        nicks.put(newNick, sender);
        sendOk(sender);

        if (sender.getState() == UserState.INIT)
            sender.setState(UserState.OUTSIDE);
        else if (sender.getState() == UserState.INSIDE) {
            final ChatUser[] usersSameRoom = sender.getRoom().getUsers();
            final ChatMessage chatMessage = new ChatMessage(MessageType.NEWNICK, oldNick, newNick);

            for (final ChatUser to : usersSameRoom)
                if (sender.equals(to))
                    sendMessage(to.getSocketChannel(), chatMessage);
        }
    }

    private static void processMessage(final String message, final ChatUser sender) throws IOException {
        if (sender.getState() == UserState.INSIDE) {
            final ChatRoom senderRoom = sender.getRoom();
            final ChatUser[] usersSameRoom = senderRoom.getUsers();
            for (final ChatUser to : usersSameRoom) {
                final ChatMessage chatMessage = new ChatMessage(MessageType.MESSAGE, sender.getNick(), message);
                sendMessage(to.getSocketChannel(), chatMessage);
            }
        } else
            sendError(sender, "É preciso estares dentro de uma sala para poderes enviar uma mensagem");
    }

    private static boolean processInput(final SocketChannel socketChannel) throws IOException {

        inBuffer.clear();
        socketChannel.read(inBuffer);
        inBuffer.flip();

        if (inBuffer.limit() == 0)
            return false;

        // Decode message
        String message = decoder.decode(inBuffer).toString();
        final ChatUser sender = users.get(socketChannel);

        message = processMessageCompleteness(message);

        if (!incomplete) {
            final String[] split = message.split("\n+");
            for (final String msg : split) {
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

    private static String processMessageCompleteness(String message) {
        if (!message.contains("\n")) {
            incompleteMessage += message;
            incomplete = true;
        } else if (incomplete) {
            incompleteMessage += message;
            message = incompleteMessage;
            incomplete = false;
            incompleteMessage = "";
        }
        return message;
    }
}
