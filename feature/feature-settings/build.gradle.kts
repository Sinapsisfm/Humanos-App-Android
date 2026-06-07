plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.settings"
}

dependencies {
    implementation(project(":data:data-auth"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
}
