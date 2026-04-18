pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.jetbrains.intellij.platform") version "2.14.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
}

rootProject.name = "ai-code-quality-risk"