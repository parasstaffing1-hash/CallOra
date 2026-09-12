# R8 / ProGuard rules for Callora release builds.
#
# The app reflects over types in three places that shrinking would otherwise break:
# Moshi JSON adapters, Retrofit service interfaces, and Room entities. Everything below
# exists to keep one of those working; nothing here is speculative.

# ---- Attributes needed by reflective libraries -------------------------------------------
-keepattributes Signature
-keepattributes InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes SourceFile, LineNumberTable

# ---- Retrofit ----------------------------------------------------------------------------
# Service interfaces are implemented by a runtime proxy, so their methods and generic
# signatures must survive.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ---- OkHttp ------------------------------------------------------------------------------
# Optional TLS providers referenced but not bundled.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Moshi -------------------------------------------------------------------------------
# Codegen emits *JsonAdapter classes that are looked up by name at runtime.
-keep class **JsonAdapter { <init>(...); *; }
-keepnames @com.squareup.moshi.JsonClass class *
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum { <fields>; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
-dontwarn com.squareup.moshi.**

# ---- Room --------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ---- Callora models ----------------------------------------------------------------------
# Room entities and the Gemini request/response DTOs are both mapped by field name.
-keep class com.example.data.model.** { *; }

# ---- Audio engine ------------------------------------------------------------------------
# The DSP is plain Kotlin and safe to optimise, but keep the enum constants: presets are
# persisted by enum name in SharedPreferences and resolved with valueOf().
-keepclassmembers enum com.example.audio.VoiceEffect { *; }

# ---- Firebase ----------------------------------------------------------------------------
-dontwarn com.google.firebase.**
-keepnames class com.google.firebase.** { *; }

# ---- Kotlin ------------------------------------------------------------------------------
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
