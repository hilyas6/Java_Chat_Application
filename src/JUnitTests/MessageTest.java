package JUnitTests;

import common.Member;
import common.Message;
import java.util.List;
import common.MessageType;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Message class to validate constructors, accessors, and edge-case behavior.
 */
class MessageTest {
	
    /**
     * Tests message creation with a String payload.
     * Ensures content is stored and memberList is null.
     */
    @Test
    void testConstructorWithStringPayload() {
        Message msg = new Message(MessageType.BROADCAST, "alice", "Hello World");
        
        assertEquals(MessageType.BROADCAST, msg.getType());
        assertEquals("alice", msg.getSenderId());
        assertEquals("Hello World", msg.getContent());
        assertNull(msg.getMemberList());
    }

    /**
     * Tests message creation with an empty List<Member> payload.
     * Ensures the memberList is initialized but empty.
     */
    @Test
    void testConstructorWithEmptyMemberList() {
        List<Member> emptyList = new ArrayList<>();
        Message msg = new Message(MessageType.MEMBER_LIST, "server", emptyList);

        assertEquals(MessageType.MEMBER_LIST, msg.getType());
        assertEquals("server", msg.getSenderId());
        assertNotNull(msg.getMemberList());
        assertTrue(msg.getMemberList().isEmpty());
    }

    /**
     * Tests message creation with a non-empty List<Member> payload.
     * Ensures the list is stored correctly and can be accessed.
     */
    @Test
    void testConstructorWithMemberList() throws Exception {
        Member mockMember = createFakeMember();
        List<Member> list = new ArrayList<>();
        list.add(mockMember);

        Message msg = new Message(MessageType.MEMBER_LIST, "server", list);
        assertEquals(1, msg.getMemberList().size());
        assertEquals("bob", msg.getMemberList().getFirst().getId());
    }

    /**
     * Verifies the recipient ID can be set and retrieved correctly.
     */
    @Test
    void testSetAndGetRecipientId() {
        Message msg = new Message(MessageType.PRIVATE, "alice", "Secret");
        msg.setRecipientId("bob");
        assertEquals("bob", msg.getRecipientId());
    }


    /**
     * Tests setting and retrieving coordinator status and ID.
     */
    @Test
    void testSetAndGetCoordinatorFlags() {
        Message msg = new Message(MessageType.JOIN, "server", "Welcome");
        msg.setCoordinator(true);
        msg.setCoordinatorId("alice");

        assertTrue(msg.isCoordinator());
        assertEquals("alice", msg.getCoordinatorId());
    }

    /**
     * Tests that an invalid payload (e.g. List<String>) throws an IllegalArgumentException.
     */
    @Test
    void testInvalidPayloadThrowsException() {
        List<String> wrongTypeList = List.of("not", "members");
        
        assertThrows(IllegalArgumentException.class, () -> 
        new Message(MessageType.MEMBER_LIST, "server", wrongTypeList));
    }

    /**
     * Helper method to create a fake Member using reflection,
     * accessing the protected constructor.
     */
    private Member createFakeMember() throws Exception {
        var actor = Member.class.getDeclaredConstructor(String.class, String.class, int.class, boolean.class);
        actor.setAccessible(true);
        return actor.newInstance("bob", "127.0.0.1", 1234, false);
    }
}
