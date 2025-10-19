package utils;

import common.Member;
import common.Message;
import java.util.List;
import common.MessageType;

/**
 * A utility class for creating different types of messages used in the group chat system.
 */
public class MessageFactory {
	
    /**
     * Creates a JOIN message.
     *
     * @param senderId ID of the member joining
     * @param content  Optional message content
     * @return Message representing a join request
     */
    public static Message createJoinMessage(String senderId, String content) {
        return new Message(MessageType.JOIN, senderId, content);
    }

    /**
     * Creates a LEAVE message.
     *
     * @param senderId ID of the member leaving
     * @return Message indicating a member left the group
     */
    public static Message createLeaveMessage(String senderId) {
        return new Message(MessageType.LEAVE, senderId, "Leaving the group");
    }

    /**
     * Creates a MEMBER_NAME_LIST message to send a list of member IDs to a specific recipient.
     *
     * @param recipientId ID of the member receiving the name list
     * @param members     List of all group members
     * @return Message containing a sorted list of member names
     */
    public static Message createMemberNameList(String recipientId, List<Member> members) {
        List<String> names = members.stream()
                .map(Member::getId)
                .sorted()
                .toList();

        Message msg = new Message(MessageType.MEMBER_NAME_LIST, "server", null);
        msg.setRecipientId(recipientId);
        msg.setNameList(names);
        return msg;
    }

    /**
     * Creates a BROADCAST message to send a message to all members.
     *
     * @param senderId ID of the sender
     * @param content  Message content
     * @return Broadcast message
     */
    public static Message createBroadcastMessage(String senderId, String content) {
        return new Message(MessageType.BROADCAST, senderId, content);
    }


    /**
     * Creates a PRIVATE message to send to a specific member.
     *
     * @param senderId    ID of the sender
     * @param recipientId ID of the recipient
     * @param content     Message content
     * @return Private message
     */
    public static Message createPrivateMessage(String senderId, String recipientId, String content) {
        Message message = new Message(MessageType.PRIVATE, senderId, content);
        message.setRecipientId(recipientId);
        return message;
    }

    /**
     * Creates a manual HEARTBEAT request message.
     *
     * @param coordinatorId ID of the coordinator sending the ping
     * @return Heartbeat message
     */
    public static Message createManualHeartbeatRequest(String coordinatorId) {
        return new Message(MessageType.HEARTBEAT, coordinatorId, "manual ping");
    }

    /**
     * Creates a MEMBER_LIST update message containing the full list of members.
     *
     * @param senderId ID of the sender
     * @param members  List of all members
     * @return Member list update message
     */
    public static Message createMemberListUpdate(String senderId, List<Member> members) {
        return new Message(MessageType.MEMBER_LIST, senderId, members);
    }

    /**
     * Creates an ERROR message.
     *
     * @param content Description of the error
     * @return Error message
     */
    public static Message createError(String content) {
        return new Message(MessageType.ERROR, "server", content);
    }


    /**
     * Creates a REQUEST_MEMBER_LIST_APPROVAL message.
     *
     * @param senderId ID of the sender
     * @return Member list approval request message
     */
    public static Message createMemberListApprovalRequest(String senderId) {
        return new Message(MessageType.REQUEST_MEMBER_LIST_APPROVAL, senderId, "Requesting member list " +
                "approval.");
    }

    /**
     * Creates a REQUEST_MEMBER_LIST message to fetch the current member list.
     *
     * @param senderId ID of the requester
     * @return Member list request message
     */
    public static Message createMemberListRequest(String senderId) {
        return new Message(MessageType.REQUEST_MEMBER_LIST, senderId, "");
    }

    /**
     * Creates a JOIN acknowledgement message containing coordinator info.
     *
     * @param content       Message content
     * @param isCoordinator Whether the recipient is a coordinator
     * @param coordinatorId ID of the current coordinator
     * @return Join acknowledgment message
     */
    public static Message createJoinAck(String content, boolean isCoordinator, String coordinatorId) {
        Message msg = new Message(MessageType.JOIN, "server", content);
        msg.setCoordinator(isCoordinator);
        msg.setCoordinatorId(coordinatorId);
        return msg;
    }

}
