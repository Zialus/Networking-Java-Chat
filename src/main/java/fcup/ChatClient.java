package fcup;

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
  public void printMessage(final C_ChatMessage message) {
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
    socketChannel.write(encoder.encode(CharBuffer.wrap(message+"\n")));
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
      printMessage(C_ChatMessage.parseString(received_msg));
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

    if(args.length < 2) {
      System.out.println("Usage: chatClient <server ip> <server port>");
      return;
    }

    String ip = args[0];
    String port = args[1];

    ChatClient client = new ChatClient(ip, Integer.parseInt(port));
    client.run();
  }

}



enum C_MessageType { OK, ERROR, MESSAGE, NEWNICK, JOINED, LEFT, BYE, PRIVATE, SALA }

class C_ChatMessage {

  private C_MessageType messageType;
  private String messageFirstPart;
  private String messageSecondPart;

  public C_ChatMessage(C_MessageType msg, String msg_part1, String msg_part2) {
    this.messageType = msg;
    this.messageFirstPart = msg_part1;
    this.messageSecondPart = msg_part2;
  }

  public String toString(Boolean prettify) {
    String finalMsg = "";

    switch(this.messageType){
    case OK:
      finalMsg = "Comando aceite!";
      break;
    case ERROR:
      finalMsg = "Erro--> " + this.messageFirstPart + " <--Erro";
      break;
    case MESSAGE:
      finalMsg = this.messageFirstPart + ": " + this.messageSecondPart;
      break;
    case NEWNICK:
      finalMsg = this.messageFirstPart + " mudou o seu nick para " + this.messageSecondPart + "!";
      break;
    case JOINED:
      finalMsg = this.messageFirstPart + " entrou na sala!";
      break;
    case LEFT:
      finalMsg = this.messageFirstPart + " saiu da sala!";
      break;
    case BYE:
      finalMsg = "Carrega 'enter' para sair da aplicação";
      break;
    case SALA:
      finalMsg = this.messageFirstPart + ", está na sala " + this.messageSecondPart;
      break;
    case PRIVATE:
      finalMsg = "(Privado) " + this.messageFirstPart + ": " + this.messageSecondPart;
      break;
    }

    
    finalMsg += "\n";

    return finalMsg;
  }

  public static C_ChatMessage parseString(String unparsedMessage) {
    C_MessageType _messageType = null;
    String _messageFirstPart = "";
    String _messageSecondPart = "";

    String[] msgParts = unparsedMessage.split(" ");

    if (msgParts[0].equals("OK")) {
      _messageType = C_MessageType.OK;
    }
    else if (msgParts[0].equals("ERROR")) {
      _messageType = C_MessageType.ERROR;
      _messageFirstPart = unparsedMessage.substring(6);
    }
    else if (msgParts[0].equals("MESSAGE")) {
      _messageType = C_MessageType.MESSAGE;
      _messageFirstPart = msgParts[1];
      String finalMessage = "";
      for (int i = 2; i < msgParts.length; i ++) {
        if (i > 2) finalMessage += " ";
        finalMessage += msgParts[i];
      }
      _messageSecondPart = finalMessage;
    }
    else if (msgParts[0].equals("NEWNICK")) {
      _messageType = C_MessageType.NEWNICK;
      _messageFirstPart = msgParts[1];
      _messageSecondPart = msgParts[2];
    }
    else if (msgParts[0].equals("JOINED")) {
      _messageType = C_MessageType.JOINED;
      _messageFirstPart = msgParts[1];
    } else if (msgParts[0].equals("LEFT")) {
      _messageType = C_MessageType.LEFT;
      _messageFirstPart = msgParts[1];
    } else if (msgParts[0].equals("BYE")) {
      _messageType = C_MessageType.BYE;
    } else if (msgParts[0].equals("SALA")) {
      _messageType = C_MessageType.SALA;
      _messageFirstPart = msgParts[1];
      _messageSecondPart = msgParts[2];
    } else if (msgParts[0].equals("PRIVATE")) {
      _messageType = C_MessageType.PRIVATE;
      _messageFirstPart = msgParts[1];
      String finalMessage = "";
      for (int i = 2; i < msgParts.length; i ++) {
        if (i > 2) finalMessage += " ";
        finalMessage += msgParts[i];
      }
      _messageSecondPart = finalMessage;
    }

    return (new C_ChatMessage(_messageType, _messageFirstPart, _messageSecondPart));
  }

}
