package client;

import common.Message;
import java.util.List;
import java.util.Objects;
import common.MessageType;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.MessageFactory;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.LoggerUtil;

public class ChatGUI implements ClientObserver {
	private static final Logger LOGGER = LoggerUtil.getLogger();

	// User and connection info
    private final String userId;
    private final String serverIp;
    private final int serverPort;
    
    // GUI components
    private TextArea chatArea;
    private ListView<String> memberList;
    private TextArea messageInput;
    private ComboBox<String> recipientSelect;
    private Button sendButton, leaveButton, membersButton, heartbeatButton ;
    
    // Client logic
    private Client client;
    private boolean isConnected = false;

    public ChatGUI(String userId, String serverIp, int serverPort) {
        this.userId = userId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public boolean start(Stage stage) {
        setupUI(stage);

        client = new Client(userId, serverIp, serverPort);
        client.registerObserver(this);
        

        try {
            client.connect();
            isConnected = true;
        } catch (Exception ex) {
            if (ex.getMessage().contains("ID already in use")) {
                return false; // Custom check: user already connected
            }

            Platform.runLater(() -> {
            	// Show alert on failed connection
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Could not connect to server");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            });

            return false;
        }

        chatArea.appendText("Connected as " + userId + "\n");
        stage.show();
        return true;
    }

    private void setupUI(Stage stage) {
    	// Chat display
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefWidth(380);
        
        // Member list on the right
        memberList = new ListView<>();
        memberList.setMinWidth(150);
        memberList.setMaxWidth(210);
        
        // Text input field
        messageInput = new TextArea();
        messageInput.setPromptText("Type your message...");
        messageInput.setWrapText(true);
        messageInput.setPrefRowCount(3);
        messageInput.setMinHeight(60);
        messageInput.setMaxHeight(100);
        
        // Message recipient selection
        recipientSelect = new ComboBox<>();
        recipientSelect.getItems().add("Broadcast");
        recipientSelect.getSelectionModel().selectFirst();
        
        // Main action buttons
        sendButton = new Button("Send");
        leaveButton = new Button("Leave");
        membersButton = new Button("Show Members");
        heartbeatButton = new Button("Active Check");

        // Right side controls stacked vertically
        VBox rightControls = new VBox(10,
        	    new HBox(10, recipientSelect, sendButton, leaveButton),
        	    new HBox(10, membersButton, heartbeatButton)
        	);
        	rightControls.setMinWidth(Region.USE_PREF_SIZE);
        	
            // Bottom message input and controls
        	HBox bottomInput = new HBox(10, messageInput, rightControls);
        	HBox.setHgrow(messageInput, Priority.ALWAYS);
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        bottomInput.setMinHeight(Region.USE_PREF_SIZE);
        bottomInput.setPadding(new Insets(5, 0, 0, 0));

        // Chat pane container
        VBox leftPane = new VBox(chatArea);
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // Center pane includes chat and member list
        HBox centerPane = new HBox(10, leftPane, memberList);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(memberList, Priority.SOMETIMES);
        VBox.setVgrow(centerPane, Priority.ALWAYS);

        // Root container
        VBox root = new VBox(10, centerPane, bottomInput);
        root.setPadding(new Insets(10));
        VBox.setVgrow(centerPane, Priority.ALWAYS);

        // Scene setup
        Scene scene = new Scene(root, 700, 500);
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.setScene(scene);
        stage.setTitle("Distributed Group Chat - " + userId);
        stage.setResizable(true);

        // Handle window close
        stage.setOnCloseRequest(_ -> {
            if (client != null && isConnected) {
                client.leave();
            }
        });

        // Send message logic
        sendButton.setOnAction(_ -> {
            String msg = messageInput.getText().trim();
            String recipient = recipientSelect.getValue();
            if (recipient == null) recipient = "Broadcast";

            if (!msg.isEmpty()) {
                client.sendMessage(msg, recipient);
                messageInput.clear();
            }
        });

        // Send on Enter
        messageInput.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                sendButton.fire();
            }
        });

        // Leave or reconnect logic
        leaveButton.setOnAction(_ -> {
            if (isConnected) {
                client.leave();
                chatArea.appendText("You have left the chat.\n");
                leaveButton.setText("Reconnect");
                isConnected = false;
                toggleControls(false);
            } else {
                try {
                    client = new Client(userId, serverIp, serverPort);
                    client.registerObserver(this);
                    client.connect();

                    memberList.getItems().clear();
                    recipientSelect.getItems().clear();
                    recipientSelect.getItems().add("Broadcast");
                    recipientSelect.getSelectionModel().selectFirst();

                    chatArea.appendText("Reconnected to the chat as " + userId + "\n");
                    leaveButton.setText("Leave");
                    isConnected = true;
                    toggleControls(true);
                } catch (Exception ex) {
                    chatArea.appendText("Failed to reconnect: " + ex.getMessage() + "\n");
                }
            }
        });

        // Coordinator sends member list
        membersButton.setOnAction(_ -> {
            try {
                if (client.isCoordinator()) {
                    // Request directly
                    client.sendRaw(MessageFactory.createMemberListRequest(userId));
                } else {
                    // Ask coordinator to approve
                    client.sendRaw(MessageFactory.createMemberListApprovalRequest(userId));
                    chatArea.appendText("Request sent to coordinator for approval...\n");
                }
            } catch (Exception e) {
                chatArea.appendText("Failed to request member list.\n");
            }
        });

        // Coordinator triggers active check
        heartbeatButton.setOnAction(_ -> {
            try {
                if (client.isCoordinator()) {
                    client.sendRaw(MessageFactory.createManualHeartbeatRequest(userId));
                    chatArea.appendText("Active check sent to all group members. Timer restart (5 minutes interval)\n");
                } else {
                    chatArea.appendText("Only the coordinator can use the Active check.\n");
                }
            } catch (Exception e) {
                chatArea.appendText("Error: Failed to send Active check.\n");
            }
        });
    }

