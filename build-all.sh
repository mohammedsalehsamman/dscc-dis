#!/usr/bin/env sh
# Builds all four modules with the Maven Wrapper (downloads Maven on first use; needs JDK 17+).
# Output: <module>/target/<module>.jar plus its libraries in <module>/target/lib
set -e
cd "$(dirname "$0")"
./mvnw -B -q package -DskipTests
echo "Build OK"
