# Secure Concurrent Chat Application

A secure, multi-threaded chat application implemented in Java, featuring virtual threads, TLS/SSL encryption, user authentication, chat rooms, and AI integration.

## Features

- **Secure Communication**: End-to-end encryption using TLS/SSL
- **User Authentication**: Login system with username/password verification
- **Chat Rooms**: Create and join multiple chat rooms
- **Concurrent Design**: Utilizes Java virtual threads for high scalability
- **AI Integration**: Special rooms with AI assistant capabilities via Ollama
- **Thread-safe Implementation**: Proper concurrency control using read-write locks

## Requirements

- **Java 21+** (for virtual threads support)
- **Ollama** (optional, for AI room functionality)

## Quick Start

### 1. Generate SSL Certificates

Before running the application, generate the necessary SSL certificates:

```bash
./scripts/generate_certs.sh
```

This creates a server keystore and client truststore for secure communication.

### 2. Start the Server

Run the server with default settings:

```bash
./scripts/run_server.sh
```

Optional parameters:
- Port number (default: 8888)
- Users file path (default: resources/main/users.txt)
- Enable SSL debug (true/false)

Example with all parameters:
```bash
./scripts/run_server.sh 9999 /path/to/users.txt true
```

### 3. Connect with Clients

Start one or more clients:

```bash
./scripts/run_client.sh
```

Optional parameters:
- Server address (default: localhost)
- Port number (default: 8888)
- Enable SSL debug (true/false)

Example with all parameters:
```bash
./scripts/run_client.sh chat.example.com 9999 true
```

## User Commands

Once connected to the server, clients can use these commands:

- `/login <username> <password>` - Authenticate with the server
- `/list` - Show available chat rooms
- `/create <room_name>` - Create a regular chat room
- `/create <room_name> <ai_prompt>` - Create an AI-assisted room
- `/join <room_name>` - Join a chat room
- `/leave` - Leave current chat room
- `/help` - Show available commands
- `/exit` - Disconnect from server

## AI Room Setup

To use AI rooms, [Ollama](https://ollama.com/) must be running on the server.

1. Install Ollama following instructions at [ollama.com](https://ollama.com/)
2. Pull a model: `ollama pull llama2` (or another model)
3. Ensure Ollama is running and accessible at http://localhost:11434/

## Security Notes

- The application uses self-signed certificates for TLS
- In production, replace with proper CA-signed certificates
- Default passwords are stored in plaintext; consider implementing proper hashing

## Error Handling

The application includes robust error handling for:
- Network disconnections
- Authentication failures
- Invalid commands
- Resource unavailability
- Concurrent access issues

## Architecture Overview

- **Server**: Main application server using virtual threads
- **ClientHandler**: Manages individual client connections
- **Room**: Chat room implementation with concurrency control
- **AuthenticationService**: User authentication and validation
- **OllamaService**: Integration with Ollama for AI capabilities

## Development Plan

This application was developed following a phased approach:
1. Basic TCP communication
2. User authentication
3. Room management with basic chat
4. Concurrency control with read-write locks
5. AI room integration
6. TLS/SSL implementation
7. Final refinements and testing

For detailed implementation notes, see [implementation-plan.md](implementation-plan.md).

## Troubleshooting

- **Connection issues**: Verify server is running and firewall settings
- **Authentication failures**: Check username/password in users.txt
- **SSL errors**: Ensure certificates are generated correctly
- **AI room not responding**: Check Ollama service status

## License

This project is part of the CPD (Parallel and Distributed Computing) course at FEUP.

## Contributors

Group 15:
- [Student 1]
- [Student 2]
- [Student 3]