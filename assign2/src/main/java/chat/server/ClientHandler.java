package chat.server;

import chat.server.auth.AuthenticationService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler {

    private final Socket clientSocket;
    private final AuthenticationService authService;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean isAuthenticated = false;
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    public ClientHandler(Socket clientSocket, AuthenticationService authService) {
        this.clientSocket = clientSocket;
        this.authService = authService;
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

            String inputLine;
            // Read messages from the client and echo them back
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Message from " + username + ": " + inputLine);
                // Echo the message back to the client
                out.println(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            // If user was authenticated, log them out
            if (isAuthenticated && username != null) {
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
