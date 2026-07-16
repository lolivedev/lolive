# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- General ---
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, Signature, InnerClasses, EnclosingMethod, Exceptions, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --- Retrofit (suspend + R8 full mode) ---
# R8 会剥掉 Continuation/接口方法的泛型签名，导致：
# java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface com.ho.lolive.data.remote.**
-keep class com.ho.lolive.data.remote.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- kotlinx.serialization ---
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class com.ho.lolive.**$$serializer { *; }
-keepclassmembers class com.ho.lolive.** {
    *** Companion;
}
-keepclasseswithmembers class com.ho.lolive.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DTOs used by Retrofit + kotlinx.serialization.
-keep class com.ho.lolive.data.remote.dto.** { *; }
-keep @kotlinx.serialization.Serializable class com.ho.lolive.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Native bridge ---
-keep class com.ho.lolive.core.nativebridge.NativeEndpointBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
