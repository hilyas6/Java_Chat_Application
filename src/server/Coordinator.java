package server;

import common.Member;


/**
 * The Coordinator class is responsible for managing the coordinator (leader) in the chat system.
 * The coordinator is the member responsible for handling key operations such as approving member list requests.
 * This class provides static methods to set, get, and reset the coordinator.
 */
public class Coordinator {
    private static Member coordinator;

    /**
     * Sets the coordinator to the specified member.
     * @param member The member to be assigned as the coordinator.
     */
    public static void setCoordinator(Member member) {
        coordinator = member;
    }

    /**
     * Retrieves the current coordinator.
     * @return The coordinator member, or null if no coordinator is assigned.
     */
    public static Member getCoordinator() {
        return coordinator;
    }

    /**
     * Retrieves the ID of the current coordinator.
     * @return The ID of the coordinator, or null if no coordinator exists.
     */
    public static String getCoordinatorId() {
        return coordinator != null ? coordinator.getId() : null;
    }

    /**
     * Resets the coordinator, removing any assigned coordinator.
     */
    public static void reset() {
        coordinator = null;
    }
}
