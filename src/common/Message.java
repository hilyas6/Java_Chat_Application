package common;

import java.io.Serial;
import java.util.List;
import java.io.Serializable;

/**
 * Represents a message exchanged between clients and server in the group chat system.
 * A message can contain text content or a member list, and can be sent privately or broadcast.
 */
@SuppressWarnings("unchecked")
public class Message implements Serializable {
	
    @Serial
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final String senderId;
    private String recipientId;
    private String content;
    private List<String> nameList;
    private List<Member> memberList;
    private String coordinatorId;
    
    private boolean isCoordinator;


    /**
     * Gets the coordinator ID associated with the message.
     */
    public String getCoordinatorId() { return coordinatorId;}

    /**
     * Sets the coordinator ID in the message.
     */
    public void setCoordinatorId(String id) {this.coordinatorId = id;}

    /**
     * Checks if the sender of the message is a coordinator.
     */
    public boolean isCoordinator() {return isCoordinator;}

    /**
     * Marks the sender of the message as a coordinator.
     */
    public void setCoordinator(boolean coordinator) {isCoordinator = coordinator;}

    /**
     * Sets a list of member names (used in name list messages).
     */
    public void setNameList(List<String> nameList) {this.nameList = nameList;}

    /**
     * Gets the list of member names.
     */
    public List<String> getNameList() {return nameList;}

    /**
     * Creates a message with a type, sender ID, and optional payload.
     * The payload can be:
     * - A String (used for content messages),
     * - A List<Member> (used for member list sharing).
     *
     * @param type    The type of message.
     * @param senderId The sender's ID.
     * @param payload The message payload (String or List<Member>).
     */
    public Message(MessageType type, String senderId, Object payload) {
        this.type = type;
        this.senderId = senderId;

        if (payload instanceof String) {
            this.content = (String) payload;
        } else if (payload instanceof List<?> rawList) {
            if (!rawList.isEmpty() && rawList.getFirst() instanceof Member) {
                this.memberList = (List<Member>) rawList;
            } else if (rawList.isEmpty()) {
                this.memberList = (List<Member>) rawList; // empty list is safe to cast
            } else {
                throw new IllegalArgumentException("Expected List<Member> but got mixed types");
            }
        }
    }

    /**
     * Gets the type of the message (e.g., JOIN, LEAVE, BROADCAST, etc.).
     */
    public MessageType getType() { return type; }
    
    /**
     * Gets the ID of the sender who created the message.
     */
    public String getSenderId() { return senderId; }
    
    /**
     * Gets the ID of the intended recipient.
     * Can be null for broadcast messages.
     */
    public String getRecipientId() { return recipientId; }
    
    /**
     * Gets the textual content of the message.
     * This is used in chat or system notifications.
     */
    public String getContent() { return content; }
    
    /**
     * Gets the list of members included in the message.
     * Used for sharing group membership data.
     */
    public List<Member> getMemberList() { return memberList; }

    /**
     * Sets the ID of the recipient for this message.
     * Useful for targeting private messages.
     */
    public void setRecipientId(String recipientId) {this.recipientId = recipientId;}
}
