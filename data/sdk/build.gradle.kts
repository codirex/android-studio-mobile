plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.data.sdk"
}

dependencies {
    implementation(project(":domain:sdk"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
