# Concurrent TCP Chat Server

## Overview
This project implements a multi-user, room-based chat server and client in Java (SE 21 or later) using TCP/IP. It demonstrates core concepts of network programming and concurrent systems, featuring:
*   User authentication
*   Dynamic creation and joining of chat rooms
*   Real-time message broadcasting within rooms
*   Special AI-powered rooms that interact with a local LLM (Ollama)
*   Robust concurrency handling using Java Virtual Threads and explicit `java.util.concurrent.locks` (without relying on concurrent collections).
*   Secure communication using TLS/SSL.

## Features
*   **Client-Server Architecture:** Standard TCP-based communication.
*   **Virtual Threads:** Server handles each client connection on a dedicated virtual thread for high scalability.
*   **User Authentication:** Users must log in with a username and password before participating.
*   **Chat Rooms:** Users can list available rooms, join existing ones, or create new ones.
*   **AI Integration:** Option to create "AI rooms" where a specified prompt and the conversation history are sent to a local Ollama instance, with the AI's response posted back to the room.
*   **Explicit Locking:** Demonstrates manual concurrency control using `ReentrantReadWriteLock` to protect shared data (room list and individual room states).
*   **Secure Communication:** Uses TLS/SSL to encrypt communication between client and server.

## How to Run

**Prerequisites:**
*   Java Development Kit (JDK) SE 21 or later.
*   (Optional) Ollama installed and running locally if using AI rooms. Ensure the server code points to the correct Ollama API endpoint.
*   A Java Keystore (`.jks`) file for the server's SSL certificate (e.g., `server.jks`). You might need to create a trust store for the client or configure it to trust the self-signed certificate.

**Compilation:**
```bash
# Navigate to the source directory
javac *.java
# Or use your preferred build tool (Maven/Gradle) if structure allows
```

**Running the Server:**

`# Example: java Server <port> <path_to_user_file> <path_to_keystore> <keystore_password>
java Server 8080 users.txt server.jks password123`

**content_copydownload**Use code [**with caution**](https://support.google.com/legal/answer/13505487).Bash

- <port>: The port number the server will listen on (e.g., 8080).
- <path_to_user_file>: Path to a file containing user credentials (e.g., username:password per line).
- <path_to_keystore>: Path to the server's JKS keystore file.
- <keystore_password>: Password for the keystore.

**Running the Client:**

`# Example: java Client <server_ip> <server_port> [path_to_truststore] [truststore_password]
java Client localhost 8080 client_truststore.jks password456
# Or if trusting any server certificate (less secure, for testing):
# java Client localhost 8080`

**content_copydownload**Use code [**with caution**](https://support.google.com/legal/answer/13505487).Bash

- <server_ip>: The IP address or hostname of the server (e.g., localhost).
- <server_port>: The port the server is running on.
- [path_to_truststore], [truststore_password]: Optional paths/passwords if using a specific trust store for SSL. The client might need configuration to trust the server's certificate.

## Input/Output

**Client Commands:**

- /login <username> <password>: Authenticate with the server.
- /list: List available chat rooms.
- /join <room_name>: Join an existing chat room.
- /create <room_name>: Create a new regular chat room.
- /create <room_name> <AI_prompt>: Create a new AI-powered room with the given prompt.
- /leave: Leave the current chat room.
- Any other text is treated as a message to be sent to the current room.

**Server Output (to Clients):**

- Prompts for login/commands.
- Messages from other users in the format: Username: Message text
- System messages: [Username enters the room], [Username leaves the room]
- Bot messages in AI rooms: Bot: AI response text
- Room lists, command confirmations, and error messages.

## Logic Overview

The system uses a client-server model over TCP/IP with TLS/SSL encryption.

1. The **Server** listens for incoming connections on a specified port using an SSLServerSocket.
2. Upon connection, the server creates a **Virtual Thread** using Executors.newVirtualThreadPerTaskExecutor() to handle the client independently.
3. The client must first **authenticate**. The server verifies credentials against a predefined list (loaded from a file).
4. Authenticated clients can **interact with rooms**. The server maintains a global Map of rooms, protected by a ReentrantReadWriteLock for safe concurrent access (listing, creating, finding rooms).
5. Each **Room** object manages its own set of members and message history, protected by its own ReentrantReadWriteLock.
6. When a client sends a **message**, the server adds it to the room's history (write lock) and then broadcasts it to all members (read lock used briefly to get member list snapshot).
7. In **AI rooms**, new messages trigger an asynchronous HTTP request to a configured Ollama endpoint (outside of the room lock). The response is then added to the room as a message from "Bot" (requiring the write lock again).
8. Clients continuously listen for incoming messages from the server and display them.