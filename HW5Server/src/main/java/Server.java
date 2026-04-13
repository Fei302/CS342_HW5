import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>(); // List of ClientThreads of which each handle their own client
	HashMap<String, ClientThread> usernames = new HashMap<>();	// Maps username -> their ClientThread so we can find them for direct messages
	TheServer server; // The main server that is listening for Clients and assigning them their own Client Thread
	private Consumer<Serializable> callback; // Display of all messages

	Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	// Send a Message to every connected client
	public void broadcastMessage(Message msg) {
		for (ClientThread t : clients) {
			try {
				t.out.writeObject(msg);
			} catch (Exception e) {}
		}
	}

	// Send the current user list to every client (called after someone joins or leaves)
	public void broadcastUserList() {
		Message msg = new Message(Message.MessageType.USER_LIST);
		msg.userList = new ArrayList<>(usernames.keySet());
		broadcastMessage(msg);
	}

	// Start of TheServer class
	public class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				System.out.println("Server is waiting for a client!");
				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("Client #" + count + " has connected (not yet registered).");
					clients.add(c);
					c.start();
					count++;
				}
			} catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}
	}
	// End of TheServer class

	// Start of ClientThread class
	class ClientThread extends Thread {

		Socket connection;
		int count;
		String username; // null until they successfully register
		ObjectInputStream in;
		ObjectOutputStream out;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		public void run() {
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
			}

			while (true) {
				try {
					Message data = (Message) in.readObject();

					switch (data.type) {

						case REGISTER:
							String desired = data.sender;
							if (usernames.containsKey(desired)) {
								// Username already taken - send error back to just this client
								Message error = new Message(Message.MessageType.ERROR);
								error.messageToBeSent = "Username \"" + desired + "\" is already taken. Please choose another.";
								out.writeObject(error);
								callback.accept("Client #" + count + " tried username \"" + desired + "\" - already taken.");
							} else {
								// Accept the username
								this.username = desired;
								usernames.put(desired, this);
								callback.accept(desired + " has joined the server.");
								// Broadcast updated user list to everyone
								broadcastUserList();
							}
							break;

						case SEND_MESSAGE_TO_ALL_CLIENTS:
							callback.accept(data.sender + " to all: " + data.messageToBeSent);
							broadcastMessage(data);
							break;

						case SEND_MESSAGE_TO_ONE_CLIENT:
							callback.accept(data.sender + " to " + data.receiver + ": " + data.messageToBeSent);
							ClientThread target = usernames.get(data.receiver);
							if (target != null) {
								target.out.writeObject(data); // send to recipient
								out.writeObject(data);        // also echo back to sender so they see it
							} else {
								Message error = new Message(Message.MessageType.ERROR);
								error.messageToBeSent = "User \"" + data.receiver + "\" was not found.";
								out.writeObject(error);
							}
							break;

						default:
							break;
					}

				} catch (Exception e) {
					// Client disconnected
					String name = (username != null) ? username : "Client #" + count;
					callback.accept(name + " has disconnected.");
					if (username != null) {
						usernames.remove(username);
						broadcastUserList(); // tell everyone this user left
					}
					clients.remove(this);
					break;
				}
			}
		}
	}
	// End of ClientThread class

}
