 #!/bin/bash

 set -e

 # Download a file
 download_file() {
     mkdir -p "$2"
     wget "$1" --directory-prefix=./$2/ --quiet --show-progress -nc
 }

 # Download dependencies
 download_dependencies() {
     echo "Downloading dependencies..."
     download_file "https://github.com/LXGaming/Reconstruct/releases/download/v1.3.26/reconstruct-cli-1.3.26.jar" "tools"
 }

 # Fetch version manifest
 get_version_manifest() {
     curl -s "http://piston-meta.mojang.com/mc/game/version_manifest.json"
 }

 # Get version information from the manifest
 get_version_info() {
     local manifest=$(get_version_manifest)
     curl -s $(echo "$manifest" | jq -r --arg ver "$1" '.versions[] | select(.id == $ver)' | jq -r '.url')
 }

 # Download Minecraft client JAR, mappings and libraries
 download_minecraft() {
     local version_info=$(get_version_info "$1")
     echo "Downloading Minecraft $1..."

     download_file "$(echo "$version_info" | jq -r '.downloads.client.url')" "versions/$1"
     download_file "$(echo "$version_info" | jq -r '.downloads.client_mappings.url')" "versions/$1"

     for lib in $(echo "$version_info" | jq -c '.libraries[]'); do
         download_file "$(echo "$lib" | jq -r '.downloads.artifact.url')" "build/$1"
     done
 }

 # Deobfuscate the JAR file
 deobfuscate_jar() {
     local deobf_jar="build/$1/client_deobf.jar"
     if [[ ! -f "$deobf_jar" ]]; then
         echo "Deobfuscating Minecraft JAR for version $1..."
         cd tools
         java -Xmx2G -jar "reconstruct-cli-1.3.26.jar" \
             -jar "../versions/$1/client.jar" \
             -mapping "../versions/$1/client.txt" \
             -output "../$deobf_jar" -agree
         cd ..
     else
         echo "Deobfuscated JAR for version $1 already exists, skipping..."
     fi
 }

 # Extract hitboxes
 extract_hitboxes() {
     echo "Extracting hitboxes for Minecraft $1..."
     mkdir -p "generated/$1/blocks"

     javac -classpath "build/$1/*" -sourcepath "src" -d "build/$1" "src/net/gunivers/bookshelf/Extractor.java"
     java -Xmx2G -classpath "build/$1/*:build/$1" net.gunivers.bookshelf.Extractor "$1" --pretty
 }

 # Main execution
 if [[ $# -ne 1 ]]; then
     echo "Usage: $0 <minecraft_version>"
     exit 1
 fi

 echo "Starting script for Minecraft $1..."
 download_dependencies
 download_minecraft "$1"
 deobfuscate_jar "$1"
 extract_hitboxes "$1"
 echo "Script completed for Minecraft $1."
