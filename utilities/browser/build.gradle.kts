plugins { id("com.android.application") version "9.1.1"; id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" }
android {
 namespace="com.codesbyfebin.seekerutility"
 compileSdk=37
 defaultConfig { applicationId="com.codesbyfebin.seeker.browser"; minSdk=26; targetSdk=34; versionCode=1; versionName="0.1.0"; ndk { abiFilters += "arm64-v8a" } }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.06.00"))
 implementation("androidx.activity:activity-compose:1.9.1")
 implementation("androidx.compose.material3:material3")
 implementation("org.mozilla.geckoview:geckoview:157.0.20261005135250")
}
