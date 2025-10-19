package utils;

import common.Member;
import java.util.Optional;
import server.Coordinator;
import java.util.Collection;
import java.util.Comparator;

/**
 * Handles the election of a coordinator from a collection of members.
 */
public class CoordinatorElection {
	

    /**
     * Elects a coordinator by choosing the member with the highest port number.
     * 
     * @param members Collection of members to elect from.
     * @return Optional containing the elected member, if any.
     */
    public static Optional<Member> elect(Collection<Member> members) {
        return members.stream().max(Comparator.comparingInt(Member::getPort));
    }

    /**
     * Elects a coordinator by choosing the member with the lowest ID.
     * 
     * @param members Collection of members to elect from.
     * @return Optional containing the elected member, if any.
     */
    public static Optional<Member> electById(Collection<Member> members) {
        return members.stream().min(Comparator.comparing(Member::getId));
    }

    /**
     * Elects a new coordinator using the lowest ID rule,
     * updates the Coordinator state, and flags all members accordingly.
     * 
     * This method is synchronised to ensure thread safety during election.
     * 
     * @param members Collection of currently active members.
     * @return The newly elected coordinator, or null if no members are present.
     */
    public static synchronized Member electAndSetNewCoordinator(Collection<Member> members) {
        // If no members are available, reset coordinator and log it.
    	if (members.isEmpty()) {
            Coordinator.reset();
            LoggerUtil.getLogger().info("All members offline. No coordinator elected.");
            return null;
        }

        // Elect the new coordinator using lowest ID
        Member newCoordinator = electById(members).orElse(null);
        if (newCoordinator != null) {
            Coordinator.setCoordinator(newCoordinator);
            newCoordinator.setCoordinator(true);
            LoggerUtil.getLogger().info("New coordinator elected: " + newCoordinator.getId());
        }

        // Update the isCoordinator flag for all members
        for (Member member : members) {
            assert newCoordinator != null;
            if (!member.getId().equals(newCoordinator.getId())) {
                member.setCoordinator(false);
            }
        }
        return newCoordinator;
    }
}
