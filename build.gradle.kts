plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // kotlin-compose wird bei Kotlin 1.9.x nicht als separates Plugin benötigt
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
