import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        REGISTER,
        SEND_MESSAGE_TO_ALL_CLIENTS,
        SEND_MESSAGE_TO_ONE_CLIENT
    }

    // MessageToBeSent - meant only for the send messages action and register
    // Sender - Meant for send messages actions, and register
    // Receiver - meant messages actions
    String messageToBeSent, sender, receiver;
}
