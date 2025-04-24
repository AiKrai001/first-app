# 网络请求相关的ProGuard规则

# 完全禁止优化Retrofit相关部分
-dontoptimize
# 限制优化级别 - 替代proguardOptions设置
-optimizationpasses 1
-optimizations !method/removal/parameter,!code/allocation/variable

# 关键：直接禁止对Retrofit代理类的优化和混淆
-dontobfuscate
-dontshrink
-dontpreverify

# 完全保留泛型信息及方法参数 - 防止泛型擦除
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes MethodParameters
-keepattributes GenericSignature
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*
-keepattributes Exceptions

# 完全保留Retrofit相关类
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class retrofit2.Platform { *; }
-keep class retrofit2.Platform$* { *; }
-keep class retrofit2.converter.gson.** { *; }

# 特别保留动态代理生成的类
-keepnames class * implements java.lang.reflect.InvocationHandler
-keep,allowobfuscation @interface retrofit2.http.*

# 保留接口方法
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * extends retrofit2.Call
-dontwarn retrofit2.**

# 完全保留彩云天气API相关类
-keep class com.aikrai.api.CaiYunWeatherApi { *; }
-keep class com.aikrai.api.CaiYunWeatherService { *; }
-keepnames class com.aikrai.api.** { *; }

# OkHttp规则
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.*

# 特别处理返回类型的泛型信息
-keep class * extends retrofit2.Converter
-keep class * extends retrofit2.CallAdapter
-keep class retrofit2.adapter.rxjava.RxJavaCallAdapterFactory { *; }
-keep class retrofit2.converter.gson.GsonConverterFactory { *; }

# 保留所有使用注解的类
-keep class * {
    @retrofit2.http.* <methods>;
}

# 保留所有实现接口的类
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <fields>;
}

# Gson特定规则
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
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

# 完全禁止对模型类的处理
-keepclassmembers class com.aikrai.model.** { *; }

# 避免混淆内部类
-keepattributes InnerClasses

# 确保保留日志类
-keep class timber.log.** { *; }
-keep class android.util.Log { *; }

# 保留Timber标签
-keepclassmembers class ** {
    private static final org.slf4j.Logger *;
    private static final java.util.logging.Logger *;
    private static final java.util.logging.Level *;
    private static final timber.log.Timber.Tree *;
    private static final timber.log.Timber.Forest *;
}

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