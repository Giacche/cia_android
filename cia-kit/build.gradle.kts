plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ueno.cia.kit"
    buildFeatures { compose = true }
}

dependencies {
    api(project(":core"))
    implementation(project(":designsystem"))
    implementation(project(":feature:congrats"))
    implementation(project(":feature:cpf"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:home"))
    implementation(project(":feature:inputamount"))
    implementation(project(":feature:link"))
    implementation(project(":feature:movements"))
    implementation(project(":feature:paymentselector"))
    implementation(project(":feature:qr"))
    implementation(project(":feature:validatelimit"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}
