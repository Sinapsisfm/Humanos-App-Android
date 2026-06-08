plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.tasks"
}

dependencies {
    implementation(project(":data:data-tasks"))

    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(project(":testing:testing-common"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
