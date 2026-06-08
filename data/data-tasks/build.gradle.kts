plugins {
    id("humanos.android.library")
    id("humanos.hilt")
}

android {
    namespace = "eco.humanos.android.data.tasks"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-observability"))
    implementation(project(":integrations:integration-humanos"))

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
