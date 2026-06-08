plugins {
    id("humanos.android.application")
    id("humanos.compose")
    id("humanos.hilt")
    alias(libs.plugins.google.services)
}

android {
    namespace = "eco.humanos.android"

    defaultConfig {
        // Definitive applicationId confirmed by Felipe 2026-06-07 (Q-003 resolved)
        applicationId = "eco.humanos.android"
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Feature modules
    implementation(project(":feature:feature-dashboard"))
    implementation(project(":feature:feature-capture"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-tasks"))

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
