#!/bin/bash
# Script to run the opencode docker container

# Change to project root (two directories up from this script's location)
cd "$(dirname "$0")/../.." || exit 1

# Run the container
docker run -d \
    --name opencode-container-scala-cheatsheet \
    -v "$(pwd)":/workspace \
    -v ~/.opencode_auth:/root/.local/share/opencode \
    opencode-docker-scala-cheatsheet

echo "Container started successfully!"
echo "To use opencode, run:"
echo "  docker exec -it opencode-container-scala-cheatsheet opencode ."

