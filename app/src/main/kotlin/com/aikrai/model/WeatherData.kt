package com.aikrai.model

import androidx.annotation.Keep

@Keep
data class WeatherData(
    val province: String,
    val city: String,
    val district: String,
    val currentTemperature: Double,
    val weatherCondition: String,
    val humidity: Double,
    val highTemperature: Double,
    val lowTemperature: Double,
    val windSpeed: Double,
    val apparentTemperature: Double,
    val pressure: Double,
    val visibility: Double,
    val precipitation: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val forecastKeypoint: String,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>,
    val airQuality: AirQuality
) {
    // 获取显示位置，优先显示区，如果区为空则显示市
    val displayLocation: String get() = district.ifEmpty { city }
}

@Keep
data class HourlyForecast(
    val hour: String,
    val temperature: Double,
    val weatherCondition: String,
    val precipitation: Double = 0.0,
    val windSpeed: Double = 0.0,
    val windDirection: Double = 0.0,
    val humidity: Double = 0.0
)

@Keep
data class DailyForecast(
    val day: String,
    val date: String,
    val weatherCondition: String,
    val highTemperature: Double,
    val lowTemperature: Double,
    val precipitation: Double = 0.0,
    val precipitationProbability: Double = 0.0,
    val windSpeed: Double = 0.0,
    val humidity: Double = 0.0,
    val sunriseTime: String = "",
    val sunsetTime: String = ""
)

@Keep
data class AirQuality(
    val index: Double,
    val level: String,
    val pm10: Double,
    val pm25: Double,
    val no2: Double,
    val so2: Double,
    val co: Double,
    val o3: Double
)

// 模拟数据
@Keep
object MockWeatherData {
    // 简化的基础天气数据，仅保留必要结构
    val weatherData = WeatherData(
        province = "",
        city = "",
        district = "",
        currentTemperature = 0.0,
        weatherCondition = "未知",
        humidity = 0.0,
        highTemperature = 0.0,
        lowTemperature = 0.0,
        windSpeed = 0.0,
        apparentTemperature = 0.0,
        pressure = 0.0,
        visibility = 0.0,
        precipitation = 0.0,
        sunriseTime = "06:00",
        sunsetTime = "18:00",
        forecastKeypoint = "暂无天气信息",
        hourlyForecast = emptyList(),
        dailyForecast = emptyList(),
        airQuality = AirQuality(
            index = 0.0,
            level = "未知",
            pm10 = 0.0,
            pm25 = 0.0,
            no2 = 0.0,
            so2 = 0.0,
            co = 0.0,
            o3 = 0.0
        )
    )

    // 根据位置信息更新天气数据
    fun getWeatherDataForLocation(province: String, city: String, district: String): WeatherData {
        return weatherData.copy(
            province = province,
            city = city,
            district = district
        )
    }
} 