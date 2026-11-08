// Plugins are declared here once so every module resolves the same versions.
// The modules apply them through the convention plugins in build-logic/.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
}
