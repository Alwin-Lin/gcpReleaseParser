#!/bin/bash
#
# A script to parse an Android release.

set -euo pipefail

# --- Configuration ---
# Input directory containing the release files.
: "${INDIR:?Please set INDIR to the path of the release folder.}"
# Output directory for the parsed files.
: "${OUTDIR:?Please set OUTDIR to the path of the output folder.}"
# Path to the releaseParser.jar file.
: "${REL_PARSER_JAR:?Please set REL_PARSER_JAR to the path of releaseParser.jar.}"
# Path to the Java executable (must be Java 9 or higher).
: "${JAVA:=java}"

# --- Prerequisite Checks ---
if ! command -v "$JAVA" &> /dev/null; then
    echo "Error: Java executable not found at '$JAVA'. Please set the JAVA environment variable."
    exit 1
fi

if ! command -v aapt &> /dev/null; then
    echo "Error: 'aapt' not found in your PATH. Please install it and ensure it's accessible."
    exit 1
fi

# --- Java Version Check ---
JAVA_VERSION=$("$JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2}')
MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d. -f1)

if [[ "$MAJOR_VERSION" -lt 9 ]]; then
    echo "Error: Java version 9 or higher is required. You have version $JAVA_VERSION."
    exit 1
fi

# --- Main Execution ---
echo "Input directory:  $INDIR"
echo "Output directory: $OUTDIR"
echo "Parser JAR:       $REL_PARSER_JAR"
echo "Java executable:  $JAVA"
echo

echo "Cleaning up output directory: $OUTDIR"
rm -rf "$OUTDIR"
mkdir -p "$OUTDIR"

echo "Running release parser..."
"$JAVA" -jar "$REL_PARSER_JAR" -i "$INDIR" -o "$OUTDIR"

echo "Parsing complete."
