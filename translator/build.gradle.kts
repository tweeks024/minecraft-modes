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
    // Phase 2a: Java AST + symbol resolution. Pulls in javaparser-core
    // transitively, so no separate javaparser-core dependency.
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---------------------------------------------------------------------
// Per-mod runtime classpath manifest dump.
//
// Phase 2a's JavaSourceLoader needs each mod's resolved `runtimeClasspath`
// (NeoForge + Minecraft + transitive deps) so JavaParser's `JarTypeSolver`
// can resolve types like `IronGolem.createAttributes()`. We can't read it
// from the translator's CLI directly because the translator runs as a
// JavaExec task and doesn't get configuration access at task-execution
// time. Instead, declare each mod's `runtimeClasspath` as a task input
// and dump the resolved jar list to a small text file under
// `translator/build/classpaths/<modId>.txt`. The CLI reads
// `translator.classpathDir` from system properties and walks the files.
// ---------------------------------------------------------------------
val modProjects = listOf("securitycore", "securityguard", "creeperskin", "thief")

// Force each sibling mod to evaluate before this script so its
// `runtimeClasspath` configuration is fully constructed (NeoForge +
// Minecraft + all transitive deps).
modProjects.forEach { evaluationDependsOn(":$it") }

val classpathManifestDir = layout.buildDirectory.dir("classpaths")

// Register a per-mod task INSIDE each sibling mod's project that writes
// its own resolved runtimeClasspath to a text file. This is the only
// path Gradle 9 will accept: a configuration can only be resolved by
// a task that lives in the same project that owns it. Cross-project
// resolution from the translator script triggers the "without an
// exclusive lock" guard.
val perModDumpTasks: Map<String, TaskProvider<Task>> = modProjects.associateWith { modId ->
    project(":$modId").tasks.register("dumpRuntimeClasspathForTranslator") {
        group = "translator"
        description = "Dump $modId's runtimeClasspath to translator/build/classpaths/$modId.txt."
        val rtcp = project.configurations.getByName("runtimeClasspath")
        inputs.files(rtcp).withPropertyName("runtimeClasspath")
        val outFile = classpathManifestDir.map { it.file("$modId.txt") }
        outputs.file(outFile)
        doLast {
            val target = outFile.get().asFile
            target.parentFile.mkdirs()
            target.writeText(rtcp.files.joinToString("\n") { it.absolutePath })
        }
    }
}

val dumpModClasspaths = tasks.register("dumpModClasspaths") {
    group = "translator"
    description = "Aggregate trigger for per-mod runtimeClasspath dumps; produces translator/build/classpaths/."
    perModDumpTasks.values.forEach { dependsOn(it) }
    outputs.dir(classpathManifestDir)
}

tasks.test {
    useJUnitPlatform()
    // Symbol-resolution sanity tests need the classpath manifests on disk.
    dependsOn(dumpModClasspaths)
    systemProperty("translator.classpathDir", classpathManifestDir.get().asFile.absolutePath)
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
    dependsOn(dumpModClasspaths)
    systemProperty("translator.classpathDir", classpathManifestDir.get().asFile.absolutePath)
}
