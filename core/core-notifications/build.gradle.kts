plugins {
    id("humanos.android.library")
    id("humanos.hilt")
}

android {
    namespace = "eco.humanos.android.core.notifications"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-datastore"))
    implementation(project(":integrations:integration-humanos"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
