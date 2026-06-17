import java.io.FileInputStream
import java.util.Properties

plugins {
    id("humanos.android.application")
    id("humanos.compose")
    id("humanos.hilt")
}

// Apply the google-services plugin only when google-services.json is present.
// The file is gitignored (contains the Firebase config), so it exists locally
// and in environments that inject it, but NOT on bare CI runners. This keeps
// CI green (compile + unit tests) without committing secrets or requiring a
// GitHub Secret. Firebase runtime features require the file to be present.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "eco.humanos.android"

    defaultConfig {
        // Definitive applicationId confirmed by Felipe 2026-06-07 (Q-003 resolved)
        applicationId = "eco.humanos.android"
        versionCode = 17
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        // Needed for the per-flavor ENABLE_APK_AUTOUPDATE BuildConfig flag below.
        buildConfig = true
    }

    // Distribution flavor: "sideload" (current in-app APK self-update) vs "play"
    // (Google Play AAB — no self-update, no install-packages permission).
    flavorDimensions += "distribution"
    productFlavors {
        create("sideload") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_APK_AUTOUPDATE", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_APK_AUTOUPDATE", "false")
        }
    }

    signingConfigs {
        // Sign debug builds with Felipe's Firebase-registered debug keystore
        // (SHA-1 A6:04:1D:CF:…) so Google Sign-In works and each APK installs
        // in place over the previous one (same cert → no "package conflict").
        //
        // CI sets DEBUG_KEYSTORE_PATH to the decoded keystore; locally the var is
        // unset, so Gradle keeps using ~/.android/debug.keystore (already the
        // same cert on Felipe's machine). This replaces the brittle
        // "overwrite ~/.android/debug.keystore" CI step that the runner ignored.
        getByName("debug") {
            System.getenv("DEBUG_KEYSTORE_PATH")?.let { path ->
                val ks = file(path)
                if (ks.exists()) {
                    storeFile = ks
                    storePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD") ?: "android"
                    keyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
                    keyPassword = System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
                }
            }
        }
        // Release (upload) signing for Google Play. Reads a gitignored
        // keystore.properties at the repo root — NEVER committed. If absent, the
        // release build is left unsigned so CI/local can still assemble/validate.
        create("release") {
            val kp = rootProject.file("keystore.properties")
            if (kp.exists()) {
                val props = Properties()
                FileInputStream(kp).use { props.load(it) }
                val ks = props.getProperty("storeFile")?.let { rootProject.file(it) }
                if (ks != null && ks.exists()) {
                    storeFile = ks
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        getByName("release") {
            // Apply the upload signing config only when a keystore is configured
            // (keystore.properties present). isMinifyEnabled + proguard come from
            // the application convention plugin; release is non-debuggable.
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    // Feature modules
    implementation(project(":feature:feature-dashboard"))
    implementation(project(":feature:feature-capture"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-tasks"))
    implementation(project(":feature:feature-web"))

    // Core modules
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-model"))
    implementation(project(":core:core-observability"))

    // Integration modules (Hilt modules must be visible at app level)
    implementation(project(":integrations:integration-humanos"))
    implementation(project(":integrations:integration-quebot"))

    // Data modules
    implementation(project(":data:data-auth"))

    // Firebase (BOM manages versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)

    // Icons (extended set for navigation icons)
    implementation(libs.androidx.compose.material.icons.extended)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
