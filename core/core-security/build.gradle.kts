plugins {
    id("humanos.android.library")
    id("humanos.hilt")
}

android {
    namespace = "eco.humanos.android.core.security"
}

dependencies {
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
}
