#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")/.."

# Check if keytool is available (part of JDK)
if ! command -v keytool &> /dev/null; then
    echo "Error: keytool not found. Please ensure Java JDK is installed and in your PATH."
    exit 1
fi

# Create resources directory if it doesn't exist
mkdir -p resources/main

# Configuration variables
KEYSTORE="resources/main/server.jks"
TRUSTSTORE="resources/main/client_truststore.jks"
KEYSTORE_PASSWORD="password"
TRUSTSTORE_PASSWORD="password"
CERT_ALIAS="serveralias"
VALIDITY_DAYS=365
CERT_DNAME="CN=localhost, OU=G15, O=FEUP, L=Porto, ST=Porto, C=PT"

echo "Generating certificates for secure communication..."

# Delete existing keystores and truststores
rm -f "$KEYSTORE" "$TRUSTSTORE"

# Generate the server keystore with a new keypair
echo "Creating server keystore..."
keytool -genkeypair \
  -alias "$CERT_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -dname "$CERT_DNAME" \
  -keystore "$KEYSTORE" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEYSTORE_PASSWORD" \
  -deststoretype pkcs12

# Export the server certificate
echo "Exporting server certificate..."
keytool -exportcert \
  -alias "$CERT_ALIAS" \
  -file resources/main/server.cert \
  -keystore "$KEYSTORE" \
  -storepass "$KEYSTORE_PASSWORD"

# Create client truststore and import server certificate
echo "Creating client truststore..."
keytool -importcert \
  -alias "$CERT_ALIAS" \
  -file resources/main/server.cert \
  -keystore "$TRUSTSTORE" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

# Clean up temporary certificate file
rm resources/main/server.cert

echo "Certificate generation complete!"
echo "  - Server keystore: $KEYSTORE"
echo "  - Client truststore: $TRUSTSTORE"
echo ""
echo "For testing purposes:"
echo "  - Keystore password: $KEYSTORE_PASSWORD"
echo "  - Truststore password: $TRUSTSTORE_PASSWORD"
echo ""
echo "Note: In a production environment, use strong, unique passwords and proper certificate authorities."