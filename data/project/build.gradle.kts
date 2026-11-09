plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.data.project"
}

dependencies {
    implementation(project(":domain:project"))
    implementation(project(":domain:filetree"))
    implementation(project(":domain:editor"))
    implementation(project(":core:fs"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
