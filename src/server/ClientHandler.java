package server;

import java.io.*;
import common.Member;
import java.util.List;
import common.Message;
import java.net.Socket;
import utils.LoggerUtil;
import common.MessageType;
import utils.MessageFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Handles communication with a single client.
 * Implements Runnable so it can be executed by a thread.
 */
public class ClientHandler implements Runnable {
	
    private final Socket socket;
    private final Server server;
    private final Logger logger;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.logger = LoggerUtil.getLogger();
    }


    /**
     * Handles the full lifecycle of a client connection:
     * - Receiving JOIN
     * - Message routing (broadcast, private, heartbeat, etc.)
     * - LEAVE handling
     * - Graceful disconnection
     */
    @Override
    public void run() {
        String clientId = null;
        boolean registered = false;

        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Expect JOIN message first
            Message hello = (Message) in.readObject();
            clientId = hello.getSenderId();

            Member member = new Member(clientId, socket, in, out);
            boolean success = server.addMember(clientId, member);

            if (!success) {
                out.writeObject(MessageFactory.createError("ID already in use."));
                out.flush();
                socket.close();
                return;
            }

            registered = true;

            // Notify all members that someone joined
            String joinNotice = "👤 " + clientId + " joined the chat.";
            Message broadcastJoin = MessageFactory.createBroadcastMessage("server", joinNotice);
            for (Member m : server.getAllMembers()) {
                if (!m.getId().equals(clientId)) {
                    m.send(broadcastJoin);
                }
            }


            // Assign coordinator if first to join
            if (server.getAllMembers().size() == 1) {
                member.setCoordinator(true);
                Coordinator.setCoordinator(member); // store globally
            }

            // Acknowledge join with coordinator status
            String coordinatorMsg;
            if (member.isCoordinator()) {
                coordinatorMsg = "You are the coordinator.";
            } else {
                coordinatorMsg = "Current coordinator is: " + Coordinator.getCoordinatorId();
            }

            Message joinAck =MessageFactory.createJoinAck(coordinatorMsg, member.isCoordinator(), Coordinator.getCoordinatorId());
            out.writeObject(joinAck);
            out.flush();

            logger.info("Member joined: " + clientId);

            while (true) {
                Message msg = (Message) in.readObject();
                if (msg.getType() == MessageType.LEAVE) {
                    logger.info("Member requested leave: " + clientId);
                    try {
                        out.reset(); // Clear object stream cache
                        out.writeObject(MessageFactory.createLeaveMessage("server"));
                        out.flush();
                    } catch (IOException e) {
                    	logger.warning("Could not send LEAVE_ACK to " + clientId + ": " + e.getMessage());
                    }

                    break;
                }

                // Coordinator directly handles member list requests
                if (msg.getType() == MessageType.REQUEST_MEMBER_LIST) {
                    String coordinatorId = Coordinator.getCoordinatorId();
                    if (clientId.equals(coordinatorId)) {
                        List<Member> cleanList = server.getAllMembers().stream().map(Member::copyForClient).toList();
                        Member requester = server.getMember(clientId);
                        requester.send(MessageFactory.createMemberListUpdate(coordinatorId, cleanList));
                    }
                    continue;
                }

             // Non-coordinators ask for approval to access member list
                if (msg.getType() == MessageType.REQUEST_MEMBER_LIST_APPROVAL) {
                    Member coordinator = Coordinator.getCoordinator();
                    if (coordinator != null) {
                        Message prompt = MessageFactory.createMemberListApprovalRequest(msg.getSenderId());
                        prompt.setRecipientId(coordinator.getId());
                        coordinator.send(prompt);
                    }
                    continue;
                }

                // Coordinator approved the request: send member list
                if (msg.getType() == MessageType.MEMBER_LIST_APPROVED) {
                    Member target = server.getMember(msg.getRecipientId());
                    if (target != null) {
                        List<Member> cleanList = server.getAllMembers().stream().map(Member::copyForClient).toList();
                        target.send(MessageFactory.createMemberListUpdate(Coordinator.getCoordinatorId(), cleanList));
                    }
                    continue;
                }

                // Coordinator denied the member list request
                if (msg.getType() == MessageType.MEMBER_LIST_DENIED) {
                    Member target = server.getMember(msg.getRecipientId());
                    if (target != null) {
                        target.send(MessageFactory.createError(msg.getContent()));

                    }
                    continue;
                }

                // Heartbeat (pong or manual ping)
                if (msg.getType() == MessageType.HEARTBEAT) {
                    if ("pong".equals(msg.getContent())) {  
                    	logger.info("Heartbeat received from " + msg.getSenderId());
                    	
                        server.markHeartbeatResponse(msg.getSenderId(), true);
                        
                        String activeMsg = msg.getSenderId() + " is still active.";
                        Member coordinator = Coordinator.getCoordinator();
                        if (coordinator != null && !coordinator.getId().equals(msg.getSenderId())) {
                            coordinator.send(MessageFactory.createBroadcastMessage("server", activeMsg));
                        }
                        
                    } else if ("manual ping".equals(msg.getContent())) {
                        String coordinatorId = Coordinator.getCoordinatorId();
                        if (clientId.equals(coordinatorId)) {
                            for (Member m : server.getAllMembers()) {
                                if (!m.getId().equals(coordinatorId)) {
                                    m.send(new Message(MessageType.HEARTBEAT, coordinatorId, null));
                                }
                            }
                            server.startHeartbeatTimer();
                        }
                    }
                    continue;
                }

                // Broadcast to all
                if (msg.getType() == MessageType.BROADCAST) {
                    logger.info("[Broadcast] from " + clientId + ": " + msg.getContent());
                    for (Member m : server.getAllMembers()) {
                        m.send(msg);
                    }
                    
                // Private message to one
                } else if (msg.getType() == MessageType.PRIVATE) {
                    logger.info("[Private] from " + clientId + " to " + msg.getRecipientId() + ": " + msg.getContent());
                    Member recipient = server.getMember(msg.getRecipientId());
                    if (recipient != null) recipient.send(msg);
                }
            }

        } catch (EOFException e) {
            logger.info("Client " + clientId + " disconnected (EOF).");
        } catch (Exception e) {
            logger.warning("Connection error with client " + clientId + ": " + e.getMessage());
            logger.log(Level.WARNING, "Connection error with client " + clientId, e);
        } finally {
            // Cleanup after client disconnects
            if (registered && clientId != null) {
                server.removeMember(clientId);
                
                String leaveNotice = "👋 " + clientId + " left the chat.";
                Message broadcastLeave = MessageFactory.createBroadcastMessage("server", leaveNotice);

                for (Member m : server.getAllMembers()) {
                    m.send(broadcastLeave);
                }
            }

            try {
                socket.close();
            } catch (IOException e) {
                logger.warning("Failed to close socket for " + clientId);
            }
        }
    }
}
