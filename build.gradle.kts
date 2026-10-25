import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.compose") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-okhttp:3.5.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    // Audio: JavaFX Media ships the native decoders inside these jars, so the
    // packaged app needs no VLC/GStreamer install on the user's machine.
    implementation("org.openjfx:javafx-base:27.0.1:win")
    implementation("org.openjfx:javafx-graphics:27.0.1:win")
    implementation("org.openjfx:javafx-media:27.0.1:win")
}

kotlin { jvmToolchain(21) }

compose.desktop {
    application {
        mainClass = "com.spoz.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Spoz"
            packageVersion = "1.0.0"
            description = "Spo~z Desktop - search and stream from YouTube Music"
            vendor = "rawde_vvvvvv"
            windows {
                menu = true
                shortcut = true
                perUserInstall = true
                upgradeUuid = "7b2f4c9e-1a34-4d0b-9f21-3c5e8a6d1b70"
            }
        }
    }
}
