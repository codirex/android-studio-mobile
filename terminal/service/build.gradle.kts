plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.terminal.service"
}

dependencies {
    implementation(project(":terminal:protocol"))
    implementation(project(":terminal:pty"))
    implementation(project(":terminal:bootstrap"))

    testImplementation(libs.junit4)
}
