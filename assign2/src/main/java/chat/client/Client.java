package chat.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Client {

    private final String serverAddress;
    private final int serverPort;
    private SSLSocket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleIn;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isAuthenticated = new AtomicBoolean(false);
    private final AtomicBoolean isInRoom = new AtomicBoolean(false);
    private ExecutorService executor;
    private String username;
    private String sessionToken;
    
    // Client ID for session file
    private final String clientId;
    private static final String SESSION_FILE_FORMAT = "resources/main/client_session_%s.txt";

    // SSL configuration
    private static final String TRUSTSTORE_PATH = "resources/main/client_truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "password";
    private static final String SSL_PROTOCOL = "TLS";

    public Client(String serverAddress, int serverPort, String clientId) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.consoleIn = new BufferedReader(new InputStreamReader(System.in));
        this.clientId = clientId != null ? clientId : "default";
    }

    public void start() {
        try {
            // Try to load a previous session token
            loadSession();
            
            System.out.println("Connecting to server at " + serverAddress + ":" + serverPort + "...");
            System.out.println("Client ID: " + clientId);

            try {
                // Connect to the server using SSL
                socket = createSSLSocket();
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (ConnectException e) {
                System.err.println("Error: Could not connect to server at " + serverAddress + ":" + serverPort);
                System.err.println("Please check that the server is running and the address is correct.");
                return;
            } catch (Exception e) {
                System.err.println("Connection error: " + e.getMessage());
                return;
            }

            isRunning.set(true);

            // Register shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            System.out.println("Connected securely to server at " + serverAddress + ":" + serverPort);

            // Start a virtual thread to read server responses
            executor = Executors.newVirtualThreadPerTaskExecutor();
            executor.submit(this::readServerResponses);

            // Wait for initial server welcome message
            Thread.sleep(500);
            
            // Try to authenticate with session token if we have one
            if (sessionToken != null && !sessionToken.isEmpty()) {
                System.out.println("Attempting to authenticate with saved session token...");
                out.println("SESSION_TOKEN:" + sessionToken);
                // Wait a moment to see if authentication succeeds
                Thread.sleep(1000);
            }

            // Authentication loop if not yet authenticated
            while (isRunning.get() && !isAuthenticated.get()) {
                try {
                    System.out.print("Please login (/login username password): ");
                    String loginCommand = consoleIn.readLine();

                    if (loginCommand == null || loginCommand.equalsIgnoreCase("/exit")) {
                        System.out.println("Exiting...");
                        shutdown();
                        return;
                    }

                    // Validate login command format
                    if (!loginCommand.startsWith("/login ")) {
                        System.out.println("Invalid command format. Use: /login username password");
                        continue;
                    }

                    // Parse username for later reference
                    String[] parts = loginCommand.split("\\s+", 3);
                    if (parts.length < 3) {
                        System.out.println("Invalid login format. Use: /login username password");
                        continue;
                    }

                    this.username = parts[1];

                    // Send login command to server
                    out.println(loginCommand);

                    // Wait for authentication response (handled in readServerResponses)
                    // Sleep a bit to allow response to be processed
                    Thread.sleep(1000);
                } catch (IOException e) {
                    System.err.println("Error reading from console: " + e.getMessage());
                    shutdown();
                    return;
                }
            }

            // Main thread reads user input and sends to server if authenticated
            if (isAuthenticated.get()) {
                // Only display the login banner if we have a username and are not already in a room
                if (username != null && !username.isEmpty() && !isInRoom.get()) {
                    System.out.println("\n============================================");
                    System.out.println("You are now logged in as " + username);
                    System.out.println("Type /help to see available commands");
                    System.out.println("Type /exit to disconnect from the server");
                    System.out.println("============================================\n");
                }

                String userInput;
                while (isRunning.get() && (userInput = consoleIn.readLine()) != null) {
                    // Check for client-side exit command
                    if (userInput.equalsIgnoreCase("/exit")) {
                        System.out.println("Disconnecting from server...");
                        shutdown();
                        break;
                    }

                    // Send the input to the server
                    out.println(userInput);
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Client interrupted: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void loadSession() {
        try {
            Path sessionFilePath = getSessionFilePath();
            if (Files.exists(sessionFilePath)) {
                sessionToken = Files.readString(sessionFilePath).trim();
                if (!sessionToken.isEmpty()) {
                    System.out.println("Found saved session token for client ID: " + clientId);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load session: " + e.getMessage());
        }
    }

    private void saveSession() {
        if (sessionToken == null) {
            return;
        }
        
        try {
            Path sessionFilePath = getSessionFilePath();
            Files.createDirectories(sessionFilePath.getParent());
            Files.writeString(sessionFilePath, sessionToken);
            System.out.println("Session token saved for client ID: " + clientId);
        } catch (IOException e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    private Path getSessionFilePath() {
        return Paths.get(String.format(SESSION_FILE_FORMAT, clientId));
    }

    /**
     * Creates an SSL socket with the appropriate SSL configuration.
     */
    private SSLSocket createSSLSocket() throws Exception {
        try {
            // Load the truststore that contains the trusted certificates
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(TRUSTSTORE_PATH)) {
                trustStore.load(fis, TRUSTSTORE_PASSWORD.toCharArray());
            }

            // Create trust manager factory using the truststore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize SSLContext with the trust managers
            SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create the SSL socket factory
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            // Create and configure the SSL socket
            SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(serverAddress, serverPort);

            // Begin the SSL handshake
            sslSocket.startHandshake();

            return sslSocket;
        } catch (IOException e) {
            throw new Exception("Failed to create SSL socket: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new Exception("SSL configuration error: " + e.getMessage(), e);
        }
    }

    private void readServerResponses() {
        try {
            String response;
            while (isRunning.get() && (response = in.readLine()) != null) {
                // Check for authentication responses
                if (response.startsWith("AUTH_OK:")) {
                    isAuthenticated.set(true);
                    
                    // Check if we're automatically placed in a room (reconnection case)
                    if (response.contains("reconnected to room:") || response.contains("joined room")) {
                        isInRoom.set(true);
                    }
                    
                    // Extract username from the welcome message
                    if (response.contains("Welcome back, ")) {
                        String[] parts = response.split("Welcome back, ");
                        if (parts.length > 1) {
                            String userPart = parts[1];
                            // Extract username up to the next punctuation or space
                            int endIndex = userPart.indexOf('!');
                            if (endIndex > 0) {
                                this.username = userPart.substring(0, endIndex);
                            }
                        }
                    }
                    
                    // Extract session token from authentication response if present
                    if (response.contains("Your session token:")) {
                        String[] parts = response.split("Your session token: ");
                        if (parts.length > 1) {
                            sessionToken = parts[1].trim();
                            saveSession();
                        }
                    }
                    
                    System.out.println(response);
                } else if (response.startsWith("AUTH_FAIL:")) {
                    System.out.println(response);
                    // If too many failed attempts, server will close the connection
                    if (response.contains("Too many failed")) {
                        shutdown();
                    }
                } else {
                    // Track room join/leave events
                    if (response.startsWith("You joined room:")) {
                        isInRoom.set(true);
                    } else if (response.startsWith("You left room:")) {
                        isInRoom.set(false);
                    }
                    
                    // Display the server message
                    System.out.println(response);
                }
            }
        } catch (SocketException e) {
            if (isRunning.get()) {
                System.err.println("Connection to server lost: " + e.getMessage());
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        } finally {
            if (isRunning.get()) {
                System.out.println("Server connection closed.");
                shutdown();
            }
        }
    }

    public void shutdown() {
        // Only execute shutdown once
        if (!isRunning.getAndSet(false)) {
            return;
        }

        // Close executor
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close resources
        closeResources();

        System.out.println("Disconnected from server. Goodbye!");
    }

    private void closeResources() {
        try {
            if (out != null) {
                out.println("/exit"); // Try to notify server before disconnecting
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Just log and continue with shutdown
            System.err.println("Error closing network resources: " + e.getMessage());
        }

        // Always close console reader last
        try {
            if (consoleIn != null) {
                consoleIn.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing console reader: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 8888;
        String clientId = "default";

        // Parse command-line arguments if provided
        if (args.length >= 1) {
            serverAddress = args[0];
        }
        if (args.length >= 2) {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 8888.");
            }
        }
        if (args.length >= 3) {
            clientId = args[2];
        }

        Client client = new Client(serverAddress, serverPort, clientId);
        client.start();
    }
}
