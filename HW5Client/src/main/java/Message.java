import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        REGISTER,                   // Client -> Server: "I want this username"
        ERROR,                      // Server -> Client: "Something went wrong"
        USER_LIST,                  // Server -> All Clients: updated list of connected users
        SEND_MESSAGE_TO_ALL_CLIENTS,
        SEND_MESSAGE_TO_ONE_CLIENT
    }

    MessageType type;               // What kind of message is this?
    String messageToBeSent;         // The actual text content
    String sender;                  // Who sent it
    String receiver;                // Who it's going to (only for SEND_MESSAGE_TO_ONE_CLIENT)
    ArrayList<String> userList;     // Only used for USER_LIST type

    Message(MessageType type) {
        this.type = type;
    }
}