    @Override
    public void onMessageReceived(Message msg) {
        switch (msg.getType()) {
            case HEARTBEAT -> handleHeartbeatPrompt();
            case REQUEST_MEMBER_LIST_APPROVAL -> showApprovalPrompt(msg);
            case MEMBER_LIST_APPROVED -> // Optionally: you could wait for MEMBER_LIST to follow, or trigger something
                    Platform.runLater(() -> chatArea.appendText("Coordinator approved the member list request." +
                            "\n"));
            case JOIN -> {
                client.setCoordinatorId(msg.getCoordinatorId());
                Platform.runLater(() -> chatArea.appendText(msg.getContent() + "\n"));
            }
            case PRIVATE, BROADCAST -> {
                String output = msg.getType() == MessageType.PRIVATE
                        ? "[Private] " + msg.getSenderId() + ": " + msg.getContent()
                        : (msg.getSenderId().equals("server") ? msg.getContent() : msg.getSenderId() + ": " +
                        msg.getContent());
                Platform.runLater(() -> chatArea.appendText(output + "\n"));
            }
            case MEMBER_NAME_LIST -> {
                if (client.isCoordinator()) {
                    List<String> names = msg.getNameList();
                    Platform.runLater(() -> {
                        List<String> labeledNames = names.stream()
                                .map(name -> name.equals(client.getId()) ? name + " [Coordinator]" : name)
                                .toList();

                        memberList.getItems().setAll(labeledNames);

                        // ✅ Update ComboBox with private message targets
                        recipientSelect.getItems().clear();
                        recipientSelect.getItems().add("Broadcast");
                        for (String name : names) {
                            if (!name.equals(userId)) {
                                recipientSelect.getItems().add(name);
                            }
                        }
                        recipientSelect.getSelectionModel().selectFirst();
                    });
                }
            }

            default -> // Safety fallback
            LOGGER.log(Level.WARNING, "Unhandled message type: {0}", msg.getType());
        }
    }

