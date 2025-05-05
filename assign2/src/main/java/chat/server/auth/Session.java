package chat.server.auth;

public class Session {
    private final String username;
    private final long expirationTime;

    public Session(String username, long expirationTime) {
        this.username = username;
        this.expirationTime = expirationTime;
    }

    public String getUsername() {
        return username;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
}
