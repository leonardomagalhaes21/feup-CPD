package chat.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Room {

    private final String name;
    private final Set<ClientHandler> members;
    private final List<String> messageHistory;
    private static final int MAX_HISTORY_SIZE = 100;  // Limit message history size

    public Room(String name) {
        this.name = name;
        this.members = new HashSet<>();
        this.messageHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public boolean addMember(ClientHandler client) {
        return members.add(client);
    }

    public boolean removeMember(ClientHandler client) {
        return members.remove(client);
    }

    public Set<ClientHandler> getMembers() {
        return new HashSet<>(members);  // Return a copy to prevent concurrent modification issues
    }

    public int getMemberCount() {
        return members.size();
    }

    public void addMessage(String message) {
        messageHistory.add(message);
        // Keep history size limited
        if (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.remove(0);  // Remove oldest message
        }
    }

    public List<String> getRecentMessages(int count) {
        int startIndex = Math.max(0, messageHistory.size() - count);
        return new ArrayList<>(messageHistory.subList(startIndex, messageHistory.size()));
    }

    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);  // Return a copy to prevent concurrent modification issues
    }

    public void broadcast(String message, ClientHandler sender) {
        // Add the message to history
        addMessage(message);

        // Broadcast to all members except the sender
        for (ClientHandler member : getMembers()) {
            if (member != sender) {
                member.sendMessage(message);
            }
        }
    }
}
