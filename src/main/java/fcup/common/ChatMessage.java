package fcup.common;

public final class ChatMessage {
    private final MessageType messageType;
    private final String messageFirstPart;
    private final String messageSecondPart;

    public ChatMessage(final MessageType messageType, final String messageFirstPart, final String messageSecondPart) {
        this.messageType = messageType;
        this.messageFirstPart = messageFirstPart;
        this.messageSecondPart = messageSecondPart;
    }

    public String toString(final boolean prettify) {
        String finalMsg;

        if (prettify) {
            switch (this.messageType) {
                case OK:
                    finalMsg = "Comando aceite!";
                    break;
                case ERROR:
                    finalMsg = "Erro ao processar o comando!";
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
                case PRIVATE:
                    finalMsg = "(Privado) " + this.messageFirstPart + ": " + this.messageSecondPart;
                    break;
                case SALA:
                    finalMsg = "Olá " + this.messageFirstPart + " estás na sala: " + this.messageSecondPart;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.messageType);
            }
        } else {
            switch (this.messageType) {
                case OK:
                    finalMsg = "OK";
                    break;
                case ERROR:
                    finalMsg = "ERROR " + this.messageFirstPart;
                    break;
                case MESSAGE:
                    finalMsg = "MESSAGE " + this.messageFirstPart + " " + this.messageSecondPart;
                    break;
                case NEWNICK:
                    finalMsg = "NEWNICK " + this.messageFirstPart + " " + this.messageSecondPart;
                    break;
                case JOINED:
                    finalMsg = "JOINED " + this.messageFirstPart;
                    break;
                case LEFT:
                    finalMsg = "LEFT " + this.messageFirstPart;
                    break;
                case BYE:
                    finalMsg = "BYE";
                    break;
                case SALA:
                    finalMsg = "SALA " + this.messageFirstPart + " " + this.messageSecondPart;
                    break;
                case PRIVATE:
                    finalMsg = "PRIVATE " + this.messageFirstPart + " " + this.messageSecondPart;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.messageType);
            }
        }

        finalMsg += "\n";

        return finalMsg;
    }

    public static ChatMessage parseString(final String unparsedMessage) {
        final String[] msgParts = unparsedMessage.split(" ");

        final String action = msgParts[0];

        final MessageType messageType = MessageType.valueOf(action);
        String messageFirstPart = "";
        String messageSecondPart = "";

        switch (messageType) {
            case OK:
            case BYE:
                break;
            case ERROR:
                messageFirstPart = unparsedMessage.substring(6);
                break;
            case MESSAGE:
            case PRIVATE:
                messageFirstPart = msgParts[1];
                messageSecondPart = createSecondPart(msgParts);
                break;
            case NEWNICK:
            case SALA:
                messageFirstPart = msgParts[1];
                messageSecondPart = msgParts[2];
                break;
            case JOINED:
            case LEFT:
                messageFirstPart = msgParts[1];
                break;
        }

        return (new ChatMessage(messageType, messageFirstPart, messageSecondPart));
    }

    private static String createSecondPart(final String[] msgParts) {
        final StringBuilder finalMessage = new StringBuilder();

        for (int i = 2; i < msgParts.length; i++) {
            if (i > 2) {
                finalMessage.append(" ");
            }
            finalMessage.append(msgParts[i]);
        }

        return finalMessage.toString();
    }

}
