import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// MapLibre Compose's desktop build needs Java 25. Gradle fetches it if it is
// not installed, and the app is compiled, run and packaged with it.
val desktopJava = 25

kotlin {
    jvmToolchain(desktopJava)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.maplibre.compose)
    runtimeOnly(libs.maplibre.compose.runtime.linux.x64)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.calemccammon.weather.kmp.MainKt"
        javaHome =
            javaToolchains
                .launcherFor { languageVersion = JavaLanguageVersion.of(desktopJava) }
                .get()
                .metadata.installationPath.asFile.absolutePath
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.calemccammon.weather.kmp"
            packageVersion = "1.0.0"
        }
    }
}
