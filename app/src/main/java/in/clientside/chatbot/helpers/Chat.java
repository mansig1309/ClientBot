package in.clientside.chatbot.helpers;

public class Chat {
    private String message, sender;

    public Chat(String message, String sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }
}
