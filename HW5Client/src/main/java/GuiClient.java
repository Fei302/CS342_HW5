import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.HashMap;

public class GuiClient extends Application {

	// Login scene controls
	TextField usernameField;
	Button connectButton;
	Label loginErrorLabel;

	// Main chat scene controls
	TextField messageField;
	Button sendButton;
	ListView<String> messageList;
	ListView<String> userList;
	ComboBox<String> recipientCombo;

	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	Stage primaryStage;

	// Track registration state
	String pendingUsername = null;  // what we tried to register as
	String myUsername = null;       // set only after server confirms success
	boolean registered = false;

	ObservableList<String> connectedUsers = FXCollections.observableArrayList();

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;

		clientConnection = new Client(data -> {
			Platform.runLater(() -> handleIncomingMessage((Message) data));
		});
		clientConnection.start();

		// Build the controllers that are shared across scenes
		messageList = new ListView<>();
		userList = new ListView<>(connectedUsers);
		recipientCombo = new ComboBox<>();
		recipientCombo.getItems().add("All");
		recipientCombo.setValue("All");

		sceneMap = new HashMap<>();
		sceneMap.put("login", createLoginScene());
		sceneMap.put("client", createClientGui());

		// Handle closing the GUI without error
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		// Start on the login scene
		primaryStage.setScene(sceneMap.get("login"));
		primaryStage.setTitle("Messaging App - Login");
		primaryStage.show();
	}

	// Central handler for all incoming messages from the server
	private void handleIncomingMessage(Message msg) {
		switch (msg.type) {

			case ERROR:
				if (!registered) {
					// Registration failed - show error and let them try again
					loginErrorLabel.setText(msg.messageToBeSent);
					pendingUsername = null;
					primaryStage.setScene(sceneMap.get("login"));
				} else {
					// Some other error during chat (e.g. user not found for DM)
					messageList.getItems().add("[Error] " + msg.messageToBeSent);
				}
				break;

			case USER_LIST:
				// Update the online users list and the recipient dropdown
				connectedUsers.clear();
				recipientCombo.getItems().clear();
				recipientCombo.getItems().add("All");
				for (String user : msg.userList) {
					if (!user.equals(myUsername)) {
						connectedUsers.add(user);
						recipientCombo.getItems().add(user);
					}
				}
				recipientCombo.setValue("All");

				// If we were waiting for registration confirmation, switch to main scene now
				if (!registered && pendingUsername != null) {
					myUsername = pendingUsername;
					pendingUsername = null;
					registered = true;
					primaryStage.setScene(sceneMap.get("client"));
					primaryStage.setTitle("Messaging App - " + myUsername);
				}
				break;

			case SEND_MESSAGE_TO_ALL_CLIENTS:
				messageList.getItems().add("[All] " + msg.sender + ": " + msg.messageToBeSent);
				break;

			case SEND_MESSAGE_TO_ONE_CLIENT:
				messageList.getItems().add("[DM] " + msg.sender + " -> " + msg.receiver + ": " + msg.messageToBeSent);
				break;

			default:
				messageList.getItems().add(msg.messageToBeSent);
				break;
		}
	}

	public Scene createLoginScene() {
		Label prompt = new Label("Enter a username to join:");
		usernameField = new TextField();
		usernameField.setPromptText("Username...");

		loginErrorLabel = new Label("");
		loginErrorLabel.setStyle("-fx-text-fill: red;");

		connectButton = new Button("Join Server");
		connectButton.setOnAction(e -> {
			String desired = usernameField.getText().trim();
			if (!desired.isEmpty()) {
				pendingUsername = desired;
				loginErrorLabel.setText(""); // clear any previous error
				Message msg = new Message(Message.MessageType.REGISTER);
				msg.sender = desired;
				clientConnection.send(msg);
			}
		});

		VBox loginBox = new VBox(10, prompt, usernameField, connectButton, loginErrorLabel);
		loginBox.setPadding(new Insets(20));
		loginBox.setStyle("-fx-background-color: blue; -fx-font-family: 'serif';");
		return new Scene(loginBox, 400, 200);
	}

	public Scene createClientGui() {
		messageField = new TextField();
		messageField.setPromptText("Type a message...");

		sendButton = new Button("Send");
		sendButton.setOnAction(e -> {
			String text = messageField.getText().trim();
			if (!text.isEmpty()) {
				String recipient = recipientCombo.getValue();
				Message msg;
				if (recipient.equals("All")) {
					msg = new Message(Message.MessageType.SEND_MESSAGE_TO_ALL_CLIENTS);
				} else {
					msg = new Message(Message.MessageType.SEND_MESSAGE_TO_ONE_CLIENT);
					msg.receiver = recipient;
				}
				msg.sender = myUsername;
				msg.messageToBeSent = text;
				clientConnection.send(msg);
				messageField.clear();
			}
		});

		// Bottom input row = [To: dropdown] [message field] [Send]
		HBox inputRow = new HBox(10,
				new Label("To:"), recipientCombo, messageField, sendButton);
		HBox.setHgrow(messageField, Priority.ALWAYS);
		inputRow.setPadding(new Insets(8));

		// Right panel: list of online users
		VBox usersPanel = new VBox(5, new Label("Online Users:"), userList);
		usersPanel.setPrefWidth(130);
		usersPanel.setPadding(new Insets(5));

		BorderPane layout = new BorderPane();
		layout.setCenter(messageList);
		layout.setRight(usersPanel);
		layout.setBottom(inputRow);
		layout.setStyle("-fx-background-color: blue; -fx-font-family: 'serif';");
		layout.setPadding(new Insets(10));

		return new Scene(layout, 650, 420);
	}
}
