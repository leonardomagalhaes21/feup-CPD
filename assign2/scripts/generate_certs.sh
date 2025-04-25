#!/bin/bash

# Set script to exit on any error
set -e

# Define variables
KEYSTORE_DIR="../resources/main"
SERVER_KEYSTORE="$KEYSTORE_DIR/server.jks"
CLIENT_TRUSTSTORE="$KEYSTORE_DIR/client_truststore.jks"
KEY_PASSWORD="password"
CERT_VALIDITY=365

# Make sure the directory exists
mkdir -p $KEYSTORE_DIR

# Clean up any existing files
rm -f $SERVER_KEYSTORE $CLIENT_TRUSTSTORE

echo "======================================================"
echo "Generating server certificate and keystore"
echo "======================================================"

# Generate the server's keystore (with a self-signed certificate)
keytool -genkeypair \
    -alias chatserver \
    -keyalg RSA \
    -keysize 2048 \
    -validity $CERT_VALIDITY \
    -keystore $SERVER_KEYSTORE \
    -storepass $KEY_PASSWORD \
    -keypass $KEY_PASSWORD \
    -dname "CN=ChatServer, OU=CPD, O=FEUP, L=Porto, S=Porto, C=PT"

echo "Server keystore generated: $SERVER_KEYSTORE"

echo "======================================================"
echo "Exporting server's public certificate"
echo "======================================================"

# Export the server's public certificate
keytool -exportcert \
    -alias chatserver \
    -keystore $SERVER_KEYSTORE \
    -storepass $KEY_PASSWORD \
    -file "$KEYSTORE_DIR/server_cert.cer"

echo "Server certificate exported: $KEYSTORE_DIR/server_cert.cer"

echo "======================================================"
echo "Creating client's truststore with server's certificate"
echo "======================================================"

# Import the server's certificate into the client's truststore
keytool -importcert \
    -alias chatserver \
    -file "$KEYSTORE_DIR/server_cert.cer" \
    -keystore $CLIENT_TRUSTSTORE \
    -storepass $KEY_PASSWORD \
    -noprompt

echo "Client truststore created: $CLIENT_TRUSTSTORE"

# Clean up the temporary certificate file
rm -f "$KEYSTORE_DIR/server_cert.cer"

echo "======================================================"
echo "Certificate generation complete."
echo "Server keystore: $SERVER_KEYSTORE"
echo "Client truststore: $CLIENT_TRUSTSTORE"
echo "Password for both stores: $KEY_PASSWORD"
echo "======================================================"

# Make the script executable
chmod +x $0

echo "You can now run the secure server and client using the provided scripts."