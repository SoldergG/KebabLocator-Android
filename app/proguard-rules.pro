# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class com.kebablocator.android.models.** { *; }
-keep class com.kebablocator.android.services.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Google Maps
-keep class com.google.android.gms.maps.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
