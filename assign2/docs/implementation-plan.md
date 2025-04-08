# Step-by-Step Implementation Plan: Concurrent TCP Chat Server

This plan outlines the phases to build the chat system, focusing on iterative development and testing.

**Phase 1: Basic Server & Client Connection (No Auth/Rooms/TLS)**

*   **Goal:** Establish basic TCP communication between server and client using virtual threads.
*   **Steps:**
    1.  **Server:** Create `Server.java`. Implement `main` method to create a `ServerSocket` listening on a specified port.
    2.  **Server:** Create an `ExecutorService` using `Executors.newVirtualThreadPerTaskExecutor()`.
    3.  **Server:** In a loop, accept client connections (`serverSocket.accept()`). For each accepted `Socket`, submit a new handler task (e.g., a `Runnable` or `Callable`) to the executor service.
    4.  **Server Handler Task:** Implement basic handler logic: receive a line of text from the client's input stream, print it to the server console, send it back to the client's output stream. Handle basic I/O errors.
    5.  **Client:** Create `Client.java`. Implement `main` method to connect to the server's IP and port using a `Socket`.
    6.  **Client:** Implement logic to read user input from the console, send it to the server, read the response from the server, and print it to the console.
*   **Testing:**
    *   Run the server.
    *   Run multiple client instances.
    *   Verify each client can send a message and receive the exact same message back (echo).
    *   Verify clients operate independently.

**Phase 2: User Authentication**

*   **Goal:** Implement user login before allowing further interaction.
*   **Steps:**
    1.  **Server:** Define a simple user store (e.g., `Map<String, String>` for username -> password). Load this from a file specified by a command-line argument at server startup.
    2.  **Server Handler:** Modify the start of the handler task. Expect `/login <user> <pass>` command from client first.
    3.  **Server Handler:** Parse login command, check credentials against the user store. Send a success ("AUTH_OK") or failure ("AUTH_FAIL") message back to the client. Close connection on failure after a few attempts?
    4.  **Client:** Prompt user for `/login` command. Send command to server.
    5.  **Client:** Wait for server response ("AUTH_OK" or "AUTH_FAIL"). Display status to user. Only proceed to next phase if authentication is successful.
*   **Testing:**
    *   Test valid username/password combinations.
    *   Test invalid username/password combinations.
    *   Test sending other commands before login (should be rejected).
    *   Test concurrent login attempts from multiple clients.

**Phase 3: Basic Room Management & Chat (No Concurrency Control Yet)**

*   **Goal:** Allow users to create, list, join rooms and exchange messages within them. Use standard (non-thread-safe) collections for now.
*   **Steps:**
    1.  **Shared:** Define `ClientHandler.java`. This class will manage the state for a connected, authenticated client (socket, streams, username, current room reference). Modify the server to create and use `ClientHandler` instances after successful authentication.
    2.  **Server:** Define `Room.java`. Include `String roomName`, `Set<ClientHandler> members` (`HashSet`), `List<String> messageHistory` (`ArrayList`).
    3.  **Server:** Implement global room storage: `Map<String, Room> rooms = new HashMap<>();` in the `Server` class.
    4.  **Server Handler:** Implement logic for client commands:
        *   `/list`: Return a list of keys from the `rooms` map.
        *   `/create <name>`: Create a new `Room` object, add it to the `rooms` map. Handle name collisions.
        *   `/join <name>`: Find the `Room`. Add the client's `ClientHandler` to the `Room.members`. Set the `currentRoom` in `ClientHandler`. Broadcast "[User] enters" message. Send some recent history?
        *   `/leave`: Remove `ClientHandler` from current `Room.members`. Broadcast "[User] leaves". Clear `currentRoom` in `ClientHandler`.
        *   Message Text: If client has a `currentRoom`, format message (`username: text`), add to `Room.messageHistory`, iterate through `Room.members`, and send the formatted message to each `ClientHandler`'s output stream (except the sender).
    5.  **Client:** Implement sending commands (`/list`, `/create`, `/join`, `/leave`) and regular messages. Implement receiving and displaying messages from the server (might need a separate reading thread on the client).
*   **Testing:**
    *   Create rooms. List rooms. Join rooms. Send messages.
    *   Verify messages are only seen by users in the same room.
    *   Verify join/leave notifications work.
    *   Test joining a non-existent room. Test creating a room that already exists.

**Phase 4: Concurrency Control**

