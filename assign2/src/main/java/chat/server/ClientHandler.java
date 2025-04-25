package chat.server;

import chat.server.auth.AuthenticationService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ClientHandler {

    private final Socket clientSocket;
    private final AuthenticationService authService;
    private final Server server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Room currentRoom;
    private boolean isAuthenticated = false;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int RECENT_MESSAGES_COUNT = 10;

    public ClientHandler(Socket clientSocket, AuthenticationService authService, Server server) {
        this.clientSocket = clientSocket;
        this.authService = authService;
        this.server = server;
    }

    public void handle() {
        try {
            // Set up input and output streams
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send welcome message and authentication prompt
            out.println("Welcome to the chat server! Please login using: /login <username> <password>");

            // Authentication phase
            if (!authenticate()) {
                return; // Exit if authentication fails
            }

            // Process client commands
            processCommands();
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            // If user was authenticated, log them out and leave any room they were in
            if (isAuthenticated && username != null) {
                leaveCurrentRoom();
                authService.logout(username);
                System.out.println("User logged out: " + username);
            }
            closeResources();
        }
    }

    private boolean authenticate() throws IOException {
        int attempts = 0;
        String line;

        while (attempts < MAX_LOGIN_ATTEMPTS && (line = in.readLine()) != null) {
            if (line.startsWith("/login")) {
                String[] parts = line.split("\\s+", 3);

                if (parts.length < 3) {
                    out.println("AUTH_FAIL: Invalid format. Use: /login <username> <password>");
                    attempts++;
                    continue;
                }

                username = parts[1];
                String password = parts[2];

                if (authService.authenticate(username, password)) {
                    isAuthenticated = true;
                    out.println("AUTH_OK: Welcome, " + username + "!");
                    System.out.println("User authenticated: " + username);
                    return true;
                } else {
                    out.println("AUTH_FAIL: Invalid credentials or user already logged in");
                    attempts++;
                }
            } else {
                out.println("AUTH_FAIL: Please login first using: /login <username> <password>");
                attempts++;
            }
        }

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            out.println("AUTH_FAIL: Too many failed login attempts. Connection closed.");
            System.out.println("Client exceeded maximum login attempts. Connection closed.");
        }

        return false;
    }

    private void processCommands() throws IOException {
        String line;
        sendHelp();

        while ((line = in.readLine()) != null) {
            if (line.startsWith("/")) {
                // Handle commands
                String[] parts = line.split("\\s+", 2);
                String command = parts[0].toLowerCase();

                switch (command) {
                    case "/list":
                        listRooms();
                        break;
                    case "/create":
                        if (parts.length < 2) {
                            out.println("ERROR: Usage: /create <roomname>");
                        } else {
                            createRoom(parts[1]);
                        }
                        break;
                    case "/join":
                        if (parts.length < 2) {
                            out.println("ERROR: Usage: /join <roomname>");
                        } else {
                            joinRoom(parts[1]);
                        }
                        break;
                    case "/leave":
                        leaveCurrentRoom();
                        break;
                    case "/help":
                        sendHelp();
                        break;
                    default:
                        out.println("Unknown command: " + command + ". Type /help for available commands.");
                }
            } else if (line.trim().isEmpty()) {
                // Ignore empty messages
                continue;
            } else {
                // Handle regular chat message
                sendChatMessage(line);
            }
        }
    }

    private void listRooms() {
        Map<String, Room> rooms = server.getRooms();

        if (rooms.isEmpty()) {
            out.println("No rooms available. Create one with /create <roomname>");
            return;
        }

        out.println("Available rooms:");
        for (Map.Entry<String, Room> entry : rooms.entrySet()) {
            Room room = entry.getValue();
            out.println("- " + room.getName() + " (" + room.getMemberCount() + " users)");
        }
    }

    private void createRoom(String roomName) {
        roomName = roomName.trim();

        if (roomName.isEmpty()) {
            out.println("ERROR: Room name cannot be empty");
            return;
        }

        Room newRoom = server.createRoom(roomName);

        if (newRoom == null) {
            out.println("ERROR: Room '" + roomName + "' already exists");
        } else {
            out.println("Room '" + roomName + "' created successfully");
        }
    }

    private void joinRoom(String roomName) {
        if (currentRoom != null && currentRoom.getName().equals(roomName)) {
            out.println("You are already in room '" + roomName + "'");
            return;
        }

        // Leave current room if in one
        leaveCurrentRoom();

        Room room = server.getRoom(roomName);

        if (room == null) {
            out.println("ERROR: Room '" + roomName + "' does not exist");
            return;
        }

        // Add client to the room
        room.addMember(this);
        currentRoom = room;

        // Confirm to client
        out.println("You joined room: " + roomName);

        // Send recent message history
        List<String> recentMessages = room.getRecentMessages(RECENT_MESSAGES_COUNT);
        if (!recentMessages.isEmpty()) {
            out.println("Recent messages:");
            for (String message : recentMessages) {
                out.println(message);
            }
        }

        // Broadcast join message to other room members
        room.broadcast("[" + username + " has joined the room]", this);
    }

    private void leaveCurrentRoom() {
        if (currentRoom != null) {
            // Broadcast leave message
            currentRoom.broadcast("[" + username + " has left the room]", this);

            // Remove from room
            currentRoom.removeMember(this);

            // Inform client
            out.println("You left room: " + currentRoom.getName());

            // Clear current room reference
            currentRoom = null;
        }
    }

    private void sendChatMessage(String message) {
        if (currentRoom == null) {
            out.println("You are not in any room. Join a room first with /join <roomname>");
            return;
        }

        // Format the message with username
        String formattedMessage = username + ": " + message;

        // Send to self to confirm
        out.println(formattedMessage);

        // Broadcast to room members
        currentRoom.broadcast(formattedMessage, this);
    }

    private void sendHelp() {
        out.println("Available commands:");
        out.println("/list - List available rooms");
        out.println("/create <roomname> - Create a new room");
        out.println("/join <roomname> - Join an existing room");
        out.println("/leave - Leave current room");
        out.println("/help - Show this help message");
        out.println("");
        out.println("To send a message, simply type and press Enter when in a room");
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getUsername() {
        return username;
    }

    private void closeResources() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
