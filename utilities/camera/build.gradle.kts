plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.codesbyfebin.seekerutility"
 compileSdk = 34
 defaultConfig {
  applicationId = "com.codesbyfebin.seeker.camera"; minSdk = 26; targetSdk = 34
  versionCode = 1; versionName = "0.1.0"
  buildConfigField("String", "MODE", "\"camera\"")
  buildConfigField("String", "TITLE", "\"Seeker Camera\"")
 }


 buildFeatures { compose = true; buildConfig = true }
 composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.camera:camera-camera2:1.3.4")
 implementation("androidx.camera:camera-lifecycle:1.3.4")
 implementation("androidx.camera:camera-view:1.3.4")

 implementation(platform("androidx.compose:compose-bom:2024.06.00"))
 implementation("androidx.activity:activity-compose:1.9.1")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
 implementation("androidx.documentfile:documentfile:1.0.1")
 implementation("com.google.zxing:core:3.5.3")
 testImplementation("junit:junit:4.13.2")
}
