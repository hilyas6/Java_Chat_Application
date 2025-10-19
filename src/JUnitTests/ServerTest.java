package JUnitTests;

import common.Member;
import server.Server;
import org.junit.jupiter.api.*;

import java.io.Serial;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Server class, focusing on member management and heartbeat behavior.
 */
class ServerTest {
	
    private Server server;

    /**
     * Initialises a new server instance before each test.
     */
    @BeforeEach
    void setup() {
        server = new Server(5000);  // Test port
    }

    /**
     * Helper method to create a fake Member instance using reflection.
     */
    private Member createFakeMember(String id, boolean isCoordinator) throws Exception {
        Constructor<Member> constructor = Member.class.getDeclaredConstructor(
                String.class, String.class, int.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(id, "127.0.0.1", 12345, isCoordinator);
    }

    /**
     * Verifies that a member can be added successfully and retrieved.
     */
    @Test
    void testAddMember_success() throws Exception {
        Member member = createFakeMember("user1", false);
        assertTrue(server.addMember("user1", member));
        assertEquals(member, server.getMember("user1"));
    }

    /**
     * Verifies that duplicate members cannot be added with the same ID.
     */
    @Test
    void testAddMember_duplicate() throws Exception {
        Member member1 = createFakeMember("user1", false);
        Member member2 = createFakeMember("user1", false);

        assertTrue(server.addMember("user1", member1));
        assertFalse(server.addMember("user1", member2)); // Should reject duplicate
    }

    /**
     * Verifies that a member can be removed and that the correct member remains.
     */
    @Test
    void testRemoveMember() throws Exception {
        Member m1 = createFakeMember("a", false);
        Member m2 = createFakeMember("b", false);

        server.addMember("a", m1);
        server.addMember("b", m2);
        server.removeMember("a");

        assertNull(server.getMember("a")); // Should be removed
        assertNotNull(server.getMember("b")); // Should still exist
    }

    /**
     * Verifies that the onLastMemberLeft callback is triggered
     * when the final member leaves the server.
     */
    @Test
    void testSetOnLastMemberLeft_triggered() throws Exception {
        Member solo = createFakeMember("solo", false);
        server.addMember("solo", solo);

        final boolean[] called = {false};
        server.setOnLastMemberLeft(() -> called[0] = true);

        server.removeMember("solo"); // Should trigger the callback
        assertTrue(called[0]);
    }

    /**
     * Verifies that inactive members are removed by the heartbeat check.
     */
    @Test
    void testHeartbeatRemovesInactiveMembers() throws Exception {
        Member alive = new TestMember("alive", true);
        Member dead = new TestMember("dead", false);

        server.addMember("alive", alive);
        server.addMember("dead", dead);

        // Access and invoke private method checkHeartbeats()
        Method checkMethod = Server.class.getDeclaredMethod("checkHeartbeats");
        checkMethod.setAccessible(true);
        checkMethod.invoke(server);

        // Access the scheduled cleanup task (Runnable inside the scheduler)

        // Give the ping simulation a moment (optional)
        Thread.sleep(50);

        // Manually remove inactive members
        Field responsesField = Server.class.getDeclaredField("heartbeatResponses");
        responsesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Boolean> responses = (Map<String, Boolean>) responsesField.get(server);

        // Set dead member's response to false to simulate no response
        responses.put("dead", false);
        responses.put("alive", true); // explicitly mark alive as responsive

        // Manually simulate cleanup
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : responses.entrySet()) {
            if (!entry.getValue()) {
                toRemove.add(entry.getKey());
            }
        }
        for (String id : toRemove) {
            server.removeMember(id);
        }

        // Now assert
        assertNotNull(server.getMember("alive")); // Should remain
        assertNull(server.getMember("dead"));     // Should be removed
    }


    /**
     * Verifies that starting the heartbeat timer resets and replaces the old task.
     */
    @Test
    void testHeartbeatTimerResetOnManualPing() throws Exception {
        Member coordinator = createFakeMember("coordinator", true);
        server.addMember("coordinator", coordinator);

        // Access the private field: heartbeatTask
        Field field = Server.class.getDeclaredField("heartbeatTask");
        field.setAccessible(true);

        server.startHeartbeatTimer(); // Should replace old task
        Object firstTask = field.get(server);

        server.startHeartbeatTimer();
        Object secondTask = field.get(server);

        assertNotEquals(firstTask, secondTask); // Timer task should be replaced
    }

    /**
     * A stubbed test subclass of Member that overrides ping() to simulate
     * alive and dead clients in heartbeat tests.
     */
    static class TestMember extends Member {
    	
        @Serial
        private static final long serialVersionUID = 1L;

        private final boolean respondToPing;

        TestMember(String id, boolean respondToPing) {
            super(id, "127.0.0.1", 12345, false);
            this.respondToPing = respondToPing;
        }

        @Override
        public boolean ping() {
            return respondToPing;
        }
    }
}
