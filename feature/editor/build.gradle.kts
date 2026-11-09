plugins {
    id("asm.android.library")
    // Needed in every module that contains an @AndroidEntryPoint class (ADR-0009).
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.codirex.asm.feature.editor"
}

dependencies {
    // EditorFragment mentions ProjectName; Hilt-generated code in the app mentions the port.
    api(project(":domain:project"))
    api(project(":domain:editor"))

    implementation(project(":core:concurrency"))
    implementation(project(":core:ui"))

    implementation(libs.sora.editor)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
