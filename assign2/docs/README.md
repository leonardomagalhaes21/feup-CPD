# Concurrent TCP Chat Server

## Overview
This project implements a multi-user, room-based chat server and client in Java (SE 21 or later) using TCP/IP. It demonstrates core concepts of network programming and concurrent systems, featuring:
*   User authentication
*   Dynamic creation and joining of chat rooms
*   Real-time message broadcasting within rooms
*   Special AI-powered rooms that interact with a local LLM (Ollama) (Coming soon)
*   Robust concurrency handling using Java Virtual Threads and explicit `java.util.concurrent.locks` (Coming soon)
*   Secure communication using TLS/SSL (Coming soon)

## Current Implementation Status
* ✅ Phase 1: Basic Server & Client Connection
* ✅ Phase 2: User Authentication
* 🔄 Phase 3: Basic Room Management & Chat (In progress)
* ⏳ Phase 4: Concurrency Control (Not started)
* ⏳ Phase 5: AI Rooms (Not started)
* ⏳ Phase 6: Secure Communication (TLS/SSL) (Not started)

## Features
*   **Client-Server Architecture:** Standard TCP-based communication.
*   **Virtual Threads:** Server handles each client connection on a dedicated virtual thread for high scalability.
*   **User Authentication:** Users must log in with a username and password before participating.
*   **Chat Rooms:** Users can list available rooms, join existing ones, or create new ones.
*   **AI Integration:** Option to create "AI rooms" where a specified prompt and the conversation history are sent to a local Ollama instance (Coming soon).
*   **Explicit Locking:** Demonstrates manual concurrency control using `ReentrantReadWriteLock` (Coming soon).
*   **Secure Communication:** Uses TLS/SSL to encrypt communication (Coming soon).

## How to Run

**Prerequisites:**
*   Java Development Kit (JDK) SE 21 or later.
*   (Optional) Ollama installed and running locally if using AI rooms (for Phase 5).

### Using the Provided Scripts

The project includes convenient scripts for running the server and client:

**Running the Server:**
```bash
./scripts/run_server.sh [port] [users_file_path]
```
- Default port: 8888
- Default users file: resources/main/users.txt

**Running the Client:**
```bash
./scripts/run_client.sh [server_address] [port]
```
- Default server address: localhost
- Default port: 8888

### Manual Compilation and Running

**Compilation:**
```bash
# Navigate to the project root directory
javac -d out/production/assign2 src/main/java/chat/client/*.java src/main/java/chat/server/*.java src/main/java/chat/server/auth/*.java src/main/java/chat/server/ai/*.java
```

**Running the Server:**
```bash
# From the project root directory
java -cp out/production/assign2 chat.server.Server [port] [users_file_path]
```

**Running the Client:**
```bash
# From the project root directory
java -cp out/production/assign2 chat.client.Client [server_address] [port]
```

## Testing Instructions

### Phase 1: Basic Server & Client Connection
1. Start the server: `./scripts/run_server.sh`
2. Start one or more clients: `./scripts/run_client.sh`
3. Enter messages in client console - server will echo back each message.
4. Verify that multiple clients can connect and send/receive messages independently.

### Phase 2: User Authentication
1. Start the server: `./scripts/run_server.sh`
2. Start one or more clients: `./scripts/run_client.sh`
3. Test authentication with predefined users:
   - Valid login: `/login alice password123`
   - Invalid password: `/login alice wrongpassword`
   - Invalid username: `/login nonexistentuser password`
   - Already logged in: Try logging in with the same username in a second client
4. Verify that:
   - Only authenticated users can send messages
   - Failed login attempts are properly handled
   - After 3 failed attempts, connection is closed

### Phase 3: Basic Room Management & Chat (In Progress)
1. Start the server: `./scripts/run_server.sh`
2. Start multiple clients: `./scripts/run_client.sh` (at least 2)
3. Log in on each client with different credentials
4. Test room commands:
   - List available rooms: `/list`
   - Create a new room: `/create roomname`
   - Join an existing room: `/join roomname`
   - Leave the current room: `/leave`
5. Test messaging:
   - Send messages in a room and verify only users in that room receive them
   - Verify that join/leave notifications are broadcast to room members

## User Credentials for Testing

The system comes with predefined users for testing:
- Username: `alice`, Password: `password123`
- Username: `bob`, Password: `securepass`
- Username: `charlie`, Password: `qwerty`
- Username: `admin`, Password: `admin123`

## Client Commands Reference

**Authentication:**
- `/login <username> <password>`: Authenticate with the server

**Room Management:**
- `/list`: List all available chat rooms
- `/create <roomname>`: Create a new chat room
- `/join <roomname>`: Join an existing chat room
- `/leave`: Leave the current room

**Messaging:**
- Any text that doesn't start with `/` is sent as a message to the current room

## Project Structure

```
assign2/
├── docs/                    # Documentation files
│   ├── folder-structure.txt
│   ├── implementation-plan.md
│   └── README.md
├── resources/               # Resource files
│   └── main/
│       ├── client_truststore.jks  # For future SSL implementation
│       ├── server.jks             # For future SSL implementation 
│       └── users.txt              # User credentials for authentication
├── scripts/                 # Helper scripts
│   ├── generate_certs.sh    # For future SSL implementation
│   ├── run_client.sh        # Script to run client
│   └── run_server.sh        # Script to run server
└── src/                     # Source code
    └── main/
        └── java/
            └── chat/
                ├── client/         # Client-side code
                │   └── Client.java
                └── server/         # Server-side code
                    ├── ClientHandler.java
                    ├── Room.java
                    ├── Server.java
                    ├── ai/         # AI integration (Phase 5)
                    │   └── OllamaService.java
                    └── auth/       # Authentication (Phase 2)
                        └── AuthenticationService.java
```

## Logic Overview

The system uses a client-server model over TCP/IP.

1. The **Server** listens for incoming connections on a specified port.
2. Upon connection, the server creates a **Virtual Thread** to handle the client independently.
3. The client must first **authenticate**. The server verifies credentials against a predefined list.
4. Authenticated clients can **interact with rooms**. The server maintains a global Map of rooms.
5. Each **Room** object manages its own set of members and message history.
6. When a client sends a **message**, the server adds it to the room's history and broadcasts it to all room members.
7. Clients continuously listen for incoming messages from the server and display them.

## Future Enhancements

- **Phase 4:** Concurrency control using ReentrantReadWriteLock for thread-safe room operations
- **Phase 5:** AI-powered rooms with Ollama integration 
- **Phase 6:** Secure communication using TLS/SSL encryption
- **Phase 7:** Additional refinements and error handling enhancements