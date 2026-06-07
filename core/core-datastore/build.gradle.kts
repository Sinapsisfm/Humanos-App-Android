plugins {
    id("humanos.android.library")
    id("humanos.hilt")
}

android {
    namespace = "eco.humanos.android.core.datastore"
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
