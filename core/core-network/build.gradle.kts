plugins {
    id("humanos.android.library")
    id("humanos.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eco.humanos.android.core.network"
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
