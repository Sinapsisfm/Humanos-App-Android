plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.dashboard"
}

dependencies {
    implementation(project(":data:data-tasks"))
    implementation(project(":data:data-auth"))

    testImplementation(project(":testing:testing-common"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
