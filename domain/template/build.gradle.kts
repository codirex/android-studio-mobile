plugins {
    id("asm.jvm.library")
}

dependencies {
    api(project(":domain:project"))

    testImplementation(libs.junit4)
}
