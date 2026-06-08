plugins {
    id("humanos.android.library")
    id("humanos.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eco.humanos.android.core.security"
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    // Session is serialized to JSON before being encrypted at rest in the vault.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
