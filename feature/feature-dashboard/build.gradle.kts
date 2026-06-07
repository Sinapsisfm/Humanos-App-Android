plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.dashboard"
}

dependencies {
    implementation(project(":integrations:integration-humanos"))
    implementation(project(":data:data-auth"))
}
