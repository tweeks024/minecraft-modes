// Translator: a plain JVM/Kotlin tool that translates Java NeoForge mods in this
// repo into Bedrock Add-Ons. NOT a NeoForge mod — no NeoGradle plugin here.
//
// Phase 0: skeleton only. Discovery + manifest writer + UUIDv5 generator + CLI.

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    application
}

group = "com.tweeks.translator"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        // The mod modules target Java 25 (Minecraft 26.1.2 ships JDK 25). The
        // translator is independent of that and uses the LTS JDK 21 listed in
        // gradle.properties' installation paths.
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.tweeks.translator.CliKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// The user-facing entry point:
//   ./gradlew :translator:translate --args="securityguard"
//   ./gradlew :translator:translate --args="--diff"
//   ./gradlew :translator:translate
//
// JavaExec rather than `application`'s `run` so we can wire it explicitly and
// keep the task name aligned with the design spec.
tasks.register<JavaExec>("translate") {
    group = "translator"
    description = "Translate one or all mods in this repo to Bedrock Add-Ons under bedrock-out/."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.tweeks.translator.CliKt")
    // Run from the repo root so relative paths (settings.gradle, bedrock-out/) resolve correctly.
    workingDir = rootProject.projectDir
}
