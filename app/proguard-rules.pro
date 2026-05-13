# GettingBiology ProGuard/R8 Rules

# Android entry points are kept from the manifest by the Android Gradle Plugin.
# Do not keep the whole application package; let R8 shrink/optimize app code.

# Room: generated adapters access annotated schema types.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class com.znam.app.** { *; }
-keep @androidx.room.Dao class com.znam.app.** { *; }
-keepclassmembers class com.znam.app.** {
    @androidx.room.ColumnInfo <fields>;
}
-dontwarn androidx.room.paging.**

# Parcelable creators are reflectively referenced by Android framework APIs.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Koin/Compose/Ads/Lottie/Konfetti/Coil publish consumer rules; keep warnings quiet only.
-dontwarn org.koin.**
-dontwarn androidx.compose.**
-dontwarn com.google.android.gms.ads.**
-dontwarn com.airbnb.lottie.**
-dontwarn nl.dionsegijn.konfetti.**
-dontwarn coil.**
-dontwarn kotlinx.coroutines.**
