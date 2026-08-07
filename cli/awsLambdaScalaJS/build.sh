#!/usr/bin/env bash
# Build script for AWS Lambda ScalaJS
# Compiles Scala 3 -> JavaScript (CommonJS) and packages it as a ZIP for deployment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/dist"
ZIP_FILE="$SCRIPT_DIR/lambda.zip"

echo "==> Building Scala.js bundle..."
scala-cli package "$SCRIPT_DIR/handler.scala" \
  --js \
  --js-module-kind commonjs \
  -o "$OUT_DIR/handler.js" \
  --force

echo "==> Creating deployment ZIP..."
cd "$OUT_DIR"
zip -r "$ZIP_FILE" handler.js

echo "==> Done: $ZIP_FILE"
echo "    Size: $(du -sh "$ZIP_FILE" | cut -f1)"
