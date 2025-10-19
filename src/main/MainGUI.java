package main;

import client.ChatGUI;
import java.util.Objects;
import utils.NameValidator;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.application.Application;


/**
 * Main entry point for the JavaFX-based client setup window.
 * Allows users to input their ID and connect to the chat server.
 */
public class MainGUI extends Application {
    private static final Logger LOGGER = Logger.getLogger(MainGUI.class.getName());

    /**
     * Initialises the primary JavaFX stage (the setup window).
     * Includes input fields for ID, server IP, and port, and handles user interaction.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Client Setup");

        // Input fields
        TextField idField = new TextField();
        idField.setPromptText("Your ID");

        TextField ipField = new TextField("127.0.0.1");
        TextField portField = new TextField("5000");

        Button connectButton = new Button("Connect");

        // Layout setup
        VBox root = new VBox(10,
                new Label("Enter Your ID:"), idField,
                new Label("Server IP Address:"), ipField,
                new Label("Server Port:"), portField,
                connectButton
        );
        root.setPadding(new Insets(20));

        // Handle Connect button click
        connectButton.setOnAction(_ -> connectToServer(idField, ipField, portField, primaryStage));

        // Add key event filter to handle Enter key press for the idField, ipField, and portField
        idField.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                connectButton.fire();
            }
        });

        ipField.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                connectButton.fire();
            }
        });

        portField.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                connectButton.fire();
            }
        });

        primaryStage.setScene(new Scene(root, 350, 250));
        primaryStage.show();
    }

    /**
     * Validates input, attempts to connect to the server, and launches the chat UI.
     *
     * @param idField        The input field for user ID.
     * @param ipField        The input field for server IP address.
     * @param portField      The input field for server port.
     * @param primaryStage   The current setup window.
     */
    private void connectToServer(TextField idField, TextField ipField, TextField portField, Stage primaryStage) {
        String id = idField.getText().trim();
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        // Basic validation
        if (id.isEmpty() || ip.isEmpty() || portText.isEmpty()) {
            showAlert("All fields are required.");
            return;
        }
        
        // Name validation
        if (!NameValidator.isValidName(id)) {
            showAlert("Invalid ID. Use letters only, with optional _ or -.");
            idField.requestFocus();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            showAlert("Port must be a number.");
            return;
        }

        try {
            ChatGUI chat = new ChatGUI(id, ip, port);
            Stage chatStage = new Stage();
            boolean success = chat.start(chatStage);

            if (success) {
                primaryStage.close(); // Only close if connection worked
            } else {
                showAlert("ID already in use. Please choose another."); // Let them retry
                idField.clear();
                idField.requestFocus(); // Refocus the ID input
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to start chat", ex);
            showAlert("Failed to start chat: " + ex.getMessage());
        }
    }


    /**
     * Displays a warning alert dialog with the given message.
     *
     * @param msg The warning message to display.
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Main entry point for launching the JavaFX application.
     */
    public static void main(String[] args) {launch(args);}
}
