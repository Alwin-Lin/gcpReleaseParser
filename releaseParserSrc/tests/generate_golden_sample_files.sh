#!/bin/bash

# This script generats expected parse results as protobuf files in text format.
# They will be used in unit tests to validate Parsers.
# A typical flow is as follows:
# 1. Build [uberJar] in build.gradle, Android Studio
# 2. Add the file name in targetFile below after adding a new object file in ./resources
# 3. Run: ./generate_golden_sample_files.sh
# 4. Manully validate the content of output protobuf files
# 5. Update UnitTests & respective test code to use the new files
# 6. Pass all [test] in build.gradle, Android Studion

echo Generating golden sample files for parser validation
parser="../../releaseParserProj/build/libs/releaseParser.jar"
TargetFiles="HelloActivity.apk CtsJniTestCases.apk Shell.apk"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.ApkParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="libEGL.so"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.SoParser -i resources/$file -pi -of resources/$file.pb.txt
done

TargetFiles="CtsAslrMallocTestCases32"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.SoParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="android.test.runner.vdex"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.VdexParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="android.test.runner.odex"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.OdexParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="boot-framework.oat"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.OatParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="boot-framework.art"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.ArtParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="platform.xml android.hardware.vulkan.version.xml"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.XmlParser -i resources/$file -of resources/$file.pb.txt
done

TargetFiles="build.prop"
for file in $TargetFiles; do
    echo Processing $file
    java -cp $parser com.android.cts.releaseparser.BuildPropParser -i resources/$file -of resources/$file.pb.txt
done
