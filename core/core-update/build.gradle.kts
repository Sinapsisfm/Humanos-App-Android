plugins {
    id("humanos.android.library")
    id("humanos.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eco.humanos.android.core.update"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
