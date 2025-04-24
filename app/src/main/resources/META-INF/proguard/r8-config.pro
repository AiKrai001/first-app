# R8特定的混淆规则，补充proguard-rules.pro

# 保持所有使用@Keep注解的类和成员
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# 保持Gson需要的类型
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# 保留数据存储相关类
-keep class androidx.datastore.** { *; }

# 确保所有模型类不被混淆
-keep class com.aikrai.model.** { *; }
-keep class com.aikrai.data.** { *; }
-keep class com.aikrai.location.** { *; }

# 保留DataStore和Room相关类
-keep class ** extends androidx.datastore.preferences.core.Preferences$Key { *; }
-keep class ** extends androidx.room.RoomDatabase { *; }

# 保留所有枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留Kotlin类型
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines相关
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# 保留Jetpack Compose相关
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; } 