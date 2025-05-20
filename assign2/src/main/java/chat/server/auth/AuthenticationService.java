package chat.server.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuthenticationService {

    private final Map<String, String> userCredentials = new HashMap<>();
    private final ReadWriteLock credentialsLock = new ReentrantReadWriteLock();
    private final Map<String, Boolean> loggedInUsers = new HashMap<>();
    private final ReadWriteLock loggedInUsersLock = new ReentrantReadWriteLock();

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
                    credentialsLock.writeLock().lock();
                    try {
                        userCredentials.put(parts[0], parts[1]);
                    } finally {
                        credentialsLock.writeLock().unlock();
                    }
                }
            }
        }

        credentialsLock.readLock().lock();
        try {
            System.out.println("Loaded " + userCredentials.size() + " user(s) from " + filePath);
        } finally {
            credentialsLock.readLock().unlock();
        }
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
        credentialsLock.readLock().lock();
        boolean credentialsMatch;
        try {
            credentialsMatch = userCredentials.containsKey(username)
                    && userCredentials.get(username).equals(password);
        } finally {
            credentialsLock.readLock().unlock();
        }

        if (credentialsMatch) {
            // Check if user is already logged in
            loggedInUsersLock.readLock().lock();
            boolean alreadyLoggedIn;
            try {
                alreadyLoggedIn = loggedInUsers.getOrDefault(username, false);
            } finally {
                loggedInUsersLock.readLock().unlock();
            }

            if (alreadyLoggedIn) {
                System.out.println("User " + username + " is already logged in");
                return false;
            }

            // Mark user as logged in
            loggedInUsersLock.writeLock().lock();
            try {
                loggedInUsers.put(username, true);
            } finally {
                loggedInUsersLock.writeLock().unlock();
            }
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
        loggedInUsersLock.writeLock().lock();
        try {
            loggedInUsers.put(username, false);
        } finally {
            loggedInUsersLock.writeLock().unlock();
        }
    }

    /**
     * Checks if a user exists.
     *
     * @param username The username to check
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(String username) {
        credentialsLock.readLock().lock();
        try {
            return userCredentials.containsKey(username);
        } finally {
            credentialsLock.readLock().unlock();
        }
    }
}
