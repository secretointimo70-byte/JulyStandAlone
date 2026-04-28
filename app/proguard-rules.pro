# July Offline — ProGuard rules

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# Keep JNI classes (Whisper, Piper, LlamaCpp)
-keep class com.july.offline.ai.stt.WhisperJNI { *; }
-keep class com.july.offline.ai.tts.PiperJNI { *; }
-keep class com.july.offline.ai.llm.embedded.LlamaCppJNI { *; }

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Porcupine SDK
-keep class ai.picovoice.** { *; }

# Keep Room entities and DAOs
-keep class com.july.offline.data.db.** { *; }

# Keep DataStore preferences keys
-keepclassmembers class * {
    @androidx.datastore.preferences.core.Preferences$Key *;
}

# Keep Retrofit interface methods
-keepattributes Signature
-keepattributes *Annotation*
-keep interface com.july.offline.ai.llm.LlmApiService { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google Play Services (mitigate AppCertManager/cycn noise)
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.**

# Keep model classes for serialization
-keep class com.july.offline.domain.model.** { *; }
