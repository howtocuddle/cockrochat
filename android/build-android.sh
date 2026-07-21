#!/usr/bin/env bash
# Cross-compile mesh-core to the 4 Android ABIs and regenerate the UniFFI Kotlin binding.
# Run this whenever mesh-core changes, before `gradle assembleDebug`.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
CORE="$HERE/../mesh-core"
JNI="$HERE/app/src/main/jniLibs"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.2.12479018}"

cd "$CORE"

echo ">> cross-compiling libmesh_core.so for arm64-v8a / armeabi-v7a / x86_64 / x86 (min API 26)"
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o "$JNI" --platform 26 build --release

echo ">> generating Kotlin binding from host cdylib"
cargo build --release
cargo run --quiet --bin uniffi-bindgen -- generate \
    --library target/release/libmesh_core.so --language kotlin \
    --out-dir "$HERE/app/src/main/java" --no-format

echo ">> done. jniLibs:"
find "$JNI" -name '*.so' -printf '   %p\n'
