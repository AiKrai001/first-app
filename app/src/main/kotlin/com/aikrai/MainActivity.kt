package com.aikrai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aikrai.location.LocationInfo
import com.aikrai.location.LocationManager
import com.aikrai.model.WeatherData
import com.aikrai.ui.components.AirQualityCard
import com.aikrai.ui.components.CityManagementScreen
import com.aikrai.ui.components.CitySearchScreen
import com.aikrai.ui.components.CurrentWeather
import com.aikrai.ui.components.DailyForecastCard
import com.aikrai.ui.components.HourlyForecastCard
import com.aikrai.ui.components.LocationHeader
import com.aikrai.ui.navigation.AppScreen
import com.aikrai.ui.theme.FirstappTheme
import com.aikrai.viewmodel.WeatherViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.regex.Pattern

/**
 * 自定义ViewModelFactory用于提供WeatherViewModel实例
 * 负责为ViewModel注入必要的依赖，如Context和LocationManager
 */
@Keep
class WeatherViewModelFactory(
    private val context: Context,
    private val locationManager: LocationManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(context, locationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Keep
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    // 初始化LocationManager，负责处理位置相关的功能
    private val locationManager by lazy { LocationManager(this) }

    // 使用ViewModelFactory初始化WeatherViewModel，注入必要的依赖
    private val weatherViewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(this, locationManager)
    }

    // 当前屏幕状态，用于控制返回按钮行为
    private val currentScreenState = mutableStateOf(AppScreen.WEATHER)

    // 全局错误处理器
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "捕获到未处理的协程异常", exception)
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(
                this@MainActivity,
                "应用发生错误: ${exception.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 定位权限请求处理器
     * 在权限请求结果返回后被调用，处理用户授权的结果
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // 权限已授予，立即获取位置并加载天气数据
                lifecycleScope.launch(errorHandler) {
                    try {
                        Log.d(TAG, "位置权限已获取，开始定位")
                        weatherViewModel.startLocationUpdates()
                    } catch (e: Exception) {
                        Log.e(TAG, "启动位置更新失败", e)
                        Toast.makeText(
                            this@MainActivity,
                            "获取位置失败: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                        // 尝试加载默认位置
                        weatherViewModel.loadLastLocation()
                    }
                }
                // 获取到位置后，ViewModel会自动触发天气查询
                // 并且会检查是否需要添加或更新该位置到保存列表中
            }

            else -> {
                // 用户拒绝了权限请求
                Log.w(TAG, "用户拒绝了位置权限请求")
                Toast.makeText(
                    this,
                    "需要位置权限来获取准确的天气信息",
                    Toast.LENGTH_LONG
                ).show()
                // 尝试加载默认位置或上次保存的位置
                lifecycleScope.launch(errorHandler) {
                    weatherViewModel.loadLastLocation()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDebug =
            applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebug) {
            Timber.plant(DebugLogTree())
            Timber.d("应用启动：调试模式")
        } else {
            Timber.plant(ReleaseTree())
            Timber.i("应用启动：发布模式")
        }

        Log.d(TAG, "onCreate: 应用启动")

        try {
            // 1. 启用边缘到边缘显示，优化视觉效果
            enableEdgeToEdge()
            // 2. 允许内容绘制到系统栏区域，提供沉浸式体验
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // 3. 启动时立即检查位置权限并开始获取位置
            checkLocationPermission()

            // 4. 设置返回键处理
            setupBackPressedCallback()

            // 5. 设置UI内容，应用Jetpack Compose布局
            setContent {
                FirstappTheme {
                    WeatherAppWithViewModel(
                        viewModel = weatherViewModel,
                        onCityDelete = { locationInfo ->
                            deleteCityLocation(locationInfo)
                        },
                        currentScreenState = currentScreenState
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity onCreate发生严重错误", e)
            Toast.makeText(
                this,
                "应用启动失败: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }

        // 测试日志代码
        Log.d("MainActivity", "onCreate: 使用Android Log")

        Timber.v("onCreate: 这是详细日志信息") // VERBOSE级别
        Timber.d("onCreate: 这是调试日志信息") // DEBUG级别
        Timber.i("onCreate: 这是重要信息日志") // INFO级别
        Timber.w("onCreate: 这是警告日志信息") // WARN级别
        Timber.e("onCreate: 这是错误日志信息") // ERROR级别

        // 测试带异常的日志
        try {
            // 模拟一个异常
            val list = listOf(1, 2, 3)
            val item = list[10] // 这会抛出IndexOutOfBoundsException
        } catch (e: Exception) {
            Timber.e(e, "发生异常: 数组越界访问")
        }

        // 测试不同线程的日志
        Thread {
            Timber.i("这是在后台线程中记录的日志")
            try {
                Thread.sleep(100)
                throw RuntimeException("后台线程异常")
            } catch (e: Exception) {
                Timber.e(e, "后台线程发生异常")
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 活动销毁时停止位置更新，避免资源泄漏
        weatherViewModel.stopLocationUpdates()
        Log.d(TAG, "onDestroy: 应用关闭")
    }

    /**
     * 检查位置权限并请求权限
     * 在应用启动时被调用，是位置数据获取的第一步
     */
    private fun checkLocationPermission() {
        try {
            Log.d(TAG, "开始检查位置权限")
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，立即开始获取位置并加载天气
                    Log.d(TAG, "已有位置权限，开始定位")
                    lifecycleScope.launch(errorHandler) {
                        weatherViewModel.startLocationUpdates()
                    }
                    // 位置更新后，ViewModel会自动处理位置存储和天气数据获取
                }

                else -> {
                    // 没有权限，发起权限请求
                    Log.d(TAG, "没有位置权限，发起权限请求")
                    locationPermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查位置权限过程中发生错误", e)
            // 出错时尝试加载默认位置
            lifecycleScope.launch(errorHandler) {
                weatherViewModel.loadLastLocation()
            }
        }
    }

    /**
     * 删除城市位置
     */
    private fun deleteCityLocation(locationInfo: LocationInfo) {
        Log.d(TAG, "开始删除城市: ${locationInfo.district}")
        lifecycleScope.launch(errorHandler) {
            weatherViewModel.removeLocation(locationInfo)
        }
    }

    /**
     * 设置返回键处理回调
     */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentScreenState.value) {
                    AppScreen.WEATHER -> {
                        // 在天气主页时，退出应用
                        finish()
                    }

                    else -> {
                        // 在其他页面时，返回到天气主页
                        currentScreenState.value = AppScreen.WEATHER
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume: 活动已恢复")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause: 活动已暂停")
    }

    /**
     * 调试版本使用的日志树，提供完整的调用信息
     */
    private class DebugLogTree : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String {
            return String.format(
                "(%s:%s)#%s",
                element.fileName,
                element.lineNumber,
                element.methodName
            )
        }
    }

    /**
     * 发布版本使用的日志树，过滤低优先级日志，提供线程信息和优化的标签
     */
    private class ReleaseTree : Timber.Tree() {
        private val MAX_TAG_LENGTH = 23
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

        override fun isLoggable(tag: String?, priority: Int): Boolean {
            // 只记录INFO及以上级别的日志
            return priority >= Log.INFO
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val threadName = Thread.currentThread().name
            val formattedMessage = "[$threadName] $message"

            val logPriority = priority
            val logTag = createTag()

            // 普通日志
            if (t == null) {
                Log.println(logPriority, logTag, formattedMessage)
                return
            }

            // 带异常的日志
            Log.println(logPriority, logTag, "$formattedMessage\n${Log.getStackTraceString(t)}")
        }

        /**
         * 从调用堆栈创建优化的日志标签
         */
        private fun createTag(): String {
            val stackTrace = Throwable().stackTrace
            if (stackTrace.size <= CALL_STACK_INDEX) {
                return "Unknown"
            }

            val caller = stackTrace[CALL_STACK_INDEX]
            var tag = caller.className
            val matcher = ANONYMOUS_CLASS.matcher(tag)
            if (matcher.find()) {
                tag = matcher.replaceAll("")
            }

            // 获取简单类名
            tag = tag.substring(tag.lastIndexOf('.') + 1)

            // 确保标签不超过最大长度
            return if (tag.length <= MAX_TAG_LENGTH) tag else tag.substring(0, MAX_TAG_LENGTH)
        }

        companion object {
            private const val CALL_STACK_INDEX = 7
        }
    }
}

/**
 * 主要的天气应用Composable，集成了ViewModel
 * 负责展示天气数据，处理用户交互，以及城市管理功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun WeatherAppWithViewModel(
    viewModel: WeatherViewModel,
    onCityDelete: (LocationInfo) -> Unit,
    currentScreenState: androidx.compose.runtime.MutableState<AppScreen>
) {
    // 通过StateFlow收集各种UI状态
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()
    val currentLocationIndex by viewModel.currentLocationIndex.collectAsState()
    val citySearchState by viewModel.citySearchState.collectAsState()
    // 获取所有天气数据
    val allWeatherData by viewModel.allWeatherData.collectAsState()

    // 下拉刷新状态管理
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshWeather()
        }
    )
    val coroutineScope = rememberCoroutineScope()

    // 获取状态栏高度
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    // 监听天气状态变化，非加载状态时结束下拉刷新
    LaunchedEffect(weatherUiState) {
        if (weatherUiState !is WeatherViewModel.WeatherUiState.Loading) {
            isRefreshing = false
        }
    }

    // 使用传入的导航状态
    var currentScreen by currentScreenState

    // 创建分页状态，使用key参数使其在savedLocations变化时重建
    val pagerState = rememberPagerState(
        initialPage = currentLocationIndex.coerceIn(0, savedLocations.size.coerceAtLeast(1) - 1),
        pageCount = { savedLocations.size.coerceAtLeast(1) }
    )

    // 当页面改变时，更新当前位置索引
    LaunchedEffect(pagerState.currentPage) {
        if (savedLocations.isNotEmpty() && pagerState.currentPage != currentLocationIndex) {
            viewModel.setCurrentLocationIndex(pagerState.currentPage)
        }
    }

    // 当位置索引改变时，滚动到对应页面
    LaunchedEffect(currentLocationIndex) {
        if (savedLocations.isNotEmpty() && pagerState.currentPage != currentLocationIndex) {
            try {
                pagerState.scrollToPage(currentLocationIndex.coerceIn(0, savedLocations.size - 1))
            } catch (e: Exception) {
                // 处理可能的越界异常
                Log.e("MainActivity", "滚动页面异常", e)
            }
        }
    }

    // 添加新位置的功能
    val onAddLocationClick = {
        currentScreen = AppScreen.CITY_MANAGEMENT
    }

    // 菜单点击功能
    val onMenuClick = {
        // 菜单点击处理逻辑
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF42A5F5)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 根据当前导航状态显示不同屏幕
        when (currentScreen) {
            AppScreen.WEATHER -> {
                // 使用下拉刷新
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    when (val state = weatherUiState) {
                        is WeatherViewModel.WeatherUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }

                        is WeatherViewModel.WeatherUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is WeatherViewModel.WeatherUiState.Success -> {
                            // 使用水平分页器实现左右滑动切换城市
                            HorizontalPager(
                                state = pagerState,
                                // 添加key使其在内容变化时能正确重组
                                key = { page -> "page_$page" },
                                // 使用fillMaxSize避免嵌套不必要的Column
                                modifier = Modifier.fillMaxSize(),
                                // 移除分页效果以提高性能
                                pageSpacing = 0.dp,
                                // 当只有一个地点时禁用滑动
                                userScrollEnabled = savedLocations.size > 1,
                                // 添加预加载
                                beyondViewportPageCount = 1
                            ) { page ->
                                // 获取当前页面的位置信息和天气数据
                                val pageLocationInfo = if (page < savedLocations.size) savedLocations[page] else null
                                val locationKey =
                                    pageLocationInfo?.let { "${it.province}_${it.city}_${it.district}" } ?: ""
                                val pageWeatherData =
                                    if (locationKey.isNotEmpty() && allWeatherData.containsKey(locationKey))
                                        allWeatherData[locationKey]!!
                                    else
                                        state.weatherData

                                // 直接调用WeatherContent，不要放在if条件内，以避免@Composable调用问题
                                WeatherContent(
                                    weatherData = pageWeatherData,
                                    locationInfo = pageLocationInfo,
                                    onAddLocationClick = onAddLocationClick,
                                    onMenuClick = onMenuClick,
                                    statusBarPadding = statusBarPadding,
                                    locationCount = savedLocations.size,
                                    currentLocationIndex = page // 使用当前页索引
                                )
                            }
                        }
                    }

                    // 添加下拉刷新指示器
                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF1976D2)
                    )
                }
            }

            AppScreen.CITY_MANAGEMENT -> {
                CityManagementScreen(
                    cities = viewModel.getCityWeatherInfoList(),
                    onBackClick = { currentScreen = AppScreen.WEATHER },
                    onAddCityClick = { currentScreen = AppScreen.CITY_SEARCH },
                    onCityClick = { city ->
                        // 处理选择的城市，找到对应的位置并设置为当前位置
                        val index = savedLocations.indexOfFirst {
                            it.province == city.province &&
                                    it.city == city.city &&
                                    it.district == city.district
                        }
                        if (index >= 0) {
                            viewModel.setCurrentLocationIndex(index)
                        }
                        currentScreen = AppScreen.WEATHER
                    },
                    onCityDelete = { city ->
                        // 显示删除确认
                        Log.d("MainActivity", "请求删除城市: ${city.district}, ${city.city}")

                        // 查找对应的LocationInfo并删除
                        val locationInfo = savedLocations.find {
                            it.province == city.province &&
                                    it.city == city.city &&
                                    it.district == city.district
                        }

                        if (locationInfo != null) {
                            onCityDelete(locationInfo)
                        } else {
                            Log.e("MainActivity", "未找到对应的LocationInfo")
                        }
                    },
                    onCityReorder = { reorderedCities ->
                        // 处理城市重新排序
                        coroutineScope.launch {
                            val reorderedLocations = reorderedCities.mapNotNull { city ->
                                savedLocations.find {
                                    it.province == city.province &&
                                            it.city == city.city &&
                                            it.district == city.district
                                }
                            }
                            // 更新 ViewModel 中的位置列表顺序
                            viewModel.reorderLocations(reorderedLocations)
                        }
                    }
                )
            }

            AppScreen.CITY_SEARCH -> {
                // 获取搜索状态
                val searchResults = when (citySearchState) {
                    is WeatherViewModel.CitySearchState.Success -> (citySearchState as WeatherViewModel.CitySearchState.Success).results
                    else -> emptyList()
                }
                val isSearching = citySearchState is WeatherViewModel.CitySearchState.Searching

                CitySearchScreen(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onBackClick = { currentScreen = AppScreen.CITY_MANAGEMENT },
                    onSearch = { query ->
                        viewModel.searchAddress(query)
                    },
                    onCitySelected = { locationInfo ->
                        // 添加选中的位置，但不改变当前位置索引
                        viewModel.addLocationWithoutChangingCurrent(locationInfo)
                        currentScreen = AppScreen.CITY_MANAGEMENT
                    }
                )
            }
        }
    }
}

/**
 * 天气内容展示Composable
 * 负责展示天气主界面的各个卡片和信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherContent(
    weatherData: WeatherData,
    locationInfo: LocationInfo? = null,
    onAddLocationClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    statusBarPadding: PaddingValues = PaddingValues(0.dp),
    locationCount: Int = 0,
    currentLocationIndex: Int = 0
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // 使用较小的固定padding替代状态栏高度，避免过大空白
                .padding(top = 8.dp)
                .verticalScroll(scrollState)
        ) {
            LocationHeader(
                location = weatherData.displayLocation,
                locationInfo = locationInfo,
                onAddLocationClick = onAddLocationClick,
                onMenuClick = onMenuClick,
                locationCount = locationCount,
                currentLocationIndex = currentLocationIndex
            )

            CurrentWeather(
                currentTemperature = weatherData.currentTemperature,
                weatherCondition = weatherData.weatherCondition,
                humidity = weatherData.humidity,
                highTemperature = weatherData.highTemperature,
                lowTemperature = weatherData.lowTemperature,
                windSpeed = weatherData.windSpeed,
                apparentTemperature = weatherData.apparentTemperature,
                precipitation = weatherData.precipitation,
                visibility = weatherData.visibility,
                forecastKeypoint = weatherData.forecastKeypoint
            )

            HourlyForecastCard(
                hourlyForecasts = weatherData.hourlyForecast,
                sunriseTime = weatherData.sunriseTime,
                sunsetTime = weatherData.sunsetTime
            )

            DailyForecastCard(dailyForecasts = weatherData.dailyForecast)

            AirQualityCard(airQuality = weatherData.airQuality)
        }
    }
}

/**
 * 简单的错误屏幕，用于显示异常信息
 */
@Composable
fun ErrorScreen(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "加载出错",
                color = Color.Red,
                textAlign = TextAlign.Center
            )
            Text(
                text = errorMessage,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
