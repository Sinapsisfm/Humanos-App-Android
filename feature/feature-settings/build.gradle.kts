plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.settings"
}

dependencies {
    implementation(project(":data:data-auth"))
    implementation(project(":integrations:integration-humanos"))
    implementation(project(":integrations:integration-quebot"))

    implementation(libs.androidx.compose.material.icons.extended)
}
