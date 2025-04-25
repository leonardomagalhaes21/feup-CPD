package chat.server;

import chat.server.auth.AuthenticationService;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private boolean isRunning;
    private AuthenticationService authService;
    private final Map<String, Room> rooms = new HashMap<>();
    private final ReadWriteLock roomsLock = new ReentrantReadWriteLock();

    // SSL configuration
    private static final String KEYSTORE_PATH = "resources/main/server.jks";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String SSL_PROTOCOL = "TLS";

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
            isRunning = true;

            System.out.println("Secure server started on port " + port);

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
        }
    }

    /**
     * Creates an SSL server socket with the appropriate SSL configuration.
     */
    private SSLServerSocket createSSLServerSocket() throws Exception {
        // Load the keystore that contains the server certificate
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(KEYSTORE_PATH), KEYSTORE_PASSWORD.toCharArray());

        // Create key manager factory using the keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        // Initialize SSLContext with the key managers
        SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
        sslContext.init(kmf.getKeyManagers(), null, null);

        // Create the SSL server socket factory
        SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

        // Create and configure the SSL server socket
        SSLServerSocket sslServerSocket = (SSLServerSocket) socketFactory.createServerSocket(port);

        // Configure SSL parameters if needed
        // For example, you might want to specify which cipher suites or protocols are enabled
        return sslServerSocket;
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
