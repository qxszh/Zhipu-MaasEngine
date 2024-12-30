import org.gradle.api.JavaVersion.VERSION_11

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
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
    namespace = "com.zhipu.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zhipu.ai"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "APP_ID", "\"${localProperties["APP_ID"] ?: ""}\"")
        buildConfigField("String", "APP_CERTIFICATE", "\"${localProperties["APP_CERTIFICATE"] ?: ""}\"")
        buildConfigField("String", "REMOTE_TOKEN_URL", "\"${localProperties["REMOTE_TOKEN_URL"] ?: ""}\"")
        buildConfigField("String", "REMOTE_AUTH_TOKEN", "\"${localProperties["REMOTE_AUTH_TOKEN"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = VERSION_11
        targetCompatibility = VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    viewBinding {
        enable = true
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(project(":maas"))
    implementation(libs.commons.codec)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.easypermissions)
    implementation(libs.xpopup)
    implementation(libs.agora.authentication)
}