plugins {
    id("humanos.android.library")
    id("humanos.hilt")
}

android {
    namespace = "eco.humanos.android.data.auth"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-datastore"))
    implementation(project(":core:core-security"))

    implementation(libs.kotlinx.coroutines.android)
    // Bridges Firebase/Play Services Task<T> to suspend via Task.await()
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase Auth (BOM manages the version)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Credential Manager + Google ID for the modern Google Sign-In flow
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
