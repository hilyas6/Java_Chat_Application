package server;

import java.io.*;
import java.net.*;
import java.util.*;
import common.Member;
import common.Message;
import utils.LoggerUtil;
import utils.MessageFactory;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CoordinatorElection;
import javafx.application.Platform;

/**
 * Main Server class that handles client registration, coordinator election,
 * heartbeat monitoring, and communication logic.
 */
public class Server {
    private final int port;
    private ServerSocket serverSocket;
    
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final Map<String, Boolean> heartbeatResponses = new ConcurrentHashMap<>();
    
    private String coordinatorId = null;
    private volatile boolean running = true;
    private Runnable onLastMemberLeft;
    
    private ScheduledExecutorService heartbeatScheduler;
    private ScheduledFuture<?> heartbeatTask;
    private static final Logger LOGGER = LoggerUtil.getLogger();

    public Server(int port) {
        this.port = port;
    }


    /**
     * Starts the server socket, begins accepting clients, and sets up shutdown hook & heartbeat checks.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        LOGGER.info("Server running on port " + port);
        
        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("Shutting down server...");
                running = false;
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while shutting down server", e);
            }
        }));

        startHeartbeatTimer();

        // Accept loop
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, this)).start();
            } catch (IOException e) {
                if (running) {
                	LOGGER.warning("Server error: " + e.getMessage());
                } else {
                	LOGGER.info("Server stopped accepting connections.");
                }
            }
        }
    }

    /**
     * Adds a new member to the server if the ID is unique.
     */
    public synchronized boolean addMember(String id, Member member) {
        if (members.containsKey(id)) {
            LOGGER.warning("Duplicate ID attempted: " + id);
            return false;
        }

        members.put(id, member);

        // First member becomes coordinator
        if (members.size() == 1) {
            member.setCoordinator(true);
            coordinatorId = id;
            Coordinator.setCoordinator(member);
            LoggerUtil.getLogger().info("New coordinator elected: " + id);
            sendNameListToCoordinator();
        }
        
        if (id.equals(coordinatorId)) {
            Coordinator.setCoordinator(member); 
        }

        sendNameListToCoordinator();
        return true;
    }

    /**
     * Removes a member and triggers coordinator election if needed.
     */
    public synchronized void removeMember(String id) {
    	LOGGER.info("Removing member: " + id);
        members.remove(id);

        if (id.equals(coordinatorId)) {
            electNewCoordinator();
            if (!members.isEmpty()) {
                coordinatorId = Collections.min(members.keySet());
            } else {
                coordinatorId = null;
            }
        }

        // If no members remain, invoke the shutdown callback
        if (members.isEmpty() && onLastMemberLeft != null) {
            try {
                Platform.runLater(onLastMemberLeft);
            } catch (IllegalStateException e) {
                onLastMemberLeft.run();
            }
        }
        sendNameListToCoordinator();
    }


    /**
     * Elects a new coordinator if the current one leaves or fails.
     */
    private synchronized void electNewCoordinator() {
        for (Member m : members.values()) {
            m.setCoordinator(false);
        }

        Member newCoordinator = CoordinatorElection.electAndSetNewCoordinator(members.values());

        if (newCoordinator != null) {
            coordinatorId = newCoordinator.getId();
            Coordinator.setCoordinator(newCoordinator);
            newCoordinator.setCoordinator(true);
            sendNameListToCoordinator();
            
            LOGGER.info("New coordinator elected: " + coordinatorId);

            for (Member m : members.values()) {
                Message msg;
                if (m.equals(newCoordinator)) {
                    msg = MessageFactory.createJoinAck("You are the coordinator.", true, coordinatorId);
                } else {
                    msg = MessageFactory.createJoinAck("Current coordinator is: " + coordinatorId, false, coordinatorId);
                }
                m.send(msg);
            }

        } else {
            coordinatorId = null;
            Coordinator.reset();
            LOGGER.warning("No coordinator available.");
        }
    }

    /**
     * Starts or resets the heartbeat timer to check active clients periodically.
     */
    public void startHeartbeatTimer() {
        if (heartbeatScheduler == null) {
            heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        }

        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
        }

        int HEARTBEAT_INTERVAL = 5 * 60; // 5 minutes
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(
                this::checkHeartbeats, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS
        );
    }

    private void checkHeartbeats() {
        LOGGER.info("\u23F1\uFE0F Running heartbeat check...");
        
        heartbeatResponses.clear();

        for (Member member : members.values()) {
            heartbeatResponses.put(member.getId(), false); // default: not responded
            member.ping();  // sends HEARTBEAT message
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, Boolean> entry : heartbeatResponses.entrySet()) {
                if (!entry.getValue()) {                    
                    toRemove.add(entry.getKey());
                }
            }

            for (String id : toRemove) {
                removeMember(id);
                LOGGER.info("Removed inactive member: " + id);
            }

            // Check if coordinator was removed and elect a new one
            if (toRemove.contains(coordinatorId)) {             
                electNewCoordinator();

                Optional<Member> newCoordinator = CoordinatorElection.elect(members.values());
                newCoordinator.ifPresentOrElse(
                    m -> {
                        coordinatorId = m.getId();    
                        LOGGER.info("\u2705 New coordinator elected: " + coordinatorId);
                    },
                    () -> {
                        coordinatorId = null;
                        LOGGER.warning("\u274C No members left to elect as coordinator.");
                    }
                );
            }

            scheduler.shutdown();
        }, 60, TimeUnit.SECONDS);
    }

    /**
     * Marks a client as having responded to a heartbeat.
     */
    public void markHeartbeatResponse(String memberId, boolean responded) {
        heartbeatResponses.put(memberId, responded);
    }

    /**
     * Sends updated name list to the current coordinator.
     */
    private void sendNameListToCoordinator() {
        Member coordinator = Coordinator.getCoordinator();
        if (coordinator != null) {
            Message msg = MessageFactory.createMemberNameList(
                    coordinator.getId(),
                    new ArrayList<>(members.values())
            );
            coordinator.send(msg);
        }
    }

    public Member getMember(String id) {
        return members.get(id);
    }

    public Collection<Member> getAllMembers() {
        return members.values();
    }

    public void setOnLastMemberLeft(Runnable callback) {
        this.onLastMemberLeft = callback;
    }
}
