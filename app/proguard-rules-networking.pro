# 网络请求相关的ProGuard规则

# 限制优化级别 - 替代proguardOptions设置
-optimizationpasses 2

# 保留必要的属性
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes MethodParameters
-keepattributes GenericSignature
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*
-keepattributes Exceptions

# 针对Retrofit代理类的特定保护
-keepnames class * implements java.lang.reflect.InvocationHandler
-keep,allowobfuscation @interface retrofit2.http.*

# 保留API接口方法
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * extends retrofit2.Call
-dontwarn retrofit2.**

# 保护关键平台类和适配器
-keep class retrofit2.Platform { *; }
-keep class retrofit2.Platform$* { *; }
-keep class * extends retrofit2.Converter
-keep class * extends retrofit2.CallAdapter

# 完全保留彩云天气API相关类
-keep class com.aikrai.api.CaiYunWeatherApi { *; }
-keep class com.aikrai.api.CaiYunWeatherService { *; }
-keepnames class com.aikrai.api.** { *; }

# OkHttp规则
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okio.Buffer { *; }

# 特别处理返回类型的泛型信息
-keep class retrofit2.converter.gson.GsonConverterFactory { *; }

# 保留所有使用注解的类方法
-keep class * {
    @retrofit2.http.* <methods>;
}

# Gson特定规则
-keepattributes Signature
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# 保留模型类
-keep class com.aikrai.model.** { *; }
-keep class com.aikrai.api.** { *; }
-keep class com.aikrai.location.** { *; }
-keep class com.aikrai.data.** { *; }

# 特殊处理CaiYunWeatherConsolidatedData类和相关嵌套类，保证完整性
-keep class com.aikrai.model.CaiYunWeatherConsolidatedData { *; }
-keep class com.aikrai.model.CaiYunResult { *; }
-keep class com.aikrai.model.CaiYunRealtime { *; }
-keep class com.aikrai.model.CaiYunMinutely { *; }
-keep class com.aikrai.model.CaiYunHourly { *; }
-keep class com.aikrai.model.CaiYunDaily { *; }
-keep class com.aikrai.model.CaiYunAlert { *; }
-keep class com.aikrai.model.CaiYunRealtime$** { *; }
-keep class com.aikrai.model.CaiYunHourly$** { *; }
-keep class com.aikrai.model.CaiYunDaily$** { *; }
-keep class com.aikrai.model.CaiYunAlert$** { *; }

# 避免混淆内部类
-keepattributes InnerClasses

# 确保保留日志类
-keep class timber.log.** { *; }

# 保留所有带有DEBUG、VERBOSE、INFO、WARN、ERROR、TAG成员变量的类
-keepclassmembers class ** {
    private static final ** *TAG;
    public static final ** *TAG;
    private static final ** *VERBOSE;
    private static final ** *DEBUG;
    private static final ** *INFO;
    private static final ** *WARN;
    private static final ** *ERROR;
} 