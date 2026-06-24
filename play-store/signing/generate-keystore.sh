#!/bin/bash
# AI File Organizer v1.1 – Release Keystore
# Usage: bash generate-keystore.sh
set -e
KEYSTORE_FILE="ai-organizer-release.jks"
ALIAS="aiorganizer"
echo "=== AI File Organizer v1.1 – Release Keystore ==="
read -s -p "Keystore password: " STORE_PASS; echo
read -s -p "Key password [ENTER=same]: " KEY_PASS; echo
[ -z "$KEY_PASS" ] && KEY_PASS=$STORE_PASS
keytool -genkeypair -v -keystore $KEYSTORE_FILE -alias $ALIAS -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -dname "CN=Niumination, OU=Mobile, O=Niumination, L=Jakarta, ST=DKI Jakarta, C=ID"
echo ""
echo "✓ $KEYSTORE_FILE created"
echo ""
echo "GitHub Secrets → Settings → Secrets → Actions :"
echo "SIGNING_KEYSTORE_BASE64=$(base64 -w 0 $KEYSTORE_FILE | head -c 80)..."
echo "SIGNING_STORE_PASSWORD=$STORE_PASS"
echo "SIGNING_KEY_ALIAS=$ALIAS"
echo "SIGNING_KEY_PASSWORD=$KEY_PASS"
echo ""
keytool -export -rfc -alias $ALIAS -file upload_certificate.pem -keystore $KEYSTORE_FILE -storepass "$STORE_PASS"
echo "upload_certificate.pem saved – for Play Console App Signing"
