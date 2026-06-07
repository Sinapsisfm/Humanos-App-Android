plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.capture"
}

dependencies {
    implementation(project(":data:data-capture"))

    implementation(libs.androidx.compose.material.icons.extended)
}
