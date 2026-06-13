# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Gson (für API-Responses)
-keep class com.schuetzentracker.model.** { *; }
-keep class com.schuetzentracker.api.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
