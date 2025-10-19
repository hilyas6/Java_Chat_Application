package JUnitTests;

import common.Member;
import common.Message;
import common.MessageType;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;


class MemberTest {
	
	/**
     * Tests that the constructor properly sets all fields
     * and that the getter methods return correct values.
     */
    @Test
    void testConstructorAndGetters() throws Exception {
        Member member = createFakeMember("user1", "127.0.0.1", 8080, true);

        assertEquals("user1", member.getId());
        assertEquals("127.0.0.1", member.getIpAddress());
        assertEquals(8080, member.getPort());
        assertTrue(member.isCoordinator());
    }

    /**
     * Tests that setCoordinator() correctly updates the coordinator status.
     */
    @Test
    void testSetCoordinator() throws Exception {
        Member member = createFakeMember("u2", "127.0.0.1", 9000, false);
        assertFalse(member.isCoordinator());  

        member.setCoordinator(true);
        assertTrue(member.isCoordinator());  
    }

    /**
     * Verifies that the copyForClient() method returns a new Member
     * with identical attributes but safe for client use (no socket).
     */
    @Test
    void testCopyForClient() throws Exception {
        Member original = createFakeMember("copyMe", "10.0.0.1", 1234, true);
        Member copy = original.copyForClient();  

        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getIpAddress(), copy.getIpAddress());
        assertEquals(original.getPort(), copy.getPort());
        assertTrue(copy.isCoordinator());
    }

    /**
     * Ensures that calling send() does not throw an exception
     * when the output stream is null (which it is in client-safe members).
     */
    @Test
    void testSend_skipsWhenOutputStreamNull() throws Exception {
        Member member = createFakeMember("noStream", "127.0.0.1", 1234, false);
        
        // Should skip silently and not throw any exceptions
        assertDoesNotThrow(() -> member.send(new Message(MessageType.BROADCAST, "test", "content")));
    }

    /**
     * Ensures that ping() returns false when the output stream is unavailable.
     */
    @Test
    void testPing_returnsFalseWhenOutputStreamNull() throws Exception {
        Member member = createFakeMember("noStream", "127.0.0.1", 1234, false);
        assertFalse(member.ping());  
    }

    /**
     * Helper method to create a fake Member instance using reflection,
     * calling the protected constructor intended for client-safe copies.
     */
    private Member createFakeMember(String id, String ip, int port, boolean isCoordinator) throws Exception {
        Constructor<Member> actor = Member.class.getDeclaredConstructor(String.class, String.class, int.class,
                boolean.class);
        actor.setAccessible(true);  
        return actor.newInstance(id, ip, port, isCoordinator); 
    }

    
    /**
     * Ensures that transient fields like socket and input stream
     * are null in client-safe (copied) member instances.
     */
    @Test
    void testGettersForTransientFields_null() throws Exception {

        Member member = createFakeMember("ghost", "0.0.0.0", 1, false);
        assertNull(member.getSocket());
        assertNull(member.getInputStream());
    }
}
