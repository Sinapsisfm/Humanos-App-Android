plugins {
    id("humanos.android.feature")
}

android {
    namespace = "eco.humanos.android.feature.dashboard"
}

dependencies {
    implementation(project(":data:data-tasks"))
    implementation(project(":data:data-auth"))
}
