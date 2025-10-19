package main;

import server.Server;
import utils.LoggerUtil;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.io.IOException;
import java.net.ServerSocket;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.application.Application;


/**
 * Entry point for launching the chat server with a simple JavaFX UI.
 */
public class MainServer extends Application {
	private static final Logger LOGGER = LoggerUtil.getLogger();

	
    private static final int DEFAULT_PORT = 5000;

    /**
     * Initialises the JavaFX UI for configuring and starting the server.
     */
    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Start Server");

        TextField portField = new TextField(String.valueOf(DEFAULT_PORT));
        portField.setPromptText("Enter server port");

        Button startButton = new Button("Start Server");

        VBox root = new VBox(10,
                new Label("Server Port (1024-65535):"), portField,
                startButton
        );
        root.setPadding(new Insets(20));

        // Bind the "Start Server" button
        startButton.setOnAction(_ -> startServer(portField));

        // Allow pressing Enter to trigger the start action
        portField.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                startButton.fire();
            }
        });

        primaryStage.setScene(new Scene(root, 350, 150));
        primaryStage.show();
    }

    /**
     * Handles input validation and starts the server in a background thread.
     *
     * @param portField The input field containing the desired server port.
     */
    private void startServer(TextField portField) {
        String input = portField.getText().trim();
        int port;

        if (input.isEmpty()) {
            port = DEFAULT_PORT;
        } else {
            try {
                port = Integer.parseInt(input);
                if (port < 1024 || port > 65535) {
                    showAlert("Port must be between 1024 and 65535.");
                    return;
                }

                // Check port availability
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    serverSocket.getLocalPort();
                } catch (IOException ex) {
                    showAlert("Port is already in use. Please choose another.");
                    return;
                }

            } catch (NumberFormatException ex) {
                showAlert("Invalid input. Please enter a numeric port.");
                return;
            }
        }

        // Inform the user the server is starting
        showInfo("🚀 Starting server on port: " + port);

        // Run the server on a background thread
        new Thread(() -> {
        	try {
                Server server = new Server(port);

                // Define callback for when all clients disconnect
                server.setOnLastMemberLeft(() -> Platform.runLater(() -> {

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to close the server?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Last Client Disconnected");
                    confirm.setHeaderText("Server is now empty");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                        	LOGGER.info("Server continues to run.");
                            System.exit(0);
                        } else {
                            LOGGER.info("Server continues to run.");
                        }
                    });
                }));

                server.start();  // Starts listening and handling clients

            } catch (IOException ex) {
                Platform.runLater(() -> showAlert("Failed to start server: " + ex.getMessage()));
            }
        }).start();

        // Keep JavaFX app alive even if window is closed
        Platform.setImplicitExit(false);
        Stage stage = (Stage) portField.getScene().getWindow();
        stage.close(); // Close setup window
    }

    /**
     * Displays an error alert with a given message.
     *
     * @param msg The message to display.
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    /**
     * Displays an information alert with a given message.
     *
     * @param msg The message to display.
     */
    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Server Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Launches the JavaFX application.
     */
    public static void main(String[] args) {launch(args);}
}
