plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "org.nbschool.appmanager"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.nbschool.appmanager"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "0.2.4"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.android.tools.build:apksig:8.7.3")
    testImplementation("junit:junit:4.13.2")
}
tasks.withType<Test>().configureEach {
    systemProperty("apkFixture", rootProject.file("app/apk/School-App-Manager-0.1.1-debug.apk").absolutePath)
}
