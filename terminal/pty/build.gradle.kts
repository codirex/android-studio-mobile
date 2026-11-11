plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.terminal.pty"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    testImplementation(libs.junit4)
}
