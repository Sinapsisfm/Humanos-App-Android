plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.capture"
}

dependencies {
    implementation(project(":core:core-observability"))

    implementation(libs.androidx.compose.material.icons.extended)
}
