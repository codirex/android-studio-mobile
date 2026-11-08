plugins {
    id("asm.jvm.library")
}

dependencies {
    api(project(":domain:common"))

    testImplementation(libs.junit4)
}
