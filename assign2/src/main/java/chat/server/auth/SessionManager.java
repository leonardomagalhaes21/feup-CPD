package chat.server.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionManager {

    private static final long TOKEN_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(30); // 30 minutes
    private final Map<String, Session> sessions = new HashMap<>();
    private final ReadWriteLock sessionsLock = new ReentrantReadWriteLock();

    public String createSession(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String token = UUID.randomUUID().toString();
        long expirationTime = System.currentTimeMillis() + TOKEN_EXPIRATION_TIME;

        sessionsLock.writeLock().lock();
        try {
            sessions.put(token, new Session(username, expirationTime));
        } finally {
            sessionsLock.writeLock().unlock();
        }

        return token;
    }

    public String validateSession(String token) {
        if (token == null) {
            return null;
        }

        sessionsLock.readLock().lock();
        try {
            Session session = sessions.get(token);
            if (session != null && !session.isExpired()) {
                return session.getUsername();
            }
            return null;
        } finally {
            sessionsLock.readLock().unlock();
        }
    }

    public void invalidateSession(String token) {
        if (token == null) {
            return;
        }

        sessionsLock.writeLock().lock();
        try {
            sessions.remove(token);
        } finally {
            sessionsLock.writeLock().unlock();
        }
    }
}