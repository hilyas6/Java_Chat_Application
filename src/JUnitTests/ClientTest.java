package JUnitTests;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;
import client.Client;
import client.ClientObserver;
import org.junit.jupiter.api.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Client class, using a mock server via ServerSocket to simulate server behavior.
 */
class ClientTest {
	
    private ServerSocket mockServer;
    private Client client;
    private List<Message> receivedMessages;
    private boolean disconnectedCalled;
    
    private static final Logger LOGGER = Logger.getLogger(ClientTest.class.getName());

    /**
     * Setup runs before each test.
     * Creates a mock server socket, starts a server thread to simulate message exchange,
     * and connects a test client with a custom ClientObserver.
     */
    @BeforeEach
    void setup() throws IOException {
        mockServer = new ServerSocket(0); // Bind to a free port
        int port = mockServer.getLocalPort();

        receivedMessages = new ArrayList<>();
        disconnectedCalled = false;

        client = new Client("testUser", "localhost", port);
        
        // Register a test observer that tracks received messages and disconnect events
        client.registerObserver(new ClientObserver() {
            @Override
            public void onMessageReceived(Message msg) {
                receivedMessages.add(msg);

                if (msg.getType() == MessageType.JOIN) {
                    client.setCoordinatorId(msg.getCoordinatorId());
                }
            }

            @Override
            public void onMemberListUpdated(List<Member> members) {
                LOGGER.info("Member list updated: " + members.size() + " members.");
            }

            @Override
            public void onDisconnected() {
                disconnectedCalled = true;
            }
        });

        // Start mock server thread to simulate server behavior
        new Thread(() -> {
            try (Socket socket = mockServer.accept();
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                // Receive JOIN message
                Message join = (Message) in.readObject();
                assertEquals(MessageType.JOIN, join.getType());

                // Send JOIN response (marking client as coordinator)
                Message response = new Message(MessageType.JOIN, "server", "Welcome");
                response.setCoordinator(true);
                response.setCoordinatorId("testUser");
                out.writeObject(response);
                out.flush();

                // Simulate a BROADCAST message
                Thread.sleep(100);
                out.writeObject(new Message(MessageType.BROADCAST, "server", "Hello world"));
                out.flush();

                Thread.sleep(100); // Give time for the client to read the message

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in mock server thread", e);
            }
        }).start();

        // Connect client and wait for interaction
        try {
            client.connect();
            Thread.sleep(300);
        } catch (Exception e) {
            fail("Connection should succeed: " + e.getMessage());
        }
    }

    /**
     * Clean up the mock server after each test.
     */
    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null && !mockServer.isClosed()) {
            mockServer.close();
        }
    }


    /**
     * Verifies that the client receives both JOIN and BROADCAST messages upon connection.
     */
    @Test
    void testClientReceivesJoinAndBroadcast() {
        assertTrue(client.isConnected());
        assertTrue(client.isCoordinator());
        assertEquals("testUser", client.getId());
        assertEquals("testUser", client.getCoordinatorId());

        assertEquals(2, receivedMessages.size(), "Should have received JOIN and BROADCAST");

        assertEquals(MessageType.JOIN, receivedMessages.get(0).getType());
        assertEquals(MessageType.BROADCAST, receivedMessages.get(1).getType());
        assertEquals("Hello world", receivedMessages.get(1).getContent());
    }

    /**
     * Verifies that the client triggers the onDisconnected callback if server goes silent.
     */
    @Test
    void testClientHandlesDisconnect() throws InterruptedException {
        Thread.sleep(500);
        assertTrue(disconnectedCalled, "Client should have triggered onDisconnected");
    }

    /**
     * Verifies that the client correctly sends a private message to the server.
     */
    @Test
    void testSendPrivateMessage() throws Exception {
        ServerSocket privateMessageServer = new ServerSocket(0);
        int testPort = privateMessageServer.getLocalPort();
        List<Message> messagesReceived = new ArrayList<>();

        Client testClient = new Client("privateUser", "localhost", testPort);

        // Start mock server to handle private message
        new Thread(() -> {
            try (
                Socket socket = privateMessageServer.accept();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ) {
                Message join = (Message) in.readObject(); // Receive JOIN
                assertEquals(MessageType.JOIN, join.getType());

                // Respond to JOIN
                Message joinResponse = new Message(MessageType.JOIN, "server", "Welcome");
                out.writeObject(joinResponse);
                out.flush();

                // Receive PRIVATE message
                Message privateMsg = (Message) in.readObject();
                messagesReceived.add(privateMsg);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in private message test server", e);
            }
        }).start();

        testClient.connect();
        Thread.sleep(100);

        testClient.sendMessage("Secret", "targetUser");
        Thread.sleep(200);

        assertEquals(1, messagesReceived.size(), "Expected one private message to be received");

        Message received = messagesReceived.get(0);
        assertEquals(MessageType.PRIVATE, received.getType());
        assertEquals("Secret", received.getContent());
        assertEquals("targetUser", received.getRecipientId());
        assertEquals("privateUser", received.getSenderId());

        testClient.leave();
        privateMessageServer.close();
    }
    
    
    /**
     * Verifies that the client can send a raw message (manual heartbeat).
     */
    @Test
    void testSendRawMessage() {
        Message msg = new Message(MessageType.HEARTBEAT, "testUser", "ping");
        client.sendRaw(msg);
    }

    /**
     * Verifies that the client's leave method properly closes the connection.
     */
    @Test
    void testLeaveClosesConnection() {
        client.leave();
        assertFalse(client.isConnected());
    }

    /**
     * Tests setting the coordinator flag and ID via a JOIN message.
     */
    @Test
    void testCoordinatorFlag() {
        Message join = new Message(MessageType.JOIN, "server", "Welcome");
        join.setCoordinator(true);
        join.setCoordinatorId("testUser");

        receivedMessages.clear();
        client.registerObserver(new ClientObserver() {
            @Override
            public void onMessageReceived(Message msg) {
                receivedMessages.add(msg);
                if (msg.getType() == MessageType.JOIN) {
                    client.setCoordinatorId(msg.getCoordinatorId());
                }
            }
            public void onMemberListUpdated(List<Member> members) {}
            public void onDisconnected() {}
        });
        client.sendRaw(join);
    }
}
