import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.kapt") }
val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
fun setting(name: String, fallback: String = "") = local.getProperty(name, fallback)
fun quoted(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
android {
 namespace = "com.codesbyfebin.skrdstore"
 compileSdk = 34
 ndkVersion = "26.3.11579264"
 defaultConfig {
  applicationId = "com.codesbyfebin.skrdstore"; minSdk = 26; targetSdk = 34
  versionCode = 4; versionName = "0.2.2-devnet"
  buildConfigField("String", "CATALOG_URL", quoted(setting("CATALOG_URL")))
  buildConfigField("String", "RPC_URL", quoted(setting("RPC_URL", "https://api.devnet.solana.com")))
  buildConfigField("String", "ENDORSEMENT_RECIPIENT", quoted(setting("ENDORSEMENT_RECIPIENT")))
  buildConfigField("String", "MODEL_SHA256", quoted(setting("MODEL_SHA256", "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e")))
  ndk { abiFilters += "arm64-v8a" }
 }
 buildFeatures { compose = true; buildConfig = true }
 composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 if (rootProject.file("vendor/llama.cpp/CMakeLists.txt").exists()) {
  externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
 }
 if (!rootProject.file("vendor/llama.cpp/CMakeLists.txt").exists()) { sourceSets["main"].jniLibs.srcDir(rootProject.file("prebuilt")) }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.06.00"))
 implementation("androidx.activity:activity-compose:1.9.1")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 kapt("androidx.room:room-compiler:2.6.1")
 implementation("androidx.work:work-runtime-ktx:2.9.1")
 implementation("androidx.security:security-crypto:1.1.0-alpha06")
 implementation("net.zetetic:android-database-sqlcipher:4.5.4")
 implementation("androidx.sqlite:sqlite-ktx:2.4.0")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3")
 testImplementation("junit:junit:4.13.2")
}
