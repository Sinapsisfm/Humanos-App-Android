plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.dashboard"
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
}
