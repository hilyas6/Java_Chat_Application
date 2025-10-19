package client;

import common.Member;
import common.Message;
import java.util.List;

/**
 * ClientObserver defines a listener interface for receiving updates
 * from a Client instance. Classes implementing this interface can
 * observe a Client and react to incoming messages, membership changes,
 * or disconnections.
 */
public interface ClientObserver {
	
	 /**
     * Called when a new message is received from the server or another client.
     *
     * @param msg The message that was received.
     */
    void onMessageReceived(Message msg);
    
    /**
     * Called when the member list is updated.
     *
     * @param members The updated list of members in the chat.
     */
    void onMemberListUpdated(List<Member> members);
    
    /**
     * Called when the client is disconnected from the server.
     */
    void onDisconnected();
}
