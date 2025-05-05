package chat.server;

import chat.server.auth.AuthenticationService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
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
    private String sessionToken;

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
        } catch (SocketException se) {
            System.err.println("Client connection lost: " + (username != null ? username : "unknown") + " - " + se.getMessage());
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error handling client: " + e.getMessage());
            e.printStackTrace();
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
        String pendingSessionToken = null;

        try {
            while (attempts < MAX_LOGIN_ATTEMPTS && (line = in.readLine()) != null) {
                // First check if client sent a session token
                if (line.startsWith("SESSION_TOKEN:")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        pendingSessionToken = parts[1].trim();
                        System.out.println("Received session token from client");
                        
                        // Try to authenticate directly with the token
                        String existingUsername = server.getSessionManager().validateSession(pendingSessionToken);
                        if (existingUsername != null) {
                            // Valid session token - authenticate user immediately
                            username = existingUsername;
                            sessionToken = pendingSessionToken;
                            isAuthenticated = true;
                            // Restore user's room if they were in one
                            Room restoredRoom = server.getRoomForUser(username);

                            if (restoredRoom != null) {
                                System.out.println("Restoring room for user: " + username + " - Room: " + restoredRoom.getName());
                                // Add client to the room
                                restoredRoom.addMember(this);
                                currentRoom = restoredRoom;
                                
                                out.println("AUTH_OK: Welcome back, " + username + "! You have been reconnected to room: " + restoredRoom.getName());
                                
                                // Send recent message history
                                List<String> recentMessages = restoredRoom.getRecentMessages(RECENT_MESSAGES_COUNT);
                                if (!recentMessages.isEmpty()) {
                                    out.println("Recent messages:");
                                    for (String message : recentMessages) {
                                        out.println(message);
                                    }
                                }
                                
                                // Broadcast join message to other room members
                                restoredRoom.broadcast("[" + username + " has reconnected to the room]", this);
                            } else {
                                out.println("AUTH_OK: Welcome back, " + username + "!");
                            }
                            
                            System.out.println("User authenticated via session token: " + username);
                            return true;
                        } else {
                            out.println("Your session has expired. Please login with username and password.");
                        }
                        continue;
                    }
                }
                
                // Process login command
                if (line.startsWith("/login")) {
                    String[] parts = line.split("\\s+", 3);

                    if (parts.length < 3) {
                        out.println("AUTH_FAIL: Invalid format. Use: /login <username> <password>");
                        attempts++;
                        continue;
                    }

                    username = parts[1];
                    String password = parts[2];
                    
                    // Check credentials
                    if (!authService.authenticate(username, password)) {
                        out.println("AUTH_FAIL: Invalid credentials or user already logged in");
                        attempts++;
                        username = null; // Reset username since authentication failed
                        continue;
                    }

                    isAuthenticated = true;

                    // Create new session token
                    sessionToken = server.getSessionManager().createSession(username);
                    out.println("AUTH_OK: Welcome, " + username + "! Your session token: " + sessionToken);

                    System.out.println("User authenticated with credentials: " + username);
                    return true;
                } else {
                    out.println("AUTH_FAIL: Please login first using: /login <username> <password>");
                    attempts++;
                }
            }

            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                out.println("AUTH_FAIL: Too many failed login attempts. Connection closed.");
                System.out.println("Client exceeded maximum login attempts. Connection closed.");
            }
        } catch (IOException e) {
            System.err.println("Error during authentication: " + e.getMessage());
            throw e;
        }

        return false;
    }

    private void processCommands() throws IOException {
        String line;
        
        if (currentRoom == null) {
            sendHelp();
        }

        try {
            while ((line = in.readLine()) != null) {
                try {
                    if (line.startsWith("/")) {
                        // Handle commands
                        String[] parts = line.split("\\s+", 3); // Split into at most 3 parts for AI room
                        String command = parts[0].toLowerCase();

                        switch (command) {
                            case "/list":
                                listRooms();
                                break;
                            case "/create":
                                if (parts.length < 2) {
                                    out.println("ERROR: Usage: /create <roomname> [ai_prompt]");
                                } else if (parts.length == 2) {
                                    createRegularRoom(parts[1]);
                                } else { // parts.length == 3
                                    createAiRoom(parts[1], parts[2]);
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
                                server.setRoomForUser(username, null);
                                break;
                            case "/logout":
                                handleLogout();
                                return;
                            case "/exit":
                                out.println("Goodbye! Disconnecting...");
                                return;
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
                } catch (Exception e) {
                    System.err.println("Error processing command from " + username + ": " + e.getMessage());
                    out.println("Error processing your command. Please try again or type /help.");
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost with client " + username + ": " + e.getMessage());
            throw e;
        }
    }

    private void listRooms() {
        Map<String, Room> rooms = server.getRooms();

        if (rooms.isEmpty()) {
            out.println("No rooms available. Create one with /create <roomname> [ai_prompt]");
            return;
        }

        out.println("Available rooms:");
        for (Map.Entry<String, Room> entry : rooms.entrySet()) {
            Room room = entry.getValue();
            String roomInfo = "- " + room.getName() + " (" + room.getMemberCount() + " users)";

            // Add AI room indicator if it's an AI room
            if (room.isAiRoom()) {
                roomInfo += " [AI Room]";
            }

            out.println(roomInfo);
        }
    }

    private void createRegularRoom(String roomName) {
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

    private void createAiRoom(String roomName, String aiPrompt) {
        roomName = roomName.trim();

        if (roomName.isEmpty()) {
            out.println("ERROR: Room name cannot be empty");
            return;
        }

        if (aiPrompt.isEmpty()) {
            out.println("ERROR: AI prompt cannot be empty");
            return;
        }

        Room newRoom = server.createAiRoom(roomName, aiPrompt);

        if (newRoom == null) {
            out.println("ERROR: Room '" + roomName + "' already exists");
        } else {
            out.println("AI Room '" + roomName + "' created successfully with prompt: " + aiPrompt);
        }
    }

    private void createRoom(String input) {
        String[] parts = input.split("\\s+", 2);
        String roomName = parts[0].trim();

        if (roomName.isEmpty()) {
            out.println("ERROR: Room name cannot be empty");
            return;
        }

        // If there's additional text after the room name, it's an AI prompt
        if (parts.length > 1) {
            createAiRoom(roomName, parts[1]);
        } else {
            createRegularRoom(roomName);
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
        
        // Track the user's room for reconnection
        server.setRoomForUser(username, room);

        // Confirm to client
        StringBuilder joinMessage = new StringBuilder();
        joinMessage.append("You joined room: ").append(roomName);

        if (room.isAiRoom()) {
            joinMessage.append(" [AI Room with prompt: ").append(room.getAiPrompt()).append("]");
        }

        out.println(joinMessage.toString());

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
        out.println("/create <roomname> - Create a new regular room");
        out.println("/create <roomname> <ai_prompt> - Create a new AI room with specified prompt");
        out.println("/join <roomname> - Join an existing room");
        out.println("/leave - Leave current room");
        out.println("/logout - Log out current user");
        out.println("/exit - Disconnect from the server");
        out.println("/help - Show this help message");
        out.println("");
        out.println("To send a message, simply type and press Enter when in a room");
        out.println("In AI rooms, the AI will respond to your messages based on the room's prompt");
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
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Checks if the connection to this client is still active
     */
    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
    }

    private void handleLogout() {
        if (isAuthenticated && username != null) {
            leaveCurrentRoom();
            
            // Invalidate the session token
            if (sessionToken != null) {
                server.getSessionManager().invalidateSession(sessionToken);
                sessionToken = null;
            }
            
            // Log the user out on the authentication service
            authService.logout(username);
            
            // Clear room association
            server.setRoomForUser(username, null);
            
            // Reset client state
            isAuthenticated = false;
            
            System.out.println("User logged out: " + username);
            out.println("You have been logged out. Please login again with /login <username> <password>");
            
            // Reset username last
            username = null;
            
            // Restart the authentication process
            try {
                if (authenticate()) {
                    processCommands();
                }
            } catch (IOException e) {
                System.err.println("Error during re-authentication after logout: " + e.getMessage());
            }
        } else {
            out.println("You are not currently logged in.");
        }
    }
}
