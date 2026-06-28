plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.outdoor"
}

dependencies {
    implementation(project(":core:core-outdoor"))

    testImplementation(project(":testing:testing-common"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
