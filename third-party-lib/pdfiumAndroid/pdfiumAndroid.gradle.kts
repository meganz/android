plugins { id("com.android.library") }

android {
    namespace = "com.shockwave.pdfium" // match the actual package structure in the AAR
    compileSdk = rootProject.extra["compileSdkVersion"] as Int // read from root project
    
    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int // also read minSdk from root project
    }
}

dependencies {
    api(files("pdfiumandroid-1.9.3.aar"))
}
