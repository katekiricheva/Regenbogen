plugins {
    java
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "de.regenbogen"
version = "0.9-beta"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.2")
        bundledPlugin("com.intellij.java")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        name.set("Regenbogen")
        version.set(project.version.toString())
        description.set("Regenbogen clarifies the debugging process by visualizing debugger events as a program execution sequence that can be exported and shared.")
        changeNotes.set("Initial beta release")

        ideaVersion {
            sinceBuild.set("232")
            untilBuild.set("252.*")
        }
    }

    buildSearchableOptions.set(false)
}

tasks {

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

tasks.withType<Test>().configureEach {
    enabled = false
}