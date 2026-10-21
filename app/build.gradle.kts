import java.util.Properties

plugins {
    id("com.android.application")
    // AGP 9 has built-in Kotlin support; do not apply org.jetbrains.kotlin.android.
}

// Release signing resolves from the environment first (CI: KEYSTORE_FILE,
// KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD), then from an untracked local
// keystore.properties. With neither present the release APK stays unsigned
// (e.g. PR dry builds).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProperties.getProperty(propName)

android {
    namespace = "io.github.unconditional.atvapplist"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.unconditional.atvapplist"
        minSdk = 21
        targetSdk = 34
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val file = signingValue("KEYSTORE_FILE", "storeFile")
            val storePassword = signingValue("KEYSTORE_PASSWORD", "storePassword")
            if (file != null && storePassword != null) {
                storeFile = rootProject.file(file)
                storePassword = storePassword
                keyAlias = signingValue("KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
