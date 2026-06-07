plugins {
    id("humanos.android.library")
    id("humanos.hilt")
}

android {
    namespace = "eco.humanos.android.core.observability"
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
}
