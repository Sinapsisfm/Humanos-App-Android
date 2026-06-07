plugins {
    id("humanos.android.library")
    id("humanos.compose")
}

android {
    namespace = "eco.humanos.android.core.ui"
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation (for shared NavHost types)
    api(libs.androidx.navigation.compose)
}
