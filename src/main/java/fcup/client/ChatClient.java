package fcup.client;

import fcup.common.ChatMessage;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;


public class ChatClient {

    // GUI vars
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();

    // Socket vars
    private SocketChannel socketChannel;
    private Boolean connectionOver = false;

    // Decoder/Encoder for text transmission
    private final Charset charset = StandardCharsets.UTF_8;
    private final CharsetEncoder encoder = charset.newEncoder();
    private final CharsetDecoder decoder = charset.newDecoder();

    // GUI function to print message
    private void printMessage(final String message) {
        chatArea.append(message);
    }

    // Message printer (to chat)
    private void printMessage(final ChatMessage message) {
        printMessage(message.toString(true));
    }

    // Initializer: GUI and Server Connection
    private ChatClient(String server, int port) {

        // Setup GUI
        JFrame frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(e -> {
            if (Boolean.TRUE.equals(connectionOver)) {
                Runtime.getRuntime().exit(0);
            }

            newMessage(chatBox.getText());
            chatBox.setText("");
        });

        // Setup Server Connection
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(server, port));
        } catch (IOException ex) {
            System.err.println("There was an error setting up the connection with the server! (" + ex.getMessage() + ")");
        }

    }

    // Message sender - send the message to the server
    private void newMessage(String message) {
        try {
            socketChannel.write(encoder.encode(CharBuffer.wrap(message + "\n")));
        } catch (IOException e) {
            System.err.println("There was an error writing the message to the socket! (" + e.getMessage() + ")");
        }
    }

    // Listener of server messages
    private void run() throws IOException {

        socketChannel.finishConnect();

        InputStreamReader socketReader = new InputStreamReader(socketChannel.socket().getInputStream(), decoder);

        BufferedReader reader = new BufferedReader(socketReader);

        while (true) {
            String receivedMsg = reader.readLine();
            if (receivedMsg == null) {
                break; //Server sent a null message, that means I should disconnect from it
            }
            receivedMsg = receivedMsg.trim();
            printMessage(ChatMessage.parseString(receivedMsg));
        }

        socketChannel.close();

        System.out.println("Closing connection to the server...");

        try {
            Thread.sleep(73);
        } catch (InterruptedException ex) {
            System.err.println("Couldn't sleep! (" + ex.getMessage() + ")");
            Thread.currentThread().interrupt();
        }

        connectionOver = true;
    }

    // Client Main
    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println("Usage: chatClient <server ip> <server port>");
            return;
        }

        String ip = args[0];
        String port = args[1];

        ChatClient client = new ChatClient(ip, Integer.parseInt(port));
        client.run();
    }

}
