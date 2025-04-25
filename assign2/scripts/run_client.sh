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

# Navigate to the project root
cd "$(dirname "$0")/.."

# Create output directory if it doesn't exist
mkdir -p out/production/assign2

# Compile the code
javac -d out/production/assign2 src/main/java/chat/client/*.java src/main/java/chat/server/*.java src/main/java/chat/server/auth/*.java src/main/java/chat/server/ai/*.java

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful. Starting client..."
    
    # Default server address and port
    SERVER=${1:-"localhost"}
    PORT=${2:-8888}
    SSL_DEBUG=${3:-"false"}
    
    echo "Connecting to: $SERVER:$PORT"
    
    # Set SSL debug options if requested
    SSL_OPTS=""
    if [ "$SSL_DEBUG" = "true" ]; then
        echo "SSL debugging enabled"
        SSL_OPTS="-Djavax.net.debug=ssl,handshake"
    fi
    
    # Run the client with arguments (server address and port)
    java $SSL_OPTS -cp out/production/assign2 chat.client.Client $SERVER $PORT
else
    echo "Compilation failed. Please fix the errors and try again."
fi