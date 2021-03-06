plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group "me.deltarays"

val VERSION = "1.4.2"
version = VERSION

repositories {
    mavenCentral()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        content {
            includeGroup("org.spigotmc")
        }
    }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    mavenLocal()
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.10.6")
    compileOnly("org.apache.logging.log4j:log4j-api:2.14.0")
    compileOnly("org.apache.logging.log4j:log4j-core:2.14.0")
    implementation("com.squareup.okhttp3:okhttp:3.14.6")
    implementation("org.java-websocket:Java-WebSocket:1.5.1")
}


tasks {

    shadowJar {
        archiveFileName.set("DiscordConsole-$VERSION.jar")
    }
    build {
        dependsOn(shadowJar)
    }
}