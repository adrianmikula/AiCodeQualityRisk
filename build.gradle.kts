plugins {
    id("org.jetbrains.intellij.platform") version "2.13.1"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    application
}

group = "com.aicodequalityrisk"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "AI Code Quality Risk"
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2024.2")
    }

    implementation("io.modelcontextprotocol:kotlin-sdk:0.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.github.javaparser:javaparser-core:3.25.8")
    implementation("io.github.bonede:tree-sitter:0.26.6")
    implementation("io.github.bonede:tree-sitter-java:0.23.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.0")
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

    withType<Test> {
        useJUnitPlatform()
    }

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("intellij.sinceBuild").orElse("242"))
        untilBuild.set(providers.gradleProperty("intellij.untilBuild").let { provider { null } })
    }
}

application {
    mainClass.set("com.aicodequalityrisk.generator.MainKt")
}