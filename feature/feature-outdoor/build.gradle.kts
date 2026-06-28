plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.outdoor"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
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

    // Instrumented (emulador/dispositivo): render Compose on-device.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
