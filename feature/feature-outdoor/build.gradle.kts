plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.outdoor"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":core:core-outdoor"))

    testImplementation(project(":testing:testing-common"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    // Compose render test vía Robolectric (evidencia de UI sin emulador).
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(libs.androidx.test.core)
}