    @Override
    public void onMemberListUpdated(java.util.List<common.Member> members) {
        Platform.runLater(() -> {
            memberList.getItems().clear();
            recipientSelect.getItems().clear();
            recipientSelect.getItems().add("Broadcast");

            chatArea.appendText("\n📋 Member List:\n");

            for (common.Member m : members) {
                String label = m.isCoordinator() ? " [Coordinator]" : "";
                String info = String.format("• %s%s - IP: %s, Port: %d", m.getId(), label, m.getIpAddress(),
                        m.getPort());
                chatArea.appendText(info + "\n");

                memberList.getItems().add(m.getId() + label);
                if (!m.getId().equals(userId)) {
                    recipientSelect.getItems().add(m.getId());
                }
            }

            recipientSelect.getSelectionModel().selectFirst();
            chatArea.appendText("\n");
        });
    }

    private void handleHeartbeatPrompt() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Active Check");
            alert.setHeaderText("Are you still active?");

            Label contentLabel = new Label("The coordinator is checking who’s online.\nRespond in 60 seconds...");
            Label countdownLabel = new Label("60s");
            countdownLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: red;");

            VBox contentBox = new VBox(10, contentLabel, countdownLabel);
            alert.getDialogPane().setContent(contentBox);

            ButtonType yesBtn = new ButtonType("Yes");
            ButtonType noBtn = new ButtonType("No");
            alert.getButtonTypes().setAll(yesBtn, noBtn);
            
            final int[] timeLeft = {60};    
            final Timeline[] timerRef = new Timeline[1];

            KeyFrame frame = new KeyFrame(Duration.seconds(1), _ -> {
                timeLeft[0]--;
                countdownLabel.setText(timeLeft[0] + "s");

                if (timeLeft[0] == 0) {
                    timerRef[0].stop();  // ✅ Access timer via array reference
                    Platform.runLater(() -> {
                        alert.setResult(ButtonType.NO); // simulate "No"
                        alert.hide(); // closes the prompt

                        client.leave();
                        chatArea.appendText("You have been disconnected due to inactivity.\n");
                        leaveButton.setText("Reconnect");
                        isConnected = false;
                        toggleControls(false);
                    });
                }
            });

            Timeline timer = new Timeline(frame);
            timer.setCycleCount(60);
            timer.play();
            timerRef[0] = timer; // ✅ Store actual timer instance

            alert.showAndWait().ifPresent(response -> {
                timer.stop(); // stop countdown if user responds

                if (response == yesBtn) {
                    client.sendRaw(new Message(MessageType.HEARTBEAT, client.getId(), "pong"));
                } else if (response == noBtn) {
                    client.leave();
                    chatArea.appendText("You have left the chat.\n");
                    leaveButton.setText("Reconnect");
                    isConnected = false;
                    toggleControls(false);
                }
            });
        });
    }


    private void showApprovalPrompt(Message msg) {
        Platform.runLater(() -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Approve Member List Request");
            confirm.setHeaderText("Member " + msg.getSenderId() + " wants the member list.");
            confirm.setContentText("Do you approve?");

            ButtonType approve = new ButtonType("Yes");
            ButtonType deny = new ButtonType("No");
            confirm.getButtonTypes().setAll(approve, deny);

            confirm.showAndWait().ifPresent(response -> {
                Message responseMsg = (response == approve)
                        ? new Message(MessageType.MEMBER_LIST_APPROVED, userId, null)
                        : new Message(MessageType.MEMBER_LIST_DENIED, userId, "The request has been denied, " +
                        "try later.");

                responseMsg.setRecipientId(msg.getSenderId());
                client.sendRaw(responseMsg);
            });
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> chatArea.appendText("You were disconnected.\n"));
    }

    private void toggleControls(boolean enable) {
        sendButton.setDisable(!enable);
        messageInput.setDisable(!enable);
        recipientSelect.setDisable(!enable);
        membersButton.setDisable(!enable);
        heartbeatButton.setDisable(!enable);
    }
}
