package com.aikrai.data

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aikrai.location.LocationInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_preferences")

@Keep
class DataStoreManager(context: Context) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    // 使用GsonBuilder配置更安全的Gson实例
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()  // 防止特殊字符被转义
        .serializeNulls()       // 序列化null值
        .create()

    companion object {
        // 位置信息的key
        private val LOCATION_INFO = stringPreferencesKey("location_info")

        // 最后一次获取位置和天气的时间戳
        private val LAST_LOCATION_UPDATE_TIME = longPreferencesKey("last_location_update_time")

        // 已保存的位置列表
        private val SAVED_LOCATIONS = stringPreferencesKey("saved_locations")

        // 保存已保存位置列表的key
        private const val SAVED_LOCATIONS_KEY = "saved_locations"

        // 保存今日天气数据的key前缀
        private const val WEATHER_DATA_PREFIX = "weather_data_"

        // 日期格式化工具
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    // 安全地将对象转换为JSON
    private fun toJson(obj: Any): String {
        return try {
            gson.toJson(obj)
        } catch (_: Exception) {
            "{}" // 出错时返回空对象
        }
    }

    // 保存位置信息
    suspend fun saveLocationInfo(locationInfo: LocationInfo) {
        val locationInfoJson = toJson(locationInfo)
        dataStore.edit { preferences ->
            preferences[LOCATION_INFO] = locationInfoJson
            preferences[LAST_LOCATION_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    // 保存今天的天气数据，传入字符串形式的weatherData
    suspend fun saveTodayWeatherData(weatherDataJson: String, dateString: String, locationKey: String = "default") {
        val key = stringPreferencesKey("${WEATHER_DATA_PREFIX}${locationKey}_$dateString")
        dataStore.edit { preferences ->
            preferences[key] = weatherDataJson
        }
    }

    // 获取昨天的天气数据
    fun getYesterdayWeatherData(locationKey: String = "default"): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                // 获取今天的日期
                val todayDate = LocalDate.now()
                // 计算昨天的日期
                val yesterdayDate = todayDate.minusDays(1)
                // 格式化昨天的日期
                val yesterdayString = yesterdayDate.format(dateFormatter)
                // 构建昨天天气数据的key
                val yesterdayKey = stringPreferencesKey("${WEATHER_DATA_PREFIX}${locationKey}_$yesterdayString")
                // 获取昨天的天气数据
                preferences[yesterdayKey]
            }
    }

    // 获取今天的天气数据 - 根据日期和位置键
    suspend fun getTodayWeatherData(dateString: String, locationKey: String): String? {
        val key = stringPreferencesKey("${WEATHER_DATA_PREFIX}${locationKey}_$dateString")
        return dataStore.data.first()[key]
    }

    // 添加位置到已保存列表
    suspend fun addLocationToSavedList(locationInfo: LocationInfo) {
        // 检查省市区是否为空，如果有任一为空则不保存
        if (locationInfo.province.isBlank() || locationInfo.city.isBlank() || locationInfo.district.isBlank()) {
            return
        }

        val savedLocations = getSavedLocations().toMutableList()

        // 检查位置列表中是否已存在相同位置
        val exists = savedLocations.any {
            it.province == locationInfo.province &&
                    it.city == locationInfo.city &&
                    it.district == locationInfo.district
        }

        if (!exists) {
            savedLocations.add(locationInfo)
            dataStore.edit { preferences ->
                preferences[SAVED_LOCATIONS] = toJson(savedLocations)
            }
        }
    }

    // 获取已保存的位置列表（挂起函数，只获取一次数据）
    suspend fun getSavedLocations(): List<LocationInfo> {
        val locationsJson = dataStore.data.map { preferences ->
            preferences[SAVED_LOCATIONS]
        }.first() ?: return emptyList()

        return try {
            val listType: Type = object : TypeToken<List<LocationInfo>>() {}.type
            gson.fromJson(locationsJson, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // 移除已保存的位置
    suspend fun removeLocation(locationInfo: LocationInfo) {
        val savedLocations = getSavedLocations().toMutableList()
        savedLocations.removeIf {
            it.province == locationInfo.province &&
                    it.city == locationInfo.city &&
                    it.district == locationInfo.district
        }

        dataStore.edit { preferences ->
            preferences[SAVED_LOCATIONS] = toJson(savedLocations)
        }
    }

    // 获取位置的缓存键
    fun getLocationKey(locationInfo: LocationInfo): String {
        return "${locationInfo.province}_${locationInfo.city}_${locationInfo.district}"
    }

    // 保存重新排序的位置列表
    suspend fun saveReorderedLocations(locations: List<LocationInfo>) {
        dataStore.edit { preferences ->
            val locationsJson = gson.toJson(locations)
            preferences[SAVED_LOCATIONS] = locationsJson
            preferences[stringPreferencesKey(SAVED_LOCATIONS_KEY)] = locationsJson
        }
    }
}