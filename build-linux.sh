#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Building Eara ASMR Player for Linux..."
./gradlew :desktopApp:packageDeb
echo "Linux DEB package is ready under desktopApp/build/compose/binaries/main/deb/"
