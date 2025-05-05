package chat.server;

import chat.server.auth.AuthenticationService;
import chat.server.auth.SessionManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {

    private final int port;
    private final String userFilePath;
    private SSLServerSocket serverSocket;
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;
    private boolean isRunning;
    private AuthenticationService authService;
    private final Map<String, Room> rooms = new HashMap<>();
    private final ReadWriteLock roomsLock = new ReentrantReadWriteLock();

    private final SessionManager sessionManager = new SessionManager();
    private final Map<String, Room> userSessionRooms = new HashMap<>();

    // SSL configuration
    private static final String KEYSTORE_PATH = "resources/main/server.jks";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String SSL_PROTOCOL = "TLS";

    // Cleanup interval in seconds
    private static final int CLEANUP_INTERVAL = 60;

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

            // Setup SSL
            serverSocket = createSSLServerSocket();
            executor = Executors.newVirtualThreadPerTaskExecutor();

            // Setup scheduled tasks
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutor.scheduleAtFixedRate(this::performCleanup,
                    CLEANUP_INTERVAL,
                    CLEANUP_INTERVAL,
                    TimeUnit.SECONDS);

            // Register shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            isRunning = true;
            System.out.println("Secure server started on port " + port);
            System.out.println("Press Ctrl+C to shut down the server gracefully");

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
        } catch (Exception e) {
            System.err.println("Could not start server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop(); // Ensure resources are properly released
        }
    }

    /**
     * Periodically clean up disconnected clients from all rooms
     */
    private void performCleanup() {
        try {
            System.out.println("Performing periodic cleanup of disconnected clients...");
            int totalRemoved = 0;

            roomsLock.readLock().lock();
            try {
                // Create a copy to avoid concurrent modification
                Map<String, Room> roomsCopy = new HashMap<>(rooms);

                for (Map.Entry<String, Room> entry : roomsCopy.entrySet()) {
                    Room room = entry.getValue();
                    int removed = room.cleanDisconnectedClients();
                    if (removed > 0) {
                        totalRemoved += removed;
                        System.out.println("Removed " + removed + " disconnected clients from room: " + room.getName());
                    }
                }
            } finally {
                roomsLock.readLock().unlock();
            }

            if (totalRemoved > 0) {
                System.out.println("Cleanup completed. Total disconnected clients removed: " + totalRemoved);
            } else {
                System.out.println("Cleanup completed. No disconnected clients found.");
            }
        } catch (Exception e) {
            System.err.println("Error during periodic cleanup: " + e.getMessage());
        }
    }

    /**
     * Creates an SSL server socket with the appropriate SSL configuration.
     */
    private SSLServerSocket createSSLServerSocket() throws Exception {
        try {
            // Load the keystore that contains the server certificate
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(KEYSTORE_PATH)) {
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
            }

            // Create key manager factory using the keystore
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

            // Initialize SSLContext with the key managers
            SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Create the SSL server socket factory
            SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

            // Create and configure the SSL server socket
            return (SSLServerSocket) socketFactory.createServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error creating SSL server socket: " + e.getMessage());
            throw new Exception("Failed to create SSL server socket: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("SSL configuration error: " + e.getMessage());
            throw new Exception("SSL configuration error: " + e.getMessage(), e);
        }
    }

    private void handleClient(Socket clientSocket) {
        ClientHandler clientHandler = new ClientHandler(clientSocket, authService, this);
        clientHandler.handle();
    }

    public Room createRoom(String roomName) {
        roomsLock.writeLock().lock();
        try {
            if (rooms.containsKey(roomName)) {
                return null; // Room already exists
            }

            Room newRoom = new Room(roomName);
            rooms.put(roomName, newRoom);
            return newRoom;
        } finally {
            roomsLock.writeLock().unlock();
        }
    }

    public Room createAiRoom(String roomName, String aiPrompt) {
        roomsLock.writeLock().lock();
        try {
            if (rooms.containsKey(roomName)) {
                return null; // Room already exists
            }

            Room newRoom = new Room(roomName, true, aiPrompt);
            rooms.put(roomName, newRoom);
            return newRoom;
        } finally {
            roomsLock.writeLock().unlock();
        }
    }

    public Room getRoom(String roomName) {
        roomsLock.readLock().lock();
        try {
            return rooms.get(roomName);
        } finally {
            roomsLock.readLock().unlock();
        }
    }

    public Map<String, Room> getRooms() {
        roomsLock.readLock().lock();
        try {
            return new HashMap<>(rooms); // Return a copy to prevent concurrent modification
        } finally {
            roomsLock.readLock().unlock();
        }
    }



    public Room getRoomForUser(String user) {
        return userSessionRooms.get(user);
    }

    public void setRoomForUser(String user, Room room) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        userSessionRooms.put(user, room);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void stop() {
        if (!isRunning) {
            return; // Already stopped
        }

        System.out.println("Shutting down server...");
        isRunning = false;

        // Close the server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed");
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // Shut down the scheduled executor
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
                System.out.println("Scheduled tasks terminated");
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Shut down the client executor
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                System.out.println("Client handlers terminated");
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Server shutdown complete");
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

        // Handle SIGINT (Ctrl+C) for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received. Starting graceful shutdown...");
            server.stop();
        }));

        server.start();
    }
}
