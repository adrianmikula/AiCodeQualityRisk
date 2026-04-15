import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "1.9.25"
}

group = "com.aicodequalityrisk"
version = "1.0.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(emptyList())
    sandboxDir.set("/tmp/aicodequalityrisk-idea/sandbox")
}

dependencies {
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
        kotlinOptions.jvmTarget = "17"
    }

    runIde {
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    test {
        useJUnitPlatform()
    }
}
