package fcup.common;

public class ChatMessage {
    private MessageType messageType;
    private String messageFirstPart;
    private String messageSecondPart;

    public ChatMessage(MessageType _messageType, String _messageFirstPart, String _messageSecondPart) {
        this.messageType = _messageType;
        this.messageFirstPart = _messageFirstPart;
        this.messageSecondPart = _messageSecondPart;
    }

    public String toString(Boolean prettify) {
        String finalMsg = "";

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
            }
        }

        finalMsg += "\n";

        return finalMsg;
    }

    public static ChatMessage parseString(String unparsedMessage) {
        MessageType _messageType = null;
        String _messageFirstPart = "";
        String _messageSecondPart = "";

        String[] msgParts = unparsedMessage.split(" ");

        switch (msgParts[0]) {
            case "OK":
                _messageType = MessageType.OK;
                break;
            case "ERROR":
                _messageType = MessageType.ERROR;
                _messageFirstPart = unparsedMessage.substring(6);
                break;
            case "MESSAGE": {
                _messageType = MessageType.MESSAGE;
                _messageFirstPart = msgParts[1];
                _messageSecondPart = createSecondParte(msgParts);
                break;
            }
            case "NEWNICK":
                _messageType = MessageType.NEWNICK;
                _messageFirstPart = msgParts[1];
                _messageSecondPart = msgParts[2];
                break;
            case "JOINED":
                _messageType = MessageType.JOINED;
                _messageFirstPart = msgParts[1];
                break;
            case "LEFT":
                _messageType = MessageType.LEFT;
                _messageFirstPart = msgParts[1];
                break;
            case "BYE":
                _messageType = MessageType.BYE;
                break;
            case "SALA":
                _messageType = MessageType.SALA;
                _messageFirstPart = msgParts[1];
                _messageSecondPart = msgParts[2];
                break;
            case "PRIVATE": {
                _messageType = MessageType.PRIVATE;
                _messageFirstPart = msgParts[1];
                _messageSecondPart = createSecondParte(msgParts);
                break;
            }
        }

        return (new ChatMessage(_messageType, _messageFirstPart, _messageSecondPart));
    }

    private static String createSecondParte(String[] msgParts) {
        String _messageSecondPart;
        String finalMessage = "";
        for (int i = 2; i < msgParts.length; i++) {
            if (i > 2) finalMessage += " ";
            finalMessage += msgParts[i];
        }
        _messageSecondPart = finalMessage;
        return _messageSecondPart;
    }

}
