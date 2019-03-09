package fcup.Client;

import fcup.common.ChatMessage;

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


public class ChatClient {

    // GUI vars
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();

    // Socket vars
    private SocketChannel socketChannel;
    private BufferedReader reader;
    private Boolean connectionOver = false;

    // Decoder/Encoder for text transmission
    private final Charset charset = Charset.forName("UTF8");
    private final CharsetEncoder encoder = charset.newEncoder();
    private final CharsetDecoder decoder = charset.newDecoder();

    // GUI function to print message
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    // Message printer (to chat)
    public void printMessage(final ChatMessage message) {
        printMessage(message.toString(true));
    }

    // Initializer: GUI and Server Connection
    public ChatClient(String server, int port) throws IOException {

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
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                    System.out.println("There was an error sending a message! (" + ex.getMessage() + ")");
                } finally {
                    chatBox.setText("");
                }

                if (connectionOver)
                    System.exit(0);

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

    // Mensage sender - send the message to the server
    public void newMessage(String message) throws IOException {
        socketChannel.write(encoder.encode(CharBuffer.wrap(message + "\n")));
    }

    // Listener of server messages
    public void run() throws IOException {

        try {
            while (!socketChannel.finishConnect())
                ;
        } catch (Exception ex) {
            System.out.println("Ocorreu um error ao ligar ao servidor! (" + ex.getMessage() + ")");
            System.exit(0);
            return;
        }

        reader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream()));

        while (true) {
            String received_msg = reader.readLine();
            if (received_msg == null)
                break;
            received_msg = received_msg.trim();
            printMessage(ChatMessage.parseString(received_msg));
        }

        socketChannel.close();

        try {
            Thread.sleep(73);
        } catch (InterruptedException ex) {
            System.out.println("Ocorreu um error ao ligar ao servidor! (" + ex.getMessage() + ")");
            System.exit(0);
            return;
        }

        connectionOver = true;
    }

    // Client Main
    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: chatClient <server ip> <server port>");
            return;
        }

        String ip = args[0];
        String port = args[1];

        ChatClient client = new ChatClient(ip, Integer.parseInt(port));
        client.run();
    }

}
