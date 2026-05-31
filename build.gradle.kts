plugins {
    kotlin("jvm")
    id("net.fabricmc.fabric-loom")
    kotlin("plugin.serialization")
    java
}

group = property("maven_group")!!
version = "${property("mod_version")}+mc.${property("minecraft_version")}"

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases/") {
        name = "Terraformers"
    }
}

loom {
    mods {
        register("stream_shield") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation(kotlin("stdlib-jdk8"))

    // ModMenu — optional at runtime (see "suggests" in fabric.mod.json). This is a non-obfuscated
    // (named) environment, so mod deps are consumed directly like the fabric-api dependency above.
    implementation("com.terraformersmc:modmenu:${property("modmenu_version")}")
}

kotlin {
    jvmToolchain(25)
}

java {
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
    jar {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}
