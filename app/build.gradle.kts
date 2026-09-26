import java.util.Properties
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
}

val localConfig = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val publicConfig = JsonSlurper().parse(rootProject.file("config/supabase.public.json")) as Map<*, *>
fun override(name: String): String? = (System.getenv(name) ?: localConfig.getProperty(name))?.takeIf { it.isNotBlank() }
val overrideSupabase = override("SUPABASE_URL") != null || override("SUPABASE_PUBLISHABLE_KEY") != null
require(!overrideSupabase || (override("SUPABASE_URL") != null && override("SUPABASE_PUBLISHABLE_KEY") != null)) {
    "Configurá SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY juntas para cambiar de proyecto."
}
fun config(name: String): String = (override(name) ?: when (name) {
    "SUPABASE_URL" -> publicConfig["url"].toString()
    "SUPABASE_PUBLISHABLE_KEY" -> publicConfig["publishableKey"].toString()
    else -> ""
})
    .replace("\\", "\\\\").replace("\"", "\\\"")

android {
    buildFeatures { buildConfig = true }
    namespace = "com.miapp.dndcompanion"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.miapp.dndcompanion"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${config("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${config("SUPABASE_PUBLISHABLE_KEY")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "PASSWORD_RESET_URL", "\"${config("PASSWORD_RESET_URL").ifEmpty { "http://localhost:5173/?recovery=1" }}\"")
            buildConfigField("String", "API_BASE_URL", "\"${config("API_BASE_URL").ifEmpty { "http://10.0.2.2:3001/api" }}\"")
        }
        release {
            buildConfigField("String", "PASSWORD_RESET_URL", "\"${config("PASSWORD_RESET_URL")}\"")
            buildConfigField("String", "API_BASE_URL", "\"${config("API_BASE_URL")}\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}
