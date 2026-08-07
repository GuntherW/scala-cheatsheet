#!/usr/bin/env bash
# Build script für AWS Lambda Scala Native (Custom Runtime)
# Kompiliert Scala 3 -> native Linux x86_64 Binary via Docker (Amazon Linux 2023)
# Das erzeugte 'bootstrap' Binary läuft auf der provided.al2023 Lambda Runtime.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/dist"
ZIP_FILE="$SCRIPT_DIR/lambda.zip"

mkdir -p "$OUT_DIR"

echo "==> Baue Scala Native Binary via Docker (Amazon Linux 2023)..."

docker run --rm \
  -v "$SCRIPT_DIR":/workspace \
  -w /workspace \
  --platform linux/amd64 \
  amazonlinux:2023 \
  bash -c '
    set -euo pipefail

    echo "--- Installiere Abhängigkeiten..."
    dnf install -y --allowerasing \
      java-21-amazon-corretto-headless \
      clang \
      llvm \
      libstdc++-devel \
      libstdc++-static \
      zlib-devel \
      libcurl-devel \
      openssl-devel \
      curl \
      gzip \
      which \
      2>&1 | tail -3

    echo "--- Installiere Scala CLI..."
    curl -fL \
      https://github.com/VirtusLab/scala-cli/releases/latest/download/scala-cli-x86_64-pc-linux.gz \
      | gunzip -c > /usr/local/bin/scala-cli
    chmod +x /usr/local/bin/scala-cli
    echo "scala-cli: $(/usr/local/bin/scala-cli --version)"

    echo "--- Kompiliere handler.scala -> dist/bootstrap..."
    /usr/local/bin/scala-cli --power package handler.scala \
      --native \
      -o dist/bootstrap \
      --force \
      -J -Xmx2g

    echo "--- Binary Info:"
    file dist/bootstrap
    ls -lh dist/bootstrap
  '

echo "==> Erstelle deployment ZIP (bootstrap muss im Root liegen)..."
cd "$OUT_DIR"
zip "$ZIP_FILE" bootstrap

echo "==> Fertig: $ZIP_FILE"
echo "    Größe: $(du -sh "$ZIP_FILE" | cut -f1)"
