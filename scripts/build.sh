#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then gradle wrapper --gradle-version 8.7; fi
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
