#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p vendor
if [ ! -d vendor/llama.cpp ]; then git clone --depth 1 --branch b6323 https://github.com/ggml-org/llama.cpp.git vendor/llama.cpp; fi
# Pinned release; fail rather than silently use a different native ABI.
[ "$(git -C vendor/llama.cpp describe --tags --exact-match)" = b6323 ]
git -C vendor/llama.cpp rev-parse HEAD > vendor/llama-commit.txt
printf 'Install NDK 26.3.11579264 and CMake 3.22.1 in Android Studio, then run ./gradlew :app:assembleDebug\n'
