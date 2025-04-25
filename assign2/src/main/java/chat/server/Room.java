package chat.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Room {

    private final String name;
    private final Set<ClientHandler> members;
    private final List<String> messageHistory;
    private static final int MAX_HISTORY_SIZE = 100;  // Limit message history size

    // Read-write lock for thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Room(String name) {
        this.name = name;
        this.members = new HashSet<>();
        this.messageHistory = new ArrayList<>();
    }

    public String getName() {
        // Name is immutable, no lock needed
        return name;
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

    public void addMessage(String message) {
        lock.writeLock().lock();
        try {
            messageHistory.add(message);
            // Keep history size limited
            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                messageHistory.remove(0);  // Remove oldest message
            }
        } finally {
            lock.writeLock().unlock();
        }
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

    public void broadcast(String message, ClientHandler sender) {
        // First, add the message to history with a write lock
        addMessage(message);

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
            if (member != sender) {
                member.sendMessage(message);
            }
        }
    }
}
