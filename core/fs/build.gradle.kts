plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.core.fs"
}

dependencies {
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
