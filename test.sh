#!/bin/bash
# Parse a release
MY_NAME=$0 # = "/path/to/test.sh"
MY_FILENAME=${MY_NAME##*/} # = "test.sh"
MY_DIR=${MY_NAME%/$MY_FILENAME} # = "/path/to"

echo "Script dir = $MY_DIR"
echo "need Android/sdk/build-tools in the PATH for aapt, etc."
which aapt

INDIR="$MY_DIR/releaseParserSrc/tests/resources" \
    OUTDIR="$MY_DIR/../tmp/testReleaseParser" \
    REL_PARSER_JAR="$MY_DIR/releaseParserProj/build/libs/releaseParser.jar" \
    JAVA="/usr/bin/java" \
    $MY_DIR/releaseParser.sh