*   **Goal:** Make room management and messaging thread-safe using explicit locks.
*   **Steps:**
    1.  **Server:** Add a `ReentrantReadWriteLock` instance for the global `rooms` map. Protect *all* reads (get, list) with the read lock and *all* writes (create) with the write lock.
    2.  **Room:** Add a `ReentrantReadWriteLock` instance to each `Room` object.
    3.  **Room:** Refactor `addUser`, `removeUser`, `addMessage` methods to acquire the room's *write lock* at the beginning and release it in a `finally` block.
    4.  **Room:** Refactor methods like `getMembers()`, `getMessageHistory()` (if needed externally) to use the room's *read lock*.
    5.  **Server Handler (Broadcasting):** When broadcasting a message:
        *   Acquire the room's *read lock*.
        *   Get a *copy* (snapshot) of the `members` set (`new HashSet<>(currentRoom.getMembers())`).
        *   Release the room's *read lock*.
        *   Iterate over the *copy* to send messages.
*   **Testing:**
    *   Perform stress tests: many clients connecting, creating, joining, leaving, and sending messages concurrently.
    *   Look for signs of race conditions (e.g., inconsistent user lists, missed messages, exceptions).
    *   Look for deadlocks (system hangs). Use thread dumps (`jstack`) if necessary.

**Phase 5: AI Rooms**

*   **Goal:** Implement special rooms that interact with Ollama.
*   **Steps:**
    1.  **Room:** Add `boolean isAiRoom` and `String aiPrompt` fields.
    2.  **Server Handler:** Modify `/create` command logic to accept an optional prompt. If prompt is present, create an AI room (`isAiRoom=true`, store prompt).
    3.  **Server:** Implement a helper method/class for calling Ollama. Use `java.net.http.HttpClient` for making POST requests to the Ollama API endpoint (e.g., `http://localhost:11434/api/generate`). Structure the request payload with the prompt and message history.
    4.  **Room:** Modify `addMessage`:
        *   Acquire write lock.
        *   Add user message to history.
        *   If `isAiRoom` is true:
            *   Get necessary history and prompt (still under lock).
            *   Release write lock.
            *   Make the *asynchronous* HTTP call to Ollama (e.g., using `httpClient.sendAsync`).
            *   In the callback/future completion stage for the HTTP response:
                *   Parse the AI response.
                *   Format it as "Bot: response".
                *   Re-acquire the room's write lock.
                *   Add the Bot message to history.
                *   Broadcast the Bot message (using the read lock snapshot technique).
                *   Release the write lock.
*   **Testing:**
    *   Create an AI room with a prompt. Send messages. Verify Bot responses appear correctly.
    *   Verify non-AI rooms are unaffected.
    *   Test behavior when Ollama is slow or unavailable (e.g., timeouts, error handling).

**Phase 6: Secure Communication (TLS/SSL)**

*   **Goal:** Encrypt client-server communication.
*   **Steps:**
    1.  **Keystore:** Generate a Java Keystore (JKS) file (`server.jks`) containing a self-signed certificate using `keytool`.
    2.  **Server:** Modify server startup:
        *   Use `SSLServerSocketFactory` obtained via `SSLContext`.
        *   Configure `SSLContext` to use the `server.jks` keystore.
        *   Create `SSLServerSocket` instead of `ServerSocket`.
    3.  **Client:** Modify client connection:
        *   Use `SSLSocketFactory` obtained via `SSLContext`.
        *   Configure `SSLContext` (critically, the `TrustManager`) to trust the server's certificate. This might involve loading a trust store (`client_truststore.jks`) containing the server's public key or a custom `TrustManager` (especially for self-signed certs during development).
        *   Create `SSLSocket` instead of `Socket`.
*   **Testing:**
    *   Verify connections are established successfully.
    *   Use tools like Wireshark (if possible) or SSL debug flags (`-Djavax.net.debug=ssl,handshake`) to confirm TLS handshake and encrypted traffic.
    *   Retest all core chat functionalities over the TLS connection.

**Phase 7: Refinement & Final Testing**

*   **Goal:** Polish the application, add robust error handling, and perform final testing.
*   **Steps:**
    1.  **Error Handling:** Improve error handling throughout (invalid commands, network exceptions, file I/O errors, abrupt disconnects - ensure disconnected clients are removed from rooms).
    2.  **Graceful Shutdown:** Implement server shutdown logic (close server socket, shut down executor service, close client sockets). Implement client exit logic.
    3.  **README:** Write the final `README.md` with clear instructions.
    4.  **Final Testing:** Test all features thoroughly, including edge cases identified earlier (slow clients, empty rooms, concurrent actions, boundary conditions).
*   **Testing:** Comprehensive testing of all features, error conditions, and edge cases. Code review.