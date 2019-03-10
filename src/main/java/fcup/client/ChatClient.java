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


public class ChatClient {

    // GUI vars
    private JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();

    // Socket vars
    private SocketChannel socketChannel;
    private Boolean connectionOver = false;

    // Decoder/Encoder for text transmission
    private final Charset charset = Charset.forName("UTF8");
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
            try {
                newMessage(chatBox.getText());
            } catch (IOException ex) {
                System.out.println("There was an error getting text! (" + ex.getMessage() + ")");
            } finally {
                chatBox.setText("");
            }

            if (connectionOver) {
                Runtime.getRuntime().exit(0);
            }
        });

        // Setup Server Connection
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(server, port));
        } catch (IOException ex) {
            System.out.println("There was an error setting up the connection with the server! (" + ex.getMessage() + ")");
        }

    }

    // Message sender - send the message to the server
    private void newMessage(String message) throws IOException {
        socketChannel.write(encoder.encode(CharBuffer.wrap(message + "\n")));
    }

    // Listener of server messages
    private void run() throws IOException {

        socketChannel.finishConnect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream(), decoder));

        while (true) {
            String receivedMsg = reader.readLine();
            if (receivedMsg == null) {
                break;
            }
            receivedMsg = receivedMsg.trim();
            printMessage(ChatMessage.parseString(receivedMsg));
        }

        socketChannel.close();

        try {
            Thread.sleep(73);
        } catch (InterruptedException ex) {
            System.err.println("Ocorreu um error com a thread principal! (" + ex.getMessage() + ")");
            Runtime.getRuntime().exit(0);
            return;
        }

        connectionOver = true;
    }

    // Client Main
    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage: chatClient <server ip> <server port>");
            return;
        }

        String ip = args[0];
        String port = args[1];

        ChatClient client = new ChatClient(ip, Integer.parseInt(port));
        client.run();
    }

}
