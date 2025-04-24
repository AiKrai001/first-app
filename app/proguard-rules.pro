# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保留应用程序入口点
-keep class com.aikrai.MainActivity { *; }

# 使用自定义的KeepApi注解保留类和方法
-keep @com.aikrai.model.KeepApi class * { *; }
-keepclasseswithmembers class * {
    @com.aikrai.model.KeepApi *;
}

# Retrofit混淆问题专用规则 - 最高优先级
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# 关键：禁止对CaiYunWeatherApi接口进行任何处理
-keep interface com.aikrai.api.CaiYunWeatherApi { *; }
-keep class com.aikrai.api.CaiYunWeatherService { *; }

# 关键：禁止对泛型参数进行混淆和优化
-keepattributes MethodParameters
-keepattributes GenericSignature
-keepattributes InnerClasses

# Compose相关规则
-keepclasseswithmembers class androidx.compose.** { *; }
-keep class androidx.compose.ui.text.** { *; }
-keep class androidx.compose.material3.** { *; }

# Kotlin相关
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Retrofit和OkHttp
-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Room数据库
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * {*;}

# 腾讯地图SDK
-keep class com.tencent.map.** { *; }
-keep class com.tencent.tencentmap.** { *; }
-keep class com.tencent.mapsdk.** { *; }
-keep class com.tencent.location.** { *; }

# gson相关
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# 保留所有数据模型类
-keep class com.aikrai.data.model.** { *; }
-keep class com.aikrai.model.** { *; }
-keep class com.aikrai.data.** { *; }

# 保留位置相关的类
-keep class com.aikrai.location.** { *; }

# 保留ViewModel相关类
-keep class com.aikrai.viewmodel.** { *; }

# 保留状态管理相关
-keepclassmembers class ** extends androidx.lifecycle.ViewModel {
    <fields>;
}
-keepclassmembers class ** {
    @kotlinx.coroutines.flow.* <fields>;
}

# 保留Datastore相关
-keep class androidx.datastore.** { *; }

# 通用配置
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 腾讯地图SDK混淆规则
-keepattributes *Annotation*
-keepclassmembers class ** {
    public void on*Event(...);
}
-keep public class com.tencent.location.**{
    public protected *;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class c.t.**{*;}
-keep class com.tencent.map.geolocation.**{*;}
-keep class com.tencent.tencentmap.lbssdk.service.*{*;}
-dontwarn  org.eclipse.jdt.annotation.**
-dontwarn  c.t.**
-dontwarn  android.location.Location
-dontwarn  android.net.wifi.WifiManager
-dontnote ct.**

# Suppress warnings for BouncyCastle and Conscrypt classes
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE


# 保留日志相关类和方法
-keepclassmembers class ** { *** get*(); *** set*(***); }
-keep class timber.log.** { *; }
-keep class android.util.Log { *; }
-keep class **.BuildConfig { *; }