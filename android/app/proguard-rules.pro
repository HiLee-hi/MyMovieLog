# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson (TMDB DTO)
-keep class com.mymovie.log.data.remote.tmdb.dto.** { *; }

# Supabase Kotlin Serialization
-keep class com.mymovie.log.data.remote.supabase.dto.** { *; }
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
