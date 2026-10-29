plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "io.arkansasio.picocalccompanion"
 compileSdk = 35
 defaultConfig {
  applicationId = "io.arkansasio.picocalccompanion"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "1.0.0"
 }
}
dependencies {
 implementation("com.github.mik3y:usb-serial-for-android:3.8.1")
 testImplementation("junit:junit:4.13.2")
}
