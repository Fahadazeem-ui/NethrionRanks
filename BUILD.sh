#!/bin/bash
# NethrionRanks 1.1.0 — Build Script
# Requirements: JDK 21+, Maven 3.8+
# Usage: ./BUILD.sh

set -e
echo "=================================================="
echo "  NethrionRanks 1.1.0 — Building..."
echo "=================================================="

# Install original JAR to local Maven repo (needed for system scope)
mvn install:install-file \
  -Dfile=libs/NethrionRanks-original.jar \
  -DgroupId=com.nethrion \
  -DartifactId=NethrionRanks-original \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -q

# Build
mvn clean package -q

echo ""
echo "✅ Build successful!"
echo "📦 Output: target/NethrionRanks-1.1.0.jar"
echo ""
echo "Drop that file in your server's /plugins/ folder."
echo "Remove the old NethrionRanks jar first."
echo "=================================================="
