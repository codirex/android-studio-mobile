plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.data.toolchain"
}

dependencies {
    implementation(project(":domain:toolchain"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
