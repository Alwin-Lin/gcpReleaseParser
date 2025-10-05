import org.gradle.api.tasks.testing.Test
import com.google.protobuf.gradle.*
import org.gradle.api.tasks.SourceSet
import org.gradle.api.file.SourceDirectorySet
import java.util.concurrent.Callable
import org.gradle.api.plugins.JavaPluginExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0-beta05")
        // https://github.com/google/protobuf-gradle-plugin
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.5")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    `java`
    `java-library`
    id("com.google.protobuf") version "0.9.5"
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// These are variables that needs to be changed based on your project.
val srcRoot = "../releaseParserSrc"
val javSrc = "$srcRoot/src"
val generatedSrc = "./build/generated/sources/proto/main/java"
val testSrc = "$srcRoot/tests/src"
val testResource = "$srcRoot/tests/resources"
val protoSrc = "$srcRoot/proto"
val libRoot = "../libs"

// Java version must be greater than Java 9
// For Windows
extra["java_home"] = System.getenv("JAVA_HOME") ?: "/usr"
val testExecutePath = "${extra["java_home"]}/bin/java"

defaultTasks("uberJar")

// Customize https://guides.gradle.org/building-java-libraries/
configure<ProtobufExtension> {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.21.12"
    }
}

val SourceSet.proto: SourceDirectorySet
    get() = extensions.getByName("proto") as SourceDirectorySet

sourceSets {
    getByName("main") {
        proto.srcDir(protoSrc)
        java.setSrcDirs(listOf(javSrc, generatedSrc))
    }
    getByName("test") {
        java.setSrcDirs(listOf(testSrc))
        resources.setSrcDirs(listOf(testResource))
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("generateProto"))
}

tasks.withType<Test> {
    // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
    // executable('C:\Program Files\Java\jdk-14.0.1\bin\java.exe')
    executable = testExecutePath
}
// Create \releaseParserProj\build\libs\releaseParserProj.jar
// https:   //docs.gradle.org/current/userguide/working_with_files.html#sec:creating_uber_jar_example
tasks.register<Jar>("uberJar") {
    duplicatesStrategy = DuplicatesStrategy.WARN
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "com.android.cts.releaseparser.Main"
        )
    }
    archiveFileName.set("releaseParser.jar")
    from(sourceSets.getByName("main").output)
    dependsOn(configurations.getByName("runtimeClasspath"))
    from(Callable {
        configurations.getByName("runtimeClasspath").filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

dependencies {
    // https://android.googlesource.com/platform/external/protobuf/+log/refs/heads/main
    // this should match libRoot/tradefed.jar, which taken from android-cts/tools
    implementation("com.google.protobuf:protobuf-java:3.21.12")
    // Include every .jar under libRoot
    implementation(fileTree(libRoot) { include("*.jar") })
    implementation("commons-io:commons-io:2.0.1")
}
