#!/usr/bin/env bash
# Build script für AWS Lambda Scala Native (Custom Runtime)
# Kompiliert Scala 3 -> native Linux x86_64 Binary lokal via scala-cli.
#
# Voraussetzungen (einmalig installieren):
#   sudo apt install -y clang libcurl4-openssl-dev libidn2-dev zlib1g-dev
#   # Java 17+ und scala-cli müssen ebenfalls installiert sein

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/dist"
ZIP_FILE="$SCRIPT_DIR/lambda.zip"

mkdir -p "$OUT_DIR"

echo "==> Kompiliere handler.scala -> dist/bootstrap..."
scala-cli --power package "$SCRIPT_DIR/handler.scala" \
  --native \
  -o "$OUT_DIR/bootstrap" \
  --force

echo "==> Binary Info:"
file "$OUT_DIR/bootstrap"
ls -lh "$OUT_DIR/bootstrap"

echo "==> Erstelle deployment ZIP (bootstrap muss im Root liegen)..."
cd "$OUT_DIR"
zip "$ZIP_FILE" bootstrap

echo "==> Fertig: $ZIP_FILE"
echo "    Größe: $(du -sh "$ZIP_FILE" | cut -f1)"
