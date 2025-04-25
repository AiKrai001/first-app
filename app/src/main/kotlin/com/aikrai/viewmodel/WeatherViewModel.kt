package com.aikrai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aikrai.api.CaiYunWeatherService
import com.aikrai.data.DataStoreManager
import com.aikrai.location.AddressSearchService
import com.aikrai.location.LocationInfo
import com.aikrai.location.LocationManager
import com.aikrai.model.MockWeatherData
import com.aikrai.model.WeatherData
import com.aikrai.ui.components.CityWeatherInfo
import com.aikrai.ui.components.toCityWeatherInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel 用于管理及提供天气数据，自动收集 LocationManager 提供的位置状态。
 * 支持多地点天气数据的管理。
 */
class WeatherViewModel(
    private val context: Context,
    private val locationManager: LocationManager,
    private val weatherService: CaiYunWeatherService = CaiYunWeatherService(context)
) : ViewModel() {

    private val TAG = "WeatherViewModel"
    private val gson = Gson()
    private val dataStoreManager = DataStoreManager(context)
    private val addressSearchService = AddressSearchService(context)

    // 昨天天气数据
    private val _yesterdayWeatherData = MutableStateFlow<Map<String, WeatherData>>(emptyMap())
    val yesterdayWeatherData: StateFlow<Map<String, WeatherData>> = _yesterdayWeatherData.asStateFlow()

    // UIState，供界面层收集并渲染
    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    // 保存的所有位置
    private val _savedLocations = MutableStateFlow<List<LocationInfo>>(emptyList())
    val savedLocations: StateFlow<List<LocationInfo>> = _savedLocations.asStateFlow()

    // 当前选中的位置索引
    private val _currentLocationIndex = MutableStateFlow(0)
    val currentLocationIndex: StateFlow<Int> = _currentLocationIndex.asStateFlow()

    // 城市搜索状态
    private val _citySearchState = MutableStateFlow<CitySearchState>(CitySearchState.Initial)
    val citySearchState: StateFlow<CitySearchState> = _citySearchState.asStateFlow()

    // 用来缓存最近一次成功获取到的地理位置信息
    private var cachedLocationInfo: LocationInfo? = null

    // 保存所有地点的天气数据
    private val _allWeatherData = MutableStateFlow<Map<String, WeatherData>>(emptyMap())
    val allWeatherData: StateFlow<Map<String, WeatherData>> = _allWeatherData.asStateFlow()

    init {
        // 加载已保存的位置
        loadSavedLocations()

        // 设置位置变化监听
        setupLocationMonitoring()

        // 不在这里自动启动位置更新，而是由Activity来控制启动时机
        viewModelScope.launch {
            try {
                // 先加载已保存的位置并检查是否有数据
                val savedLocations = dataStoreManager.getSavedLocations()
                _savedLocations.value = savedLocations

                if (savedLocations.isNotEmpty()) {
                    // 加载所有地点的昨日天气数据
                    loadAllLocationsYesterdayWeather()

                    // 有保存的位置，先加载缓存天气数据
                    fetchAllLocationsWeather()
                }
                // 不会自动启动位置更新，等待Activity调用startLocationUpdates
            } catch (e: Exception) {
                Timber.e(e, "初始化时出错")
                _weatherUiState.value = WeatherUiState.Success(MockWeatherData.weatherData)
            }
        }
    }

    /**
     * 设置位置监听
     */
    private fun setupLocationMonitoring() {
        viewModelScope.launch {
            locationManager.locationState.collect { state ->
                when (state) {
                    is LocationManager.LocationState.Loading -> {
                        if (cachedLocationInfo == null && _weatherUiState.value !is WeatherUiState.Success) {
                            _weatherUiState.value = WeatherUiState.Loading
                        }
                    }

                    is LocationManager.LocationState.Success -> {
                        val locationInfo = state.locationInfo
                        handleNewLocation(locationInfo)
                    }

                    is LocationManager.LocationState.Error -> {
                        // 无法获取位置，使用之前的逻辑处理
                        handleLocationError()
                    }
                }
            }
        }
    }

    /**
     * 处理新获取的位置
     */
    private fun handleNewLocation(locationInfo: LocationInfo) {
        cachedLocationInfo = locationInfo  // 缓存位置信息

        viewModelScope.launch {
            // 保存位置信息到本地存储
            dataStoreManager.saveLocationInfo(locationInfo)

            // 检查是否需要将当前位置添加到位置列表
            checkAndAddCurrentLocation(locationInfo)

            // 获取该位置的天气
            fetchWeatherByLocation(locationInfo)
        }
    }

    /**
     * 检查是否需要添加当前位置到保存列表
     */
    private suspend fun checkAndAddCurrentLocation(locationInfo: LocationInfo) {
        // 检查省市区是否为空，如果有任一为空则不添加
        if (locationInfo.province.isBlank() || locationInfo.city.isBlank() || locationInfo.district.isBlank()) {
            return
        }

        val locations = _savedLocations.value

        // 检查位置列表中是否已有相同的位置
        val exists = locations.any {
            it.province == locationInfo.province &&
                    it.city == locationInfo.city &&
                    it.district == locationInfo.district
        }

        if (!exists) {
            // 位置不存在，添加到列表
            dataStoreManager.addLocationToSavedList(locationInfo)

            // 重新加载位置列表
            loadSavedLocations()

            // 切换到新位置
            val newLocations = dataStoreManager.getSavedLocations()
            val newIndex = newLocations.indexOfFirst {
                it.province == locationInfo.province &&
                        it.city == locationInfo.city &&
                        it.district == locationInfo.district
            }

            if (newIndex >= 0) {
                _currentLocationIndex.value = newIndex
            }
        }
    }

    /**
     * 处理位置错误
     */
    private fun handleLocationError() {
        viewModelScope.launch {
            // 无法获取位置，如果有缓存位置则使用缓存位置
            if (cachedLocationInfo != null) {
                fetchWeatherByLocation(cachedLocationInfo!!)
            } else {
                // 加载已保存的位置，如果有的话
                val savedLocations = dataStoreManager.getSavedLocations()
                if (savedLocations.isNotEmpty()) {
                    _savedLocations.value = savedLocations
                    fetchAllLocationsWeather()
                } else {
                    // 否则使用默认数据
                    _weatherUiState.value = WeatherUiState.Success(MockWeatherData.weatherData)
                }
            }
        }
    }

    /**
     * 加载已保存的位置列表
     */
    private fun loadSavedLocations() {
        viewModelScope.launch {
            val locations = dataStoreManager.getSavedLocations()
            _savedLocations.value = locations
            Timber.d("已加载保存的位置列表: ${locations.size}个位置")
        }
    }

    /**
     * 获取所有已保存位置的天气数据
     */
    private fun fetchAllLocationsWeather() {
        val locations = _savedLocations.value
        if (locations.isEmpty()) {
            // 没有保存的位置，尝试获取当前位置
            viewModelScope.launch {
                startLocationUpdates()
            }
            return
        }

        viewModelScope.launch {
            // 先获取缓存的天气数据
            val cachedData = mutableMapOf<String, WeatherData>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            // 先检查缓存
            for (location in locations) {
                val locationKey = dataStoreManager.getLocationKey(location)
                val cachedWeatherJson = dataStoreManager.getTodayWeatherData(currentDate, locationKey)

                if (cachedWeatherJson != null) {
                    try {
                        val weatherData = gson.fromJson(cachedWeatherJson, WeatherData::class.java)
                        cachedData[locationKey] = weatherData
                    } catch (e: Exception) {
                        Timber.e(e, "解析缓存天气数据失败")
                    }
                }
            }

            // 更新已有缓存数据
            _allWeatherData.value = cachedData

            // 如果当前选中位置索引有效且有对应数据，则更新UI
            updateCurrentLocationWeather()

            // 然后逐个请求最新数据
            for (location in locations) {
                fetchWeatherByLocation(location)
            }
        }
    }

    /**
     * 更新当前选中位置的天气UI状态
     */
    private fun updateCurrentLocationWeather() {
        val index = _currentLocationIndex.value
        val locations = _savedLocations.value

        if (locations.isEmpty()) {
            // 没有保存的位置，尝试获取当前位置
            viewModelScope.launch {
                startLocationUpdates()
            }
            return
        }

        val validIndex = index.coerceIn(0, locations.size - 1)
        val locationInfo = locations[validIndex]
        val locationKey = dataStoreManager.getLocationKey(locationInfo)

        val weatherData = _allWeatherData.value[locationKey]
        if (weatherData != null) {
            _weatherUiState.value = WeatherUiState.Success(weatherData)
        } else {
            // 如果没有数据，则显示加载状态并尝试获取数据
            _weatherUiState.value = WeatherUiState.Loading
            viewModelScope.launch {
                fetchWeatherByLocation(locationInfo)
            }
        }
    }

    /**
     * 设置当前选中的位置索引
     */
    fun setCurrentLocationIndex(index: Int) {
        val locations = _savedLocations.value
        if (locations.isEmpty()) return

        val validIndex = index.coerceIn(0, locations.size - 1)
        _currentLocationIndex.value = validIndex

        // 更新当前显示的天气数据
        updateCurrentLocationWeather()
    }

    /**
     * 刷新天气数据
     */
    fun refreshWeather() {
        viewModelScope.launch {
            if (_savedLocations.value.isEmpty()) {
                startLocationUpdates()
                return@launch
            }

            val currentIndex = _currentLocationIndex.value
            val locations = _savedLocations.value

            if (currentIndex >= 0 && currentIndex < locations.size) {
                _weatherUiState.value = WeatherUiState.Loading
                fetchWeatherByLocation(locations[currentIndex])
            } else {
                startLocationUpdates()
            }
        }
    }

    /**
     * 搜索地址
     */
    fun searchAddress(query: String) {
        if (query.isBlank()) {
            _citySearchState.value = CitySearchState.Success(emptyList())
            return
        }

        _citySearchState.value = CitySearchState.Searching

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = addressSearchService.searchSimilarAddresses(query)
                // 处理搜索结果
                val processedResults = results.map { location ->
                    location.copy(
                        province = location.province,
                        city = location.city,
                        district = location.district,
                        address = location.address
                    )
                }
                _citySearchState.value = CitySearchState.Success(processedResults)
            } catch (e: Exception) {
                Timber.e(e, "搜索地址失败")
                _citySearchState.value = CitySearchState.Error(e.message ?: "搜索失败")
            }
        }
    }

    /**
     * 添加位置到保存列表，但不改变当前位置索引
     */
    fun addLocationWithoutChangingCurrent(locationInfo: LocationInfo) {
        viewModelScope.launch {
            // 记住当前选中的位置
            val currentIndex = _currentLocationIndex.value
            val currentLocations = _savedLocations.value
            val currentLocation = if (currentLocations.isNotEmpty() && currentIndex < currentLocations.size) {
                currentLocations[currentIndex]
            } else null

            // 添加新位置
            dataStoreManager.addLocationToSavedList(locationInfo)

            // 获取新位置的天气数据
            fetchWeatherByLocation(locationInfo)

            // 重新加载位置列表
            loadSavedLocations()

            // 恢复当前位置索引
            if (currentLocation != null) {
                val newIndex = _savedLocations.value.indexOfFirst {
                    it.province == currentLocation.province &&
                            it.city == currentLocation.city &&
                            it.district == currentLocation.district
                }
                if (newIndex >= 0) {
                    _currentLocationIndex.value = newIndex
                }
            }
        }
    }

    /**
     * 删除保存的位置
     */
    fun removeLocation(locationInfo: LocationInfo) {
        Timber.d("开始删除位置: ${locationInfo.district}, ${locationInfo.city}")
        viewModelScope.launch {
            try {
                // 获取当前位置索引和删除前的位置列表
                val currentIndex = _currentLocationIndex.value
                val oldLocations = _savedLocations.value

                // 记录要删除的位置信息
                val locationToRemove = oldLocations.find {
                    it.province == locationInfo.province &&
                            it.city == locationInfo.city &&
                            it.district == locationInfo.district
                }

                if (locationToRemove == null) {
                    Timber.e("未找到要删除的位置: ${locationInfo.district}, ${locationInfo.city}")
                    return@launch
                }

                // 删除位置
                dataStoreManager.removeLocation(locationInfo)
                Timber.d("位置已从DataStore删除: ${locationInfo.district}, ${locationInfo.city}")

                // 重新加载位置列表，确保UI更新
                val newLocations = dataStoreManager.getSavedLocations()
                Timber.d("新位置列表大小: ${newLocations.size}")
                _savedLocations.value = newLocations

                // 处理当前选中的位置
                if (newLocations.isEmpty()) {
                    // 如果没有位置了，则重新获取当前位置
                    Timber.d("没有剩余位置，尝试获取当前位置")
                    startLocationUpdates()
                } else if (currentIndex >= newLocations.size) {
                    // 如果当前索引超出范围，则切换到第一个位置
                    Timber.d("当前索引($currentIndex)超出范围，切换到首位置")
                    _currentLocationIndex.value = 0
                    updateCurrentLocationWeather()
                } else {
                    // 如果当前索引仍然有效，检查是否是删除的位置
                    val wasSelectedLocationRemoved = oldLocations.getOrNull(currentIndex)?.let {
                        it.province == locationInfo.province &&
                                it.city == locationInfo.city &&
                                it.district == locationInfo.district
                    } ?: false

                    if (wasSelectedLocationRemoved) {
                        // 如果删除的是当前选中的位置，切换到首位置
                        Timber.d("删除的是当前选中的位置，切换到首位置")
                        _currentLocationIndex.value = 0
                    }

                    // 更新当前天气显示
                    updateCurrentLocationWeather()
                }
            } catch (e: Exception) {
                Timber.e(e, "删除位置出错")
            }
        }
    }

    /**
     * 通过位置获取天气数据
     */
    private suspend fun fetchWeatherByLocation(locationInfo: LocationInfo) {
        try {
            Timber.d("开始请求彩云天气数据: 经度=${locationInfo.longitude}, 纬度=${locationInfo.latitude}")

            val locationKey = dataStoreManager.getLocationKey(locationInfo)

            // 获取该位置昨天的天气数据
            val yesterdayDataJson = dataStoreManager.getYesterdayWeatherData(locationKey).first()
            val yesterdayData = if (yesterdayDataJson != null) {
                try {
                    gson.fromJson<WeatherData>(yesterdayDataJson, WeatherData::class.java)
                } catch (e: Exception) {
                    Timber.e(e, "解析昨天天气数据失败")
                    null
                }
            } else {
                null
            }

            // 更新昨天天气数据的StateFlow
            if (yesterdayData != null) {
                val updatedYesterdayData = _yesterdayWeatherData.value.toMutableMap()
                updatedYesterdayData[locationKey] = yesterdayData
                _yesterdayWeatherData.value = updatedYesterdayData
            }

            val result = weatherService.getWeatherData(
                longitude = locationInfo.longitude,
                latitude = locationInfo.latitude,
                province = locationInfo.province,
                city = locationInfo.city,
                district = locationInfo.district,
                yesterdayWeather = yesterdayData
            )

            result.fold(
                onSuccess = { weatherData ->
                    Timber.d("获取天气数据成功: ${locationInfo.district}")

                    // 更新保存的天气数据
                    val updatedData = _allWeatherData.value.toMutableMap()
                    updatedData[locationKey] = weatherData
                    _allWeatherData.value = updatedData

                    // 如果是当前选中的位置，则更新UI
                    val currentIndex = _currentLocationIndex.value
                    val locations = _savedLocations.value
                    if (currentIndex < locations.size &&
                        locations[currentIndex].district == locationInfo.district
                    ) {
                        _weatherUiState.value = WeatherUiState.Success(weatherData)
                    }

                    // 保存天气数据到本地
                    saveWeatherData(weatherData, locationInfo)
                },
                onFailure = { e ->
                    Timber.e(e, "获取天气数据失败: ${locationInfo.district}")

                    // 如果是当前选中的位置，则更新UI错误状态
                    val currentIndex = _currentLocationIndex.value
                    val locations = _savedLocations.value
                    if (currentIndex < locations.size &&
                        locations[currentIndex].district == locationInfo.district
                    ) {

                        // 检查是否有缓存数据
                        val cachedData = _allWeatherData.value[locationKey]
                        if (cachedData != null) {
                            _weatherUiState.value = WeatherUiState.Success(cachedData)
                        } else {
                            // 显示模拟数据
                            val fallbackData = MockWeatherData.getWeatherDataForLocation(
                                province = locationInfo.province,
                                city = locationInfo.city,
                                district = locationInfo.district
                            )
                            _weatherUiState.value = WeatherUiState.Success(fallbackData)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "获取天气数据异常: ${locationInfo.district}")

            // 如果是当前选中的位置，则更新UI错误状态
            val currentIndex = _currentLocationIndex.value
            val locations = _savedLocations.value
            if (currentIndex < locations.size &&
                locations[currentIndex].district == locationInfo.district
            ) {

                // 显示模拟数据
                val fallbackData = MockWeatherData.getWeatherDataForLocation(
                    province = locationInfo.province,
                    city = locationInfo.city,
                    district = locationInfo.district
                )
                _weatherUiState.value = WeatherUiState.Success(fallbackData)
            }
        }
    }

    /**
     * 保存天气数据到本地存储
     */
    private suspend fun saveWeatherData(weatherData: WeatherData, locationInfo: LocationInfo) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val locationKey = dataStoreManager.getLocationKey(locationInfo)
            val weatherDataJson = gson.toJson(weatherData)
            dataStoreManager.saveTodayWeatherData(weatherDataJson, currentDate, locationKey)
        } catch (e: Exception) {
            Timber.e(e, "保存天气数据失败")
        }
    }

    /**
     * 启动位置更新监听（例如在 Activity 的 onCreate 或权限通过后调用）
     */
    suspend fun startLocationUpdates() {
        locationManager.startLocationUpdates()
    }

    /**
     * 停止位置更新，避免不必要的耗电（例如在 Activity 的 onDestroy 中调用）
     */
    fun stopLocationUpdates() {
        locationManager.stopLocationUpdates()
    }

    /**
     * 获取所有保存位置的天气信息，用于城市管理界面显示
     */
    fun getCityWeatherInfoList(): List<CityWeatherInfo> {
        val locations = _savedLocations.value
        val weatherDataMap = _allWeatherData.value

        return locations.map { location ->
            val locationKey = dataStoreManager.getLocationKey(location)
            val weatherData = weatherDataMap[locationKey]

            if (weatherData != null) {
                location.toCityWeatherInfo(
                    temperature = weatherData.currentTemperature,
                    weatherCondition = weatherData.weatherCondition
                )
            } else {
                // 如果没有天气数据，则使用默认值
                CityWeatherInfo(
                    province = location.province,
                    city = location.city,
                    district = location.district,
                    temperature = 0.0,
                    weatherCondition = "未知"
                )
            }
        }
    }

    /**
     * 加载上次保存的位置信息和天气数据
     * 当用户拒绝位置权限时，使用该方法加载缓存的位置和天气
     */
    fun loadLastLocation() {
        viewModelScope.launch {
            try {
                // 加载保存的位置列表
                val locations = dataStoreManager.getSavedLocations()
                _savedLocations.value = locations

                if (locations.isNotEmpty()) {
                    // 有保存的位置，加载天气数据
                    fetchAllLocationsWeather()
                } else {
                    // 没有保存的位置，显示默认天气数据
                    Timber.d("没有保存的位置，显示默认天气数据")
                    _weatherUiState.value = WeatherUiState.Success(MockWeatherData.weatherData)
                }
            } catch (e: Exception) {
                Timber.e(e, "加载上次位置信息失败")
                _weatherUiState.value = WeatherUiState.Success(MockWeatherData.weatherData)
            }
        }
    }

    // UI状态类
    sealed class WeatherUiState {
        object Loading : WeatherUiState()
        data class Success(val weatherData: WeatherData) : WeatherUiState()
        data class Error(val message: String) : WeatherUiState()
    }

    // 城市搜索状态
    sealed class CitySearchState {
        object Initial : CitySearchState()
        object Searching : CitySearchState()
        data class Success(val results: List<LocationInfo>) : CitySearchState()
        data class Error(val message: String) : CitySearchState()
    }

    /**
     * 重新排序位置列表
     */
    fun reorderLocations(reorderedLocations: List<LocationInfo>) {
        viewModelScope.launch {
            // 保存重新排序的位置列表
            dataStoreManager.saveReorderedLocations(reorderedLocations)

            // 重新加载位置列表
            loadSavedLocations()

            // 确保当前位置索引仍然指向正确的位置
            val currentLocation = reorderedLocations.getOrNull(_currentLocationIndex.value)
            if (currentLocation != null) {
                val newIndex = _savedLocations.value.indexOfFirst {
                    it.province == currentLocation.province &&
                            it.city == currentLocation.city &&
                            it.district == currentLocation.district
                }
                if (newIndex >= 0) {
                    _currentLocationIndex.value = newIndex
                }
            }
        }
    }

    /**
     * 加载所有地点的昨日天气数据
     */
    private suspend fun loadAllLocationsYesterdayWeather() {
        val locations = _savedLocations.value
        if (locations.isEmpty()) return

        val yesterdayDataMap = mutableMapOf<String, WeatherData>()

        for (location in locations) {
            val locationKey = dataStoreManager.getLocationKey(location)
            val yesterdayDataJson = dataStoreManager.getYesterdayWeatherData(locationKey).first()

            if (yesterdayDataJson != null) {
                try {
                    val weatherData = gson.fromJson<WeatherData>(yesterdayDataJson, WeatherData::class.java)
                    yesterdayDataMap[locationKey] = weatherData
                } catch (e: Exception) {
                    Timber.e(e, "解析昨天天气数据失败: $locationKey")
                }
            }
        }

        _yesterdayWeatherData.value = yesterdayDataMap
    }
}
