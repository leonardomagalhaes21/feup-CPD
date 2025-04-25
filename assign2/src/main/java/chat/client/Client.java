package chat.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private boolean isRunning;
    private boolean isAuthenticated = false;
    private String username;

    // SSL configuration
    private static final String TRUSTSTORE_PATH = "resources/main/client_truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "password";
    private static final String SSL_PROTOCOL = "TLS";

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.consoleIn = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        try {
            // Connect to the server using SSL
            socket = createSSLSocket();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isRunning = true;

            System.out.println("Connected securely to server at " + serverAddress + ":" + serverPort);

            // Start a virtual thread to read server responses
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            executor.submit(this::readServerResponses);

            // Wait for initial server welcome message
            Thread.sleep(100);

            // Authentication loop
            while (isRunning && !isAuthenticated) {
                System.out.print("Please login (/login username password): ");
                String loginCommand = consoleIn.readLine();

                // Validate login command format
                if (!loginCommand.startsWith("/login ")) {
                    System.out.println("Invalid command format. Use: /login username password");
                    continue;
                }

                // Send login command to server
                out.println(loginCommand);

                // Username is everything after "/login " and before the next space
                String[] parts = loginCommand.split("\\s+", 3);
                if (parts.length >= 2) {
                    this.username = parts[1];
                }

                // Wait for authentication response (handled in readServerResponses)
                // Sleep a bit to allow response to be processed
                Thread.sleep(500);
            }

            // Main thread reads user input and sends to server if authenticated
            if (isAuthenticated) {
                System.out.println("You are now logged in as " + username + ". You can start chatting.");
                String userInput;
                while (isRunning && (userInput = consoleIn.readLine()) != null) {
                    out.println(userInput);
                }
            }

            executor.shutdown();
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Client interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("SSL error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    /**
     * Creates an SSL socket with the appropriate SSL configuration.
     */
    private SSLSocket createSSLSocket() throws Exception {
        // Load the truststore that contains the trusted certificates
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(TRUSTSTORE_PATH), TRUSTSTORE_PASSWORD.toCharArray());

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

        // Configure SSL parameters if needed
        // For example, you might want to specify which cipher suites or protocols are enabled
        // Begin the SSL handshake
        sslSocket.startHandshake();

        return sslSocket;
    }

    private void readServerResponses() {
        try {
            String response;
            while (isRunning && (response = in.readLine()) != null) {
                // Check for authentication responses
                if (response.startsWith("AUTH_OK:")) {
                    isAuthenticated = true;
                    System.out.println(response);
                } else if (response.startsWith("AUTH_FAIL:")) {
                    System.out.println(response);
                    // If too many failed attempts, server will close the connection
                    if (response.contains("Too many failed")) {
                        isRunning = false;
                    }
                } else {
                    // Display the server message
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        }
        isRunning = false;
    }

    private void closeResources() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
            if (consoleIn != null) {
                consoleIn.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 8888;

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

        Client client = new Client(serverAddress, serverPort);
        client.start();
    }
}
