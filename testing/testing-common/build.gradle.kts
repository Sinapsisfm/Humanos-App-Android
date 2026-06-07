plugins {
    id("humanos.kotlin.library")
}

dependencies {
    implementation(libs.junit)
    implementation(libs.truth)
    implementation(libs.turbine)
    implementation(libs.kotlinx.coroutines.test)
}
