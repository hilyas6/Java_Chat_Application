package common;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.LoggerUtil;


/**
 * Represents a member in the chat system, which could be a client or the coordinator.
 * Includes network details and stream references for communication.
 */
public class Member implements Serializable {
	
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerUtil.getLogger();

    private final String id;
    private final int port;
    private final String ipAddress;
    private final transient Socket socket;
    private final transient ObjectInputStream in;
    private final transient ObjectOutputStream out;
    private boolean isCoordinator = false;

    /**
     * Full constructor for a member with live socket and streams.
     */
    public Member(String id, Socket socket, ObjectInputStream in, ObjectOutputStream out) {
    	this.id = id;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.port = socket.getPort();
        this.ipAddress = socket.getInetAddress().getHostAddress();
    }

    /**
     * Copy constructor used for safe client-side representation without socket/streams.
     */
    protected  Member(String id, String ipAddress, int port, boolean isCoordinator) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isCoordinator = isCoordinator;

        this.socket = null;
        this.in = null;
        this.out = null;
    }


    /**
     * Creates a safe, serializable copy of this member for sending to clients.
     */
    public Member copyForClient() {
        return new Member(this.id, this.ipAddress, this.port, this.isCoordinator);
    }

    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIpAddress() {
        return ipAddress;
    }


    /**
     * Sends a message to this member.
     */
    public void send(Message msg) {
        if (out == null) {
            LOGGER.warning("Skipping send to " + id + " (output stream is null)");
            return;
        }

        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send to " + id, e);
        }
    }

    /**
     * Sends a heartbeat message to this member to check if it's still responsive.
     * @return true if the message was sent successfully; false otherwise.
     */
    public boolean ping() {
        if (out == null) {
            LOGGER.warning("Skipping ping to " + id + " (output stream is null)");
            return false;
        }

        try {
            out.writeObject(new Message(MessageType.HEARTBEAT, "server", null));
            out.flush();
            return true;
        } catch (IOException e) {
        	LOGGER.log(Level.FINE, "Ping to " + id + " failed", e);
            return false;
        }
    }

    public ObjectInputStream getInputStream() {
        return in;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setCoordinator(boolean isCoordinator) {
        this.isCoordinator = isCoordinator;
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }
}
