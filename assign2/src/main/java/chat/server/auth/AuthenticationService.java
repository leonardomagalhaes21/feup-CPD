package chat.server.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationService {

    private final Map<String, String> userCredentials = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loggedInUsers = new ConcurrentHashMap<>();

    /**
     * Creates a new authentication service and loads users from the specified
     * file.
     *
     * @param userFilePath Path to the file containing user credentials in
     * format username:password
     * @throws IOException If the file cannot be read
     */
    public AuthenticationService(String userFilePath) throws IOException {
        loadUsers(userFilePath);
    }

    /**
     * Loads user credentials from a file.
     *
     * @param filePath Path to the file containing user credentials
     * @throws IOException If the file cannot be read
     */
    private void loadUsers(String filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
        }

        System.out.println("Loaded " + userCredentials.size() + " user(s) from " + filePath);
    }

    /**
     * Authenticates a user with the provided credentials.
     *
     * @param username The username
     * @param password The password
     * @return true if authentication is successful, false otherwise
     */
    public boolean authenticate(String username, String password) {
        // Check if user exists and password matches
        if (userCredentials.containsKey(username)
                && userCredentials.get(username).equals(password)) {

            // Check if user is already logged in
            if (loggedInUsers.getOrDefault(username, false)) {
                System.out.println("User " + username + " is already logged in");
                return false;
            }

            // Mark user as logged in
            loggedInUsers.put(username, true);
            return true;
        }

        return false;
    }

    /**
     * Logs out a user.
     *
     * @param username The username to log out
     */
    public void logout(String username) {
        loggedInUsers.put(username, false);
    }

    /**
     * Checks if a user exists.
     *
     * @param username The username to check
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(String username) {
        return userCredentials.containsKey(username);
    }
}
