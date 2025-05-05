# Secure Concurrent Chat Application

A multi-threaded, TLS-enabled chat application with AI conversation capabilities, implemented in Java with virtual threads.

## Features

- **Secure Communication**: End-to-end TLS encryption for all client-server communication
- **Concurrent Architecture**: Built with Java's virtual threads for high scalability
- **User Authentication**: Username/password authentication
- **Multiple Chat Rooms**: Create and join different chat rooms
- **AI Conversation**: Create AI-powered rooms that respond to messages using Ollama
- **Graceful Error Handling**: Robust error handling and recovery mechanisms
- **Real-time Messaging**: Instant message delivery to all users in a room

## Prerequisites

- Java 21 or higher (required for virtual threads)
- Ollama (for AI functionality) - [Installation Guide](https://ollama.ai/download)
- OpenSSL (for certificate generation)
- Visual Studio Code (recommended IDE)

### Installing Java on Ubuntu

```bash
# Update package lists
sudo apt update

# Install OpenJDK 21
sudo apt install openjdk-21-jdk

# Verify installation
java --version
```

### Setting up Visual Studio Code

1. Download and install VSCode from [code.visualstudio.com](https://code.visualstudio.com/)
2. Install the following extensions:
   - Extension Pack for Java
   - Maven for Java
   - Test Runner for Java

```bash
# Install VSCode on Ubuntu
sudo snap install code --classic
```

### Running AI on Docker

To use Ollama in a Docker container for AI functionality:

```bash
# Pull and run Ollama Docker image
docker pull ollama/ollama
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama

# Pull a model (e.g., Llama 2)
docker exec -it ollama ollama pull llama2

# Verify Ollama server is running
curl http://localhost:11434/api/tags
```

Make sure the OllamaService.java file points to the correct Docker host address (typically `localhost:11434`) for API calls.

## Quick Start

### Generate SSL Certificates

Before starting the application, you need to generate SSL certificates:

```bash
./scripts/generate_certs.sh
```

### Start the Server

```bash
./scripts/run_server.sh [port]
```

Default port is 5000 if not specified.

### Start a Client

```bash
./scripts/run_client.sh [server_address] [port] [-C <client_id>]
```

Default server address is localhost and default port is 5000 if not specified.
The optional `-C` parameter allows specifying a client ID, which can be used for multiple clients on the same machine.

## Client Commands

Once connected to the server:

### Authentication
- `login <username> <password>` - Authenticate with the server
- `/logout` - Log out current user and authenticate as a different user

### Room Management
- `list` - List all available chat rooms
- `create <room_name>` - Create a new regular chat room
- `create-ai <room_name> <prompt>` - Create an AI-powered chat room with a specific system prompt
- `join <room_name>` - Join an existing chat room
- `leave` - Leave the current chat room

### Messaging
- Just type your message and press enter to send to the current room

### System Commands
- `help` - Display available commands
- `exit` - Disconnect from the server and exit

## Application Architecture

### Server Components
- **Server**: Main server class that accepts client connections
- **ClientHandler**: Manages individual client connections
- **Room**: Represents a chat room with message broadcasting
- **AuthenticationService**: Handles user authentication
- **OllamaService**: Provides AI functionality via Ollama API

### Client Components
- **Client**: Handles user input, server communication, and message display

## Security

- All communications are encrypted using TLS 1.3
- Passwords are validated server-side
- Custom trust stores and key stores are used for secure identification

## Configuration

### User Management
User credentials are stored in `resources/main/users.txt` in the format:
```
username:password
```

### SSL Configuration
- Server keystore: `resources/main/server.jks`
- Client truststore: `resources/main/client_truststore.jks`

## Troubleshooting

### Connection Issues
- Ensure the server is running
- Check firewall settings
- Verify port availability

### AI Room Issues
- Ensure Ollama is installed and running
- If using Docker:
  - Check if the container is running: `docker ps | grep ollama`
  - Restart if needed: `docker restart ollama`
- Check that the model specified in OllamaService is available locally
- Verify Docker port mapping if using containerized setup

### SSL Certificate Issues
- Regenerate certificates using the provided script
- Ensure keystores and truststores are properly configured

## Development

### Building from Source
```bash
javac -d bin src/main/java/chat/client/*.java src/main/java/chat/server/*.java src/main/java/chat/server/ai/*.java src/main/java/chat/server/auth/*.java
```

### Project Structure
```
├── src/main/java/chat/
│   ├── client/
│   │   └── Client.java
│   └── server/
│       ├── Server.java
│       ├── ClientHandler.java
│       ├── Room.java
│       ├── ai/
│       │   └── OllamaService.java
│       └── auth/
│           └── AuthenticationService.java
├── resources/
│   └── main/
│       ├── users.txt
│       ├── server.jks
│       └── client_truststore.jks
└── scripts/
    ├── generate_certs.sh
    ├── run_server.sh
    └── run_client.sh
```