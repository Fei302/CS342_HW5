import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        REGISTER,
        CREATE_GROUP,
        SEND_MESSAGE_TO_ALL_CLIENTS,
        SEND_GROUP_MESSAGE,
        SEND_MESSAGE_TO_ONE_CLIENT
    }


    // Note: receiver username is not always applicable as message to all clients does not need it.
    // The only one that definitely needs it is messaging to one client
    // The message to group is iffy as I do not know if the group is named
    // Upon further look at the instructions, I am even more confused.
    // Instructions say: Send a message either to all users or to an individual user.
    // They never mention sending to a group
    // However, they say "which will allow Clients to create groups, send messages to all members of a group"
    // So, it doesn't line up.

    // MessageToBeSent - meant only for the send messages action and register
    // Sender - Meant for send messages actions, create group, and register
    // Receiver - meant only for create group and messages actions
    String messageToBeSent, sender, receiver;

    // Array list to handle all the usernames that will be grouped into a group. Meant to only be used for CREATE_GROUP
    ArrayList<String> usernamesToBeGrouped;
}
