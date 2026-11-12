plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.feature.terminal"
}

dependencies {
    // The fragment's factory takes plain strings; the service types stay inside this module.
    implementation(project(":terminal:view"))
    implementation(project(":terminal:protocol"))
    implementation(project(":core:ui"))

    testImplementation(libs.junit4)
}
