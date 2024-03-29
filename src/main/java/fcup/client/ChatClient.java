package fcup.client;

import fcup.common.ChatMessage;
import lombok.extern.java.Log;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
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

@Log
public class ChatClient {

    // GUI vars
    private final JTextField chatBox = new JTextField();
    private final JTextArea chatArea = new JTextArea();

    // Socket vars
    private SocketChannel socketChannel;
    private boolean connectionOver = false;

    // Decoder/Encoder for text transmission
    private static final Charset charset = StandardCharsets.UTF_8;
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
    private ChatClient(final String server, final int port) {

        // Setup GUI
        final JFrame frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final JPanel panel = new JPanel();
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
            if (connectionOver) {
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
        } catch (final IOException ex) {
            log.severe("There was an error setting up the connection with the server! (" + ex.getMessage() + ")");
        }

    }

    // Message sender - send the message to the server
    private void newMessage(final String message) {
        try {
            socketChannel.write(encoder.encode(CharBuffer.wrap(message + "\n")));
        } catch (final IOException e) {
            log.severe("There was an error writing the message to the socket! (" + e.getMessage() + ")");
        }
    }

    // Listener of server messages
    private void run() throws IOException {

        socketChannel.finishConnect();

        final InputStreamReader socketReader = new InputStreamReader(socketChannel.socket().getInputStream(), decoder);

        final BufferedReader reader = new BufferedReader(socketReader);

        while (true) {
            String receivedMsg = reader.readLine();
            if (receivedMsg == null) {
                break; //Server sent a null message, that means I should disconnect from it
            }
            receivedMsg = receivedMsg.trim();
            printMessage(ChatMessage.parseString(receivedMsg));
        }

        socketChannel.close();

        log.info("Closing connection to the server...");

        try {
            Thread.sleep(73);
        } catch (final InterruptedException ex) {
            log.severe("Couldn't sleep! (" + ex.getMessage() + ")");
            Thread.currentThread().interrupt();
        }

        connectionOver = true;
    }

    // Client Main
    public static void main(final String[] args) {

        if (args.length != 2) {
            log.severe("Usage: chatClient <server ip> <server port>");
            return;
        }

        final String ip = args[0];
        final String port = args[1];

        final ChatClient client = new ChatClient(ip, Integer.parseInt(port));

        try {
            client.run();
        } catch (final IOException e) {
            log.severe(e.getMessage());
        }
    }

}
