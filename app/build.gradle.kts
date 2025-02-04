plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 定义一个函数来读取 local.properties 文件并返回一个 Map<String, String>
fun readProperties(file: File): Map<String, String> {
    val properties = mutableMapOf<String, String>()
    if (file.exists()) {
        file.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val (key, value) = line.split("=", limit = 2).map { it.trim() }
                    properties[key] = value
                }
            }
        }
    } else {
        println("local.properties file not found at: ${file.absolutePath}")
    }
    return properties
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = readProperties(localPropertiesFile)

println("Loading properties from: ${localPropertiesFile.absolutePath}")

android {
    namespace = "com.zhipu.realtime"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.zhipu.realtime"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "APP_ID", "\"${localProperties["APP_ID"] ?: ""}\"")
        buildConfigField("String", "APP_CERTIFICATE", "\"${localProperties["APP_CERTIFICATE"] ?: ""}\"")
        buildConfigField("String", "REMOTE_TOKEN_URL", "\"${localProperties["REMOTE_TOKEN_URL"] ?: ""}\"")
        buildConfigField("String", "REMOTE_TOKEN_URL_DEV", "\"${localProperties["REMOTE_TOKEN_URL_DEV"] ?: ""}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Android Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)

    // AndroidX Lifecycle and Navigation
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    // AndroidX DataStore
    implementation(libs.androidx.datastore)

    // Android Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // External Libraries
    implementation(libs.easypermissions)
    implementation(libs.xpopup)
    implementation(libs.utilcode)
    implementation(libs.commons.codec)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.agora.authentication)
    implementation(libs.agora.rtc)
}