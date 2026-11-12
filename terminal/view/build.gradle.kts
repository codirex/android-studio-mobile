plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.terminal.view"
}

dependencies {
    implementation(project(":terminal:emulator"))
    implementation(project(":terminal:protocol"))
    implementation(project(":terminal:service"))

    testImplementation(libs.junit4)
}
