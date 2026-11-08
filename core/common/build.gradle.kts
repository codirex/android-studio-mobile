plugins {
	alias(libs.plugins.android.library)
}

android {
	namespace = "org.codirex.asm.core.common"
	compileSdk = 36

	defaultConfig {
		minSdk = 24
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}
