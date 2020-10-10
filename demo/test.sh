#!/usr/bin/env bash
# use current directory
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "${DIR}"
# fail if any command fails
set -e

# Bash on Windows compatibility
export MSYS_NO_PATHCONV=1

# build project
if [[ ! -f "../target/docker-base-image-swapper.jar" ]]; then
	echo "[TEST]" "No application built; building project using Docker"
	docker run --rm -v "$(pwd)/..:/project" --workdir "/project" maven:3.6.3-jdk-8-slim mvn clean package
fi

# build the app
echo "[TEST]" "Building app"
docker build -t test-app:latest .


# pull old and new base images from remote Docker repository
echo "[TEST]" "Pulling base images"
docker pull openjdk:8-slim
docker pull openjdk:11-slim
# save all images with the following tags into a single docker image archive
echo "[TEST]" "Saving images into an archive..."
docker save openjdk:8-slim openjdk:11-slim test-app:latest > images.tar

# upgrade base image
echo "[TEST]" "Swapping base image..."
docker run --rm -v "$(pwd)/..:/project" --workdir "/project/demo" openjdk:8-slim \
  java \
	-jar "../target/docker-base-image-swapper.jar" \
	--old-base-tag openjdk:8-slim \
	--new-base-tag openjdk:11-slim \
	--input-tag test-app:latest \
	--output-tag test-app:transformed \
	--output-image "transformed.tar" "images.tar"

echo "[TEST]" "Importing swapped-base app image into Docker"
docker load --input "transformed.tar"

# run apps
echo "[TEST]" "Output from running original image:"
docker run --rm test-app:latest

echo "[TEST]" "Output from running swapped-base image:"
docker run --rm test-app:transformed

# cleanup
#rm images.tar transformed.tar
