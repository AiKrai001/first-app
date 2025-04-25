plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.aikrai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aikrai"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 添加ABI过滤器，只保留主流架构
        ndk {
//            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

        // 仅保留中文资源
        resConfigs("zh-rCN")
    }

    buildTypes {
        release {
            // 仍然启用代码压缩，但避免混淆重要类
            isMinifyEnabled = true
            // 启用资源压缩
            isShrinkResources = true
            // 使用ProGuard规则文件
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-rules-networking.pro"
            )

            // 为Retrofit和CaiYunWeatherApi添加特殊处理
            buildConfigField("boolean", "KEEP_RETROFIT_INTERFACES", "true")
        }
        debug {
            // 调试版本禁用优化
            isMinifyEnabled = false
            isShrinkResources = false

            // 调试构建也添加标识
            buildConfigField("boolean", "KEEP_RETROFIT_INTERFACES", "true")
        }
        // 添加一个完全没有混淆的测试构建类型
        create("noProguard") {
            initWith(getByName("release"))
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            matchingFallbacks.add("release")
        }
        create("customDebugType") {
            isDebuggable = true
        }
    }

    // 启用 App Bundle 支持
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 配置分包大小
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // 添加annotation支持
    implementation("androidx.annotation:annotation:1.7.1")

    // 使用标准Material3版本
    implementation("androidx.compose.material3:material3:1.3.1")

    // 添加标准的SwipeRefresh组件
    implementation("androidx.compose.material:material:1.6.8")

    // 添加可拖拽排序列表组件
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // 数据存储相关
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-preferences-core:1.0.0")

    // Room数据库
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")

    // 网络请求相关
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // 腾讯定位SDK
    implementation("com.tencent.map.geolocation:TencentLocationSdk-openplatform:7.5.4.3")
    implementation("com.tencent.map:tencent-map-vector-sdk:5.7.0")

    // Material图标
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // 添加Timber用于日志记录 - 保证Release版本也能输出日志
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}