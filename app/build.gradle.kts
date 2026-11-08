plugins { id("com.android.application") }
android {
    namespace = "io.github.vibernoslop"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.github.vibernoslop"
        minSdk = 32
        targetSdk = 35
        versionCode = 5
        versionName = "0.2.3"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies { compileOnly(files("libs/api-82.jar")) }
