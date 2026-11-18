plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.data.environment"
}

dependencies {
    implementation(project(":domain:environment"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
