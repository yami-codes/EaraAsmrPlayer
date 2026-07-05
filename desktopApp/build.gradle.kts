import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(project(":shared"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.asmr.player.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EaraAsmrPlayer"
            packageVersion = "1.1.4"
            description = "Eara ASMR Player - cross-platform desktop companion"
            vendor = "eValDoll"

            windows {
                menuGroup = "Eara ASMR Player"
                upgradeUuid = "8f4e2c1a-9b3d-4e5f-a6c7-d8e9f0a1b2c3"
            }

            linux {
                packageName = "eara-asmr-player"
            }
        }
    }
}
