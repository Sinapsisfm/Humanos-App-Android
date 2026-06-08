plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.web"
}

dependencies {
    // Bridge token for the session-bridge WebView (ADR-0006).
    implementation(project(":data:data-auth"))
    implementation(project(":core:core-model"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
