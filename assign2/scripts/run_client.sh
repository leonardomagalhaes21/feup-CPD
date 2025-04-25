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
    echo "Warning: Java version $java_version detected. Java 21+ is recommended for virtual threads."
    read -p "Do you want to continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Navigate to the project root
cd "$(dirname "$0")/.."

# Check if truststore exists
if [ ! -f "resources/main/client_truststore.jks" ]; then
    echo "Error: Client truststore not found at resources/main/client_truststore.jks"
    echo "Please run the generate_certs.sh script first."
    exit 1
fi

# Create output directory if it doesn't exist
mkdir -p out/production/assign2

# Compile the code
echo "Compiling client code..."
javac -d out/production/assign2 src/main/java/chat/client/*.java

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
    
    # Run the client with arguments
    java $SSL_OPTS -cp out/production/assign2 chat.client.Client $SERVER $PORT
else
    echo "Compilation failed. Please fix the errors and try again."
fi