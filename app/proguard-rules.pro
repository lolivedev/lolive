# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- General ---
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, Signature, InnerClasses, EnclosingMethod, Exceptions, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.ho.lolive.**$$serializer { *; }
-keepclassmembers class com.ho.lolive.** {
    *** Companion;
}
-keepclasseswithmembers class com.ho.lolive.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DTOs used by Retrofit + kotlinx.serialization.
-keep class com.ho.lolive.data.remote.dto.** { *; }

# --- Retrofit / OkHttp ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# --- Native bridge ---
