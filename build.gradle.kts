plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.blockforge"
version = "5.0.0-SNAPSHOT"
description = "ChaosCraft - Horror-themed game mode plugin for BlockForge Studios"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://mvn.lumine.io/repository/maven-public/") // MythicMobs + ModelEngine
    maven("https://repo.citizensnpcs.co/") // Citizens
    maven("https://repo.codemc.io/repository/maven-public/") // packetevents
    maven("https://jitpack.io") // ItemsAdder
}

dependencies {
    // Paper API 1.21.4 with NMS access
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // MythicMobs API
    compileOnly("io.lumine:Mythic-Dist:5.7.2")

    // ModelEngine API
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.7")

    // Citizens API
    compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }

    // packetevents
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")

    // ItemsAdder API
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.3-beta-14")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    jar {
        archiveClassifier.set("unshaded")
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
    }

    assemble {
        dependsOn(shadowJar)
    }

    processResources {
        // Get git commit SHA for build tracking
        val gitSha = providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
        }.standardOutput.asText.map { it.trim() }.getOrElse("unknown")

        val props = mapOf(
            "version" to project.version,
            "description" to project.description,
            "gitsha" to gitSha
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
        filesMatching("build.properties") {
            expand(props)
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
