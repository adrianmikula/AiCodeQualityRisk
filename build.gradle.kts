import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

group = "com.aicodequalityrisk"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlin-mcp-sdk/sdk")
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(emptyList())
    sandboxDir.set("/tmp/aicodequalityrisk-idea/sandbox")
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.github.javaparser:javaparser-core:3.25.8")
    implementation("io.github.bonede:tree-sitter:0.26.6")
    implementation("io.github.bonede:tree-sitter-java:0.23.5")
    testImplementation(kotlin("test"))
}

tasks {
    withType<JavaCompile> {
        options.release.set(17)
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    runIde {
    }

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("intellij.sinceBuild").orElse("233"))
        untilBuild.set(providers.gradleProperty("intellij.untilBuild").orElse(""))
    }

    test {
        useJUnitPlatform()
    }
}
