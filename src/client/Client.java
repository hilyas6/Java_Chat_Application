package client;

import common.*;
import java.io.*;
import java.net.*;
import java.util.List;
import utils.LoggerUtil;
import utils.MessageFactory;
import utils.ObservableSubject;
import java.util.logging.Logger;
import java.util.logging.Level;

//The Client class represents a networked client that connects to a server
//and communicates using serialised Message objects.
public class Client extends ObservableSubject<ClientObserver> {
	private static final Logger LOGGER = LoggerUtil.getLogger();
    private final String id;
    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private static String coordinatorId;
    private boolean isCoordinator = false;
    private boolean connected = false;

    // Constructor to initialise client ID, server IP and port
    public Client(String id, String serverIp, int serverPort) {
        this.id = id;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    // Connects to the server and starts the listening thread
    public void connect() throws IOException, ClassNotFoundException {
        socket = new Socket(serverIp, serverPort);
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
        	LOGGER.log(Level.SEVERE, "Stream setup failed", e);
            throw e;
        }

        // Send join message to server
        Message joinMsg = MessageFactory.createJoinMessage(id, "");
        out.writeObject(joinMsg);
        out.flush();

        // Receive first message from server (could be JOIN or ERROR)
        Message first = (Message) in.readObject();

        if (first.getType() == MessageType.ERROR) {
            throw new IOException(first.getContent());
        }

        if (first.getType() == MessageType.JOIN) {
            if (first.isCoordinator()) {
                isCoordinator = true;
            }
            notifyMessage(first);
        }

        // Start a background thread to continuously listen for messages
        Thread listenerThread = new Thread(this::listen);
        listenerThread.start();
        connected = true;
    }

    // Listener method that runs in a separate thread to receive server messages
    private void listen() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();

                switch (msg.getType()) {
                    case HEARTBEAT:
                    case BROADCAST:
                    case PRIVATE:
                    case REQUEST_MEMBER_LIST_APPROVAL:
                    case MEMBER_LIST_DENIED:
                    case MEMBER_NAME_LIST:
                        notifyMessage(msg);
                        break;
                    case MEMBER_LIST:
                        notifyMemberList(msg.getMemberList());
                        break;
                    case JOIN:
                        isCoordinator = msg.isCoordinator();
                        notifyMessage(msg);
                        break;
                    case LEAVE:
                        break;
                    default:
                    	LOGGER.log(Level.WARNING, "Unhandled message type received: {0}", msg.getType());
                        break;
                }
            }
        } catch (Exception e) {
            notifyDisconnect();
        }
    }


    // Sends either a broadcast or private message to the server
    public void sendMessage(String message, String recipientId) {
        try {
            Message msg = "Broadcast".equals(recipientId)
                    ? MessageFactory.createBroadcastMessage(id, message)
                    : MessageFactory.createPrivateMessage(id, recipientId, message);
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
        	LOGGER.log(Level.WARNING, "Failed to send message", e);
        }
    }

    // Sends a LEAVE message to the server and closes the connection
    public void leave() {
    	
        if (out == null || socket == null || socket.isClosed()) {
            return;
        }

        try {
            out.writeObject(MessageFactory.createLeaveMessage(id));
            socket.close();
        } catch (Exception e) {
        	LOGGER.log(Level.SEVERE, "Couldn't receive LEAVE_ACK", e);
        }
        connected = false;
    }

    // Sends a raw (pre-constructed) message to the server
    public void sendRaw(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send raw message", e);
        }
    }

    // Notifies observers with a received message
    private void notifyMessage(Message msg) {
        notifyObservers(o -> o.onMessageReceived(msg));
    }

    // Notifies observers with an updated member list
    private void notifyMemberList(List<Member> members) {
        notifyObservers(o -> o.onMemberListUpdated(members));
    }

    // Notifies observers that the client is disconnected
    private void notifyDisconnect() {
        notifyObservers(ClientObserver::onDisconnected);
    }

    // Getters and setters
    public boolean isCoordinator() {
        return isCoordinator;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getId() {
        return id;
    }

    public void setCoordinatorId(String id) {
        coordinatorId = id;
    }

    public String getCoordinatorId() {
        return coordinatorId;
    }
}
