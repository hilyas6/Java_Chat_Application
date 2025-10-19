package common;

/**
 * Represents the type of a message exchanged in the group chat system.
 * Each enum value defines a specific kind of message with a unique purpose.
 */
public enum MessageType {
    JOIN,
    LEAVE,
    BROADCAST,
    PRIVATE,
    HEARTBEAT,
    MEMBER_LIST,
    REQUEST_MEMBER_LIST,
    REQUEST_MEMBER_LIST_APPROVAL,
    MEMBER_LIST_APPROVED,
    MEMBER_LIST_DENIED,
    MEMBER_NAME_LIST,
    ERROR
}
