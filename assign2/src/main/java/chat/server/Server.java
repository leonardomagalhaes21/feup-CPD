package chat.server;

import chat.server.auth.AuthenticationService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int port;
    private final String userFilePath;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private boolean isRunning;
    private AuthenticationService authService;
    private final Map<String, Room> rooms = new HashMap<>();

    public Server(int port, String userFilePath) {
        this.port = port;
        this.userFilePath = userFilePath;
    }

    public void start() {
        try {
            // Initialize the authentication service
            authService = new AuthenticationService(userFilePath);

            // Create a default general room
            createRoom("general");
            System.out.println("Created default room: general");

            serverSocket = new ServerSocket(port);
            executor = Executors.newVirtualThreadPerTaskExecutor();
            isRunning = true;

            System.out.println("Server started on port " + port);

            // Accept client connections
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    // Handle each client in a separate virtual thread
                    executor.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        ClientHandler clientHandler = new ClientHandler(clientSocket, authService, this);
        clientHandler.handle();
    }

    public synchronized Room createRoom(String roomName) {
        if (rooms.containsKey(roomName)) {
            return null; // Room already exists
        }

        Room newRoom = new Room(roomName);
        rooms.put(roomName, newRoom);
        return newRoom;
    }

    public synchronized Room getRoom(String roomName) {
        return rooms.get(roomName);
    }

    public synchronized Map<String, Room> getRooms() {
        return new HashMap<>(rooms); // Return a copy to prevent concurrent modification
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        if (executor != null) {
            executor.shutdown();
        }
    }

    public static void main(String[] args) {
        int port = 8888; // Default port
        String userFilePath = "resources/main/users.txt"; // Default user file path

        // Parse command-line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 8888.");
            }
        }

        if (args.length > 1) {
            userFilePath = args[1];
        }

        Server server = new Server(port, userFilePath);
        server.start();
    }
}
