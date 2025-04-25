package chat.server;

import chat.server.ai.OllamaService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Room {

    private final String name;
    private final Set<ClientHandler> members;
    private final List<String> messageHistory;
    private static final int MAX_HISTORY_SIZE = 100;  // Limit message history size

    // AI room properties
    private final boolean isAiRoom;
    private final String aiPrompt;
    private final OllamaService ollamaService;

    // Read-write lock for thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Room(String name) {
        this(name, false, null);
    }

    public Room(String name, boolean isAiRoom, String aiPrompt) {
        this.name = name;
        this.members = new HashSet<>();
        this.messageHistory = new ArrayList<>();
        this.isAiRoom = isAiRoom;
        this.aiPrompt = aiPrompt;
        this.ollamaService = isAiRoom ? new OllamaService() : null;

        if (isAiRoom) {
            System.out.println("Created AI room: " + name + " with prompt: " + aiPrompt);
        }
    }

    public String getName() {
        // Name is immutable, no lock needed
        return name;
    }

    public boolean isAiRoom() {
        return isAiRoom;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public boolean addMember(ClientHandler client) {
        lock.writeLock().lock();
        try {
            return members.add(client);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeMember(ClientHandler client) {
        lock.writeLock().lock();
        try {
            return members.remove(client);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<ClientHandler> getMembers() {
        lock.readLock().lock();
        try {
            // Return a copy to prevent concurrent modification issues
            return new HashSet<>(members);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getMemberCount() {
        lock.readLock().lock();
        try {
            return members.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addMessage(String message, ClientHandler sender) {
        // First, add the user message to history
        lock.writeLock().lock();
        List<String> historySnapshot = null;
        try {
            messageHistory.add(message);
            // Keep history size limited
            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                messageHistory.remove(0);  // Remove oldest message
            }

            if (isAiRoom) {
                // Take a snapshot of the message history while holding the lock
                historySnapshot = new ArrayList<>(messageHistory);
            }
        } finally {
            lock.writeLock().unlock();
        }

        // If this is an AI room, generate a response
        if (isAiRoom && ollamaService != null && historySnapshot != null) {
            generateAiResponse(historySnapshot, sender);
        }
    }

    private void generateAiResponse(List<String> historySnapshot, ClientHandler sender) {
        try {
            ollamaService.generateResponse(
                    aiPrompt,
                    historySnapshot,
                    // onSuccess consumer
                    aiResponse -> {
                        // Format the AI's response
                        String formattedResponse = "Bot: " + aiResponse;

                        // Add the bot's response to history
                        lock.writeLock().lock();
                        try {
                            messageHistory.add(formattedResponse);
                            // Keep history size limited
                            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                                messageHistory.remove(0);
                            }
                        } finally {
                            lock.writeLock().unlock();
                        }

                        // Clean up disconnected clients before broadcasting
                        cleanDisconnectedClients();

                        // Broadcast bot's response to all members
                        Set<ClientHandler> membersCopy;
                        lock.readLock().lock();
                        try {
                            membersCopy = new HashSet<>(members);
                        } finally {
                            lock.readLock().unlock();
                        }

                        for (ClientHandler member : membersCopy) {
                            if (member.isConnected()) {
                                member.sendMessage(formattedResponse);
                            }
                        }
                    },
                    // onError consumer
                    errorMsg -> {
                        System.err.println("AI error in room " + name + ": " + errorMsg);

                        // Notify the room of the error (only the sender)
                        if (sender != null && sender.isConnected()) {
                            sender.sendMessage("Bot error: Unable to generate response. Please try again later.");
                        }
                    }
            );
        } catch (Exception e) {
            System.err.println("Failed to request AI response: " + e.getMessage());
            if (sender != null && sender.isConnected()) {
                sender.sendMessage("Bot error: Unable to connect to AI service. Please try again later.");
            }
        }
    }

    public void addMessage(String message) {
        addMessage(message, null);
    }

    public List<String> getRecentMessages(int count) {
        lock.readLock().lock();
        try {
            int startIndex = Math.max(0, messageHistory.size() - count);
            return new ArrayList<>(messageHistory.subList(startIndex, messageHistory.size()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getMessageHistory() {
        lock.readLock().lock();
        try {
            // Return a copy to prevent concurrent modification issues
            return new ArrayList<>(messageHistory);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes any disconnected clients from the room
     *
     * @return Number of clients removed
     */
    public int cleanDisconnectedClients() {
        lock.writeLock().lock();
        try {
            int removedCount = 0;
            Iterator<ClientHandler> iterator = members.iterator();
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                if (!client.isConnected()) {
                    iterator.remove();
                    removedCount++;
                    System.out.println("Removed disconnected client: " + client.getUsername() + " from room: " + name);
                }
            }
            return removedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        // First, clean up any disconnected clients
        cleanDisconnectedClients();

        // Add the message to history with a write lock and trigger AI response if needed
        addMessage(message, sender);

        // Then get a snapshot of members with a read lock
        Set<ClientHandler> membersCopy;
        lock.readLock().lock();
        try {
            membersCopy = new HashSet<>(members);
        } finally {
            lock.readLock().unlock();
        }

        // Broadcast to all members in the snapshot except the sender
        // No lock needed here as we're working on a local copy
        for (ClientHandler member : membersCopy) {
            try {
                if (member != sender && member.isConnected()) {
                    member.sendMessage(message);
                }
            } catch (Exception e) {
                System.err.println("Error sending message to " + member.getUsername() + ": " + e.getMessage());
                // Will be cleaned up on next broadcast/cleanup cycle
            }
        }
    }
}
