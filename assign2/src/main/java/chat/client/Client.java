package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleIn;
    private boolean isRunning;
    private boolean isAuthenticated = false;
    private String username;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.consoleIn = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        try {
            // Connect to the server
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isRunning = true;

            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);

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
        } finally {
            closeResources();
        }
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
                    System.out.println("Server: " + response);
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
