plugins {
    id("fabric-loom") version "1.9-SNAPSHOT"
    id("maven-publish")
}

version = "1.0.0+mc1.20.4"

group = "net.deepacat"

base { archivesName = "deepamonu" }

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    flatDir {
        dirs("libs")
    }
    maven("https://maven.parchmentmc.org")
    maven("https://maven.siphalor.de/")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://jitpack.io/")
    maven("https://cursemaven.com")
    maven("https://maven.nucleoid.xyz/")
    mavenCentral()
}

loom {
    accessWidenerPath = file("src/main/resources/deepamonu.accesswidener")
    runConfigs {
        "client" {
            vmArgs("-Dmixin.debug.export=true")
        }
    }
}

dependencies {
    minecraft(libs.minecraft)

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment(libs.parchment)
    })

    // Fabric deps
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.cloth) { exclude(group = "net.fabricmc.fabric-api") }

    // Required deps
    modImplementation(libs.mma)

    // Optional dependencies
    modImplementation(libs.bundles.umm)
    modImplementation(libs.modmenu)
    modImplementation(libs.sodium)

    modImplementation(libs.bundles.tslat)

    // Dev instance mods for debugging
    modRuntimeOnly(libs.bundles.dev)
    modRuntimeOnly(libs.provihealth)
    modImplementation(files("libs/unofficial-monumenta-mod-mc1.20.4-1.10.jar"))
}

tasks {
    processResources {
        inputs.property("version", version)
        inputs.property("minecraft_version", libs.versions.minecraft.get())
        inputs.property("loader_version", libs.versions.fabric.loader.get())
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to version,
                    "minecraft_version" to libs.versions.minecraft.get(),
                    "loader_version" to libs.versions.fabric.loader.get()
                )
            )
        }

        filesMatching("en_us.json") { filter { line -> line.replace(Regex("//.+"), "") } }
    }

    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
}

val targetJavaVersion = 17

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }

    withSourcesJar()
}

publishing {
    publications { create<MavenPublication>("mavenJava") { from(project.components["java"]) } }

    repositories { mavenLocal() }
}
