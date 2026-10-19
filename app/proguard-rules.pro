
# ============ Hilt ============
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ============ Room ============
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ============ Firebase ============
#-keep class com.liulkovich.florapoint.domain.** { *; }
#-keep class com.liulkovich.florapoint.domain.cloud.** { *; }
-keep class com.google.firebase.installations.FirebaseInstallationsKtxRegistrar {
    public <init>();
}
-keep class com.liulkovich.florapoint.domain.SyncState { *; }


# ============ Kotlin Serialization ============
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ============ WorkManager ============
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============ OSMDroid ============
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }

# ============ Отладка крашей ============
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Сохраняем ресурсы и R-классы
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Если используешь Coil / Glide
-keep class com.bumptech.glide.** { *; }
-dontwarn coil.**
-dontwarn com.bumptech.glide.**