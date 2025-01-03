#!/bin/bash
set -e

# Run the container and get its ID
CID=$(docker run -d sweethome3d-build)

# Copy the build artifacts
docker cp "$CID:/workspace/build" ./build

# Remove the container
docker rm "$CID"
