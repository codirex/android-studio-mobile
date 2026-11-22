plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.feature.build"
}

dependencies {
    // The report is plain domain data; the service types stay inside this module (ADR-0025).
    implementation(project(":domain:toolchain"))
    implementation(project(":terminal:view"))
    implementation(project(":terminal:protocol"))
    implementation(project(":core:ui"))

    testImplementation(libs.junit4)
}
