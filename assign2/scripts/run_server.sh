#!/bin/bash

# Check if Java is installed
if ! command -v java &> /dev/null || ! command -v javac &> /dev/null; then
    echo "Java JDK is not installed or not in your PATH."
    echo "Please install Java JDK using one of these commands:"
    echo "  - Ubuntu/Debian: sudo apt-get update && sudo apt-get install default-jdk"
    echo "  - Fedora/RHEL: sudo dnf install java-latest-openjdk-devel"
    echo "  - Arch Linux: sudo pacman -S jdk-openjdk"
    echo "After installation, run this script again."
    exit 1
fi

# Check Java version (need 21+ for virtual threads)
java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
if [ "$java_version" -lt 21 ]; then
    echo "Warning: Java version $java_version detected. Java 21+ is required for virtual threads."
    read -p "Do you want to continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Navigate to the project root
cd "$(dirname "$0")/.."

# Check if keystore exists
if [ ! -f "resources/main/server.jks" ]; then
    echo "Error: Server keystore not found at resources/main/server.jks"
    echo "Please run the generate_certs.sh script first."
    exit 1
fi

# Check if users file exists
USERS_FILE=${2:-"resources/main/users.txt"}
if [ ! -f "$USERS_FILE" ]; then
    echo "Error: Users file not found at $USERS_FILE"
    read -p "Do you want to create a sample users file? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Creating sample users file..."
        echo "admin:password" > "$USERS_FILE"
        echo "user1:password1" >> "$USERS_FILE"
        echo "user2:password2" >> "$USERS_FILE"
        echo "Sample users created: admin, user1, user2 (all with obvious passwords)"
    else
        echo "Please create a users file and try again."
        exit 1
    fi
fi

# Create output directory if it doesn't exist
mkdir -p out/production/assign2

# Compile the code
echo "Compiling server code..."
javac -d out/production/assign2 src/main/java/chat/server/*.java src/main/java/chat/server/auth/*.java src/main/java/chat/server/ai/*.java

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful. Starting server..."
    
    # Default port and users file
    PORT=${1:-8888}
    SSL_DEBUG=${3:-"false"}
    
    echo "Using port: $PORT"
    echo "Using users file: $USERS_FILE"
    
    # Check if Ollama is available for AI rooms
    if command -v curl &> /dev/null && curl -s --connect-timeout 2 "http://localhost:11434/api/tags" &> /dev/null; then
        echo "Ollama service detected - AI room feature is available"
    else
        echo "Warning: Ollama service not detected - AI room feature will not work"
        echo "To use AI rooms, please install Ollama from https://ollama.com/"
    fi
    
    # Set SSL debug options if requested
    SSL_OPTS=""
    if [ "$SSL_DEBUG" = "true" ]; then
        echo "SSL debugging enabled"
        SSL_OPTS="-Djavax.net.debug=ssl,handshake"
    fi
    
    # Run the server with arguments (port number and users file)
    java $SSL_OPTS -cp out/production/assign2 chat.server.Server $PORT $USERS_FILE
else
    echo "Compilation failed. Please fix the errors and try again."
fi