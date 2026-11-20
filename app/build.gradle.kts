plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ueno.cia.sandbox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ueno.cia.sandbox"
        minSdk = 26
        targetSdk = 36
        // La versión sale de -PCIA_VERSION_CODE / -PCIA_VERSION_NAME (o del entorno) para que el CI la fije por build.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = (findProperty("CIA_VERSION_CODE") as String?)?.toInt() ?: 1
        versionName = (findProperty("CIA_VERSION_NAME") as String?) ?: "0.1.0"
    }

    // La firma de release se configura por propiedades o variables de entorno; nunca hay secretos en el repo.
    // Sin ellas el release se genera sin firmar (suficiente para el CI de verificación).
    val storeFile = providers.gradleProperty("CIA_STORE_FILE").orElse(providers.environmentVariable("CIA_STORE_FILE")).orNull
    if (storeFile != null) {
        signingConfigs {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = providers.gradleProperty("CIA_STORE_PASSWORD")
                    .orElse(providers.environmentVariable("CIA_STORE_PASSWORD")).orNull
                keyAlias = providers.gradleProperty("CIA_KEY_ALIAS")
                    .orElse(providers.environmentVariable("CIA_KEY_ALIAS")).orNull
                keyPassword = providers.gradleProperty("CIA_KEY_PASSWORD")
                    .orElse(providers.environmentVariable("CIA_KEY_PASSWORD")).orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (storeFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(project(":cia-kit"))
    implementation(project(":core"))
    implementation(project(":designsystem"))
    implementation(project(":storage"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Compose trae un Espresso viejo que falla en Android 16 (InputManager.getInstance fue eliminado)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(kotlin("test"))
    debugImplementation(libs.compose.ui.test.manifest)
}
