package com.aikrai.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.Keep
import com.aikrai.model.AirQuality
import com.aikrai.model.CaiYunDaily
import com.aikrai.model.CaiYunHourly
import com.aikrai.model.CaiYunRealtime
import com.aikrai.model.CaiYunWeatherConsolidatedData
import com.aikrai.model.DailyForecast
import com.aikrai.model.HourlyForecast
import com.aikrai.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 彩云天气服务类
 *
 * 提供获取天气数据的方法，使用Retrofit和OkHttp进行网络请求
 */
@Keep
class CaiYunWeatherService(private val context: Context) {
    /** 彩云天气API的基础URL */
    private val BASE_URL = "https://api.caiyunapp.com/"

    // 优先使用AndroidManifest中的配置，如果获取失败则使用此备用值
    private val BACKUP_TOKEN = ""

    /** 从AndroidManifest.xml中获取彩云天气API Token */
    private val token: String by lazy {
        try {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val bundle: Bundle = applicationInfo.metaData
            val manifestToken = bundle.getString("CaiYunWeatherToken") ?: ""

            // 如果从Manifest获取的令牌为空，则使用备用令牌
            if (manifestToken.isBlank()) {
                Timber.w("从Manifest获取Token失败，使用备用Token")
                BACKUP_TOKEN
            } else {
                manifestToken
            }
        } catch (e: Exception) {
            Timber.e(e, "获取彩云天气Token失败，使用备用Token")
            BACKUP_TOKEN // 返回备用Token
        }
    }

    /** 创建日志拦截器，用于调试网络请求 */
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Timber.tag("OkHttp").d(message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /** OkHttp客户端实例 */
    @Keep
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor) // 添加日志拦截器
        .build()

    /** Retrofit实例 */
    @Keep
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** 彩云天气API服务 */
    @Keep
    private val weatherApi: CaiYunWeatherApi = retrofit.create(CaiYunWeatherApi::class.java)

    /**
     * 获取天气综合数据
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param alert 是否获取预警数据，默认为true
     * @param dailysteps 获取天级预报的天数，范围[1, 15]，默认为7
     * @param hourlysteps 获取小时级预报的小时数，范围[1, 360]，默认为24
     * @return 包含天气数据的Result对象，成功时包含CaiYunWeatherConsolidatedData，失败时包含异常
     */
    @Keep
    suspend fun getWeatherConsolidatedData(
        longitude: Double,
        latitude: Double,
        alert: Boolean? = true,
        dailysteps: Int? = 7,
        hourlysteps: Int? = 24,
    ): Result<CaiYunWeatherConsolidatedData> {
        return withContext(Dispatchers.IO) {
            try {
                val location = "$longitude,$latitude"
                Timber.d("请求参数: location=$location, alert=$alert, dailysteps=$dailysteps, hourlysteps=$hourlysteps")
                Timber.d("使用的Token: $token")

                // 使用预先定义的weatherApi引用
                val response = weatherApi.getWeatherData(token, location, alert, dailysteps, hourlysteps)
                Timber.i("天气API请求成功: status=${response.status}")
                Result.success(response)
            } catch (e: Exception) {
                Timber.e(e, "获取天气数据异常")
                Result.failure(e)
            }
        }
    }

    /**
     * 获取天气数据
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param province 省份
     * @param city 城市
     * @param district 区县
     * @param yesterdayWeather 昨天的天气数据（可选），用于在每日预报中显示昨天的天气
     * @return 包含天气数据的Result对象，成功时包含WeatherData，失败时包含异常
     */
    suspend fun getWeatherData(
        longitude: Double,
        latitude: Double,
        province: String,
        city: String,
        district: String,
        yesterdayWeather: WeatherData? = null
    ): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("开始获取天气数据: 经度=$longitude, 纬度=$latitude, 省=$province, 市=$city, 区=$district")
                Timber.d("使用的API token: $token")

                val consolidatedDataResult = getWeatherConsolidatedData(
                    longitude = longitude,
                    latitude = latitude,
                    dailysteps = 8,
                    hourlysteps = 24
                )

                consolidatedDataResult.fold(
                    onSuccess = { data ->
                        Timber.d("成功获取天气数据: status=${data.status}")
                        Result.success(parseWeatherData(data, province, city, district, yesterdayWeather))
                    },
                    onFailure = { e ->
                        Timber.e(e, "获取天气数据失败")
                        Result.failure(e)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "获取天气数据过程中发生异常")
                Result.failure(e)
            }
        }
    }

    /**
     * 解析彩云天气API返回的数据
     *
     * @param data 彩云天气合并数据对象
     * @param province 省份
     * @param city 城市
     * @param district 区县
     * @param yesterdayWeather 昨天的天气数据（可选）
     * @return 解析后的天气数据对象
     */
    private fun parseWeatherData(
        data: CaiYunWeatherConsolidatedData,
        province: String,
        city: String,
        district: String,
        yesterdayWeather: WeatherData? = null
    ): WeatherData {
        try {
            val result = data.result

            // 解析实时天气数据
            val realtime = result.realtime
            if (realtime == null) {
                return createDefaultWeatherData(province, city, district)
            }

            val temperature = roundToOneDecimalWithCustomRule(realtime.temperature)
            val humidity = realtime.humidity * 100.0
            val skycon = realtime.skycon
            val weatherCondition = getSkyconDescription(skycon)

            // 解析更多实时数据
            val apparentTemperature = roundToOneDecimalWithCustomRule(realtime.apparent_temperature)
            val pressure = realtime.pressure
            val visibility = realtime.visibility
            val windSpeed = realtime.wind.speed.toDouble()

            // 解析降水量
            val precipitation = realtime.precipitation.local.intensity.toDouble() / 10.0

            // 解析预报关键点
            val forecastKeypoint = result.forecast_keypoint

            // 解析空气质量
            val airQuality = parseAirQuality(realtime.air_quality)

            // 解析小时级预报
            val hourlyData = result.hourly
            val hourlyForecasts = if (hourlyData != null) {
                parseHourlyForecast(hourlyData)
            } else {
                emptyList()
            }

            // 解析天级预报
            val dailyData = result.daily
            val dailyForecasts = if (dailyData != null) {
                parseDailyForecast(dailyData, yesterdayWeather)
            } else {
                emptyList()
            }

            // 获取日出日落时间
            var sunriseTime = "06:00"
            var sunsetTime = "18:00"
            if (dailyData != null && dailyData.astro.isNotEmpty()) {
                try {
                    sunriseTime = dailyData.astro[0].sunrise.time
                    sunsetTime = dailyData.astro[0].sunset.time
                } catch (e: Exception) {
                    Timber.e(e, "解析日出日落时间失败")
                }
            }

            // 获取今天的最高温和最低温
            val todayTemps = if (dailyData != null && dailyData.temperature.isNotEmpty()) {
                Pair(
                    roundToOneDecimalWithCustomRule(dailyData.temperature[0].max.toDouble()),
                    roundToOneDecimalWithCustomRule(dailyData.temperature[0].min.toDouble())
                )
            } else if (dailyForecasts.isNotEmpty()) {
                Pair(
                    roundToOneDecimalWithCustomRule(dailyForecasts[0].highTemperature),
                    roundToOneDecimalWithCustomRule(dailyForecasts[0].lowTemperature)
                )
            } else {
                Pair(temperature + 5.0, temperature - 5.0)
            }

            return WeatherData(
                province = province,
                city = city,
                district = district,
                currentTemperature = temperature,
                weatherCondition = weatherCondition,
                humidity = humidity,
                highTemperature = todayTemps.first,
                lowTemperature = todayTemps.second,
                windSpeed = windSpeed,
                apparentTemperature = apparentTemperature,
                pressure = pressure,
                visibility = visibility,
                precipitation = precipitation,
                sunriseTime = sunriseTime,
                sunsetTime = sunsetTime,
                forecastKeypoint = forecastKeypoint,
                hourlyForecast = hourlyForecasts,
                dailyForecast = dailyForecasts,
                airQuality = airQuality
            )
        } catch (e: Exception) {
            Timber.e(e, "解析天气数据失败")
            return createDefaultWeatherData(province, city, district)
        }
    }

    /**
     * 解析空气质量数据
     *
     * @param airQualityData 彩云天气空气质量数据
     * @return 解析后的空气质量数据对象
     */
    private fun parseAirQuality(airQualityData: CaiYunRealtime.AirQuality): AirQuality {
        return try {
            AirQuality(
                index = airQualityData.aqi.chn.toDouble(),
                level = airQualityData.description.chn,
                pm10 = airQualityData.pm10.toDouble(),
                pm25 = airQualityData.pm25.toDouble(),
                no2 = airQualityData.no2.toDouble(),
                so2 = airQualityData.so2.toDouble(),
                co = airQualityData.co,
                o3 = airQualityData.o3.toDouble()
            )
        } catch (e: Exception) {
            Timber.e(e, "解析空气质量数据失败")
            AirQuality(
                index = 50.0,
                level = "良",
                pm10 = 50.0,
                pm25 = 30.0,
                no2 = 20.0,
                so2 = 10.0,
                co = 0.6,
                o3 = 100.0
            )
        }
    }

    /**
     * 解析小时级预报
     *
     * @param hourlyData 彩云天气小时级预报数据
     * @return 解析后的小时级预报列表
     */
    private fun parseHourlyForecast(hourlyData: CaiYunHourly): List<HourlyForecast> {
        val forecasts = mutableListOf<HourlyForecast>()

        try {
            val skycons = hourlyData.skycon
            val temperatures = hourlyData.temperature
            val precipitations = hourlyData.precipitation
            val winds = hourlyData.wind
            val humidities = hourlyData.humidity

            val count = minOf(skycons.size, temperatures.size, 24)

            for (i in 0 until count) {
                val skycon = skycons[i]
                val temperature = temperatures[i]

                // 解析时间
                val datetime = LocalDateTime.parse(skycon.datetime, DateTimeFormatter.ISO_DATE_TIME)
                val hour = datetime.format(DateTimeFormatter.ofPattern("HH:00"))

                // 获取降水量
                val precipitation = if (i < precipitations.size) {
                    precipitations[i].value / 100.0 // 转为小数
                } else 0.0

                // 获取风速和方向
                val windSpeed = if (i < winds.size) winds[i].speed.toDouble() else 0.0
                val windDirection = if (i < winds.size) winds[i].direction.toDouble() else 0.0

                // 获取湿度
                val humidity = if (i < humidities.size) humidities[i].value else 0.0

                forecasts.add(
                    HourlyForecast(
                        hour = hour,
                        temperature = temperature.value.toDouble(),
                        weatherCondition = getSkyconDescription(skycon.value),
                        precipitation = precipitation,
                        windSpeed = windSpeed,
                        windDirection = windDirection,
                        humidity = humidity
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "解析小时级预报失败")
        }

        return forecasts
    }

    /**
     * 解析天级预报
     *
     * @param dailyData 彩云天气天级预报数据
     * @param yesterdayWeather 昨天的天气数据（可选）
     * @return 解析后的天级预报列表
     */
    private fun parseDailyForecast(dailyData: CaiYunDaily, yesterdayWeather: WeatherData? = null): List<DailyForecast> {
        val forecasts = mutableListOf<DailyForecast>()

        try {
            val skycons = dailyData.skycon
            val temperatures = dailyData.temperature
            val precipitations = dailyData.precipitation
            val humidities = dailyData.humidity
            val winds = dailyData.wind
            val astros = dailyData.astro

            val count = minOf(skycons.size, temperatures.size, 8)

            // 获取当前时间，用于确定今天、明天等
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            // 添加昨天的数据（如果有缓存的昨日天气数据则使用，否则生成默认数据）
            if (yesterdayWeather != null) {
                // 使用缓存的昨日天气数据
                val yesterdayForecast = DailyForecast(
                    day = "昨天",
                    date = yesterday.toString(),
                    weatherCondition = yesterdayWeather.weatherCondition,
                    highTemperature = yesterdayWeather.highTemperature,
                    lowTemperature = yesterdayWeather.lowTemperature,
                    precipitation = yesterdayWeather.precipitation,
                    precipitationProbability = 0.0,
                    windSpeed = yesterdayWeather.windSpeed,
                    humidity = yesterdayWeather.humidity,
                    sunriseTime = yesterdayWeather.sunriseTime,
                    sunsetTime = yesterdayWeather.sunsetTime
                )
                forecasts.add(yesterdayForecast)
            } else {
                // 没有昨日天气数据，生成默认数据
                val yesterdayForecast = DailyForecast(
                    day = "昨天",
                    date = yesterday.toString(),
                    weatherCondition = "无",
                    highTemperature = 0.0,
                    lowTemperature = 0.0,
                    precipitation = 0.0,
                    precipitationProbability = 0.0,
                    windSpeed = 2.0,
                    humidity = 0.4,
                    sunriseTime = "",
                    sunsetTime = ""
                )
                forecasts.add(yesterdayForecast)
            }

            for (i in 0 until count) {
                val skycon = skycons[i]
                val temperature = temperatures[i]

                // 解析日期
                val dateStr = skycon.date.substring(0, 10)
                val date = LocalDate.parse(dateStr)

                // 确定显示的日期文本
                val dayName = when {
                    date.isEqual(today) -> "今天"
                    date.isEqual(today.plusDays(1)) -> "明天"
                    else -> {
                        when (date.dayOfWeek.value) {
                            1 -> "周一"
                            2 -> "周二"
                            3 -> "周三"
                            4 -> "周四"
                            5 -> "周五"
                            6 -> "周六"
                            7 -> "周日"
                            else -> "未知"
                        }
                    }
                }

                // 获取降水信息
                val precip = if (i < precipitations.size) precipitations[i].avg / 10.0 else 0.0
                val precipProb = if (i < precipitations.size) precipitations[i].probability.toDouble() else 0.0

                // 获取湿度
                val humidity = if (i < humidities.size) humidities[i].avg else 0.0

                // 获取风速
                val windSpeed = if (i < winds.size) winds[i].avg.speed else 0.0

                // 获取日出日落时间
                val sunriseTime = if (i < astros.size) astros[i].sunrise.time else "06:00"
                val sunsetTime = if (i < astros.size) astros[i].sunset.time else "18:00"

                forecasts.add(
                    DailyForecast(
                        day = dayName,
                        date = dateStr,
                        weatherCondition = getSkyconDescription(skycon.value),
                        highTemperature = temperature.max.toDouble(),
                        lowTemperature = temperature.min.toDouble(),
                        precipitation = precip,
                        precipitationProbability = precipProb,
                        windSpeed = windSpeed,
                        humidity = humidity,
                        sunriseTime = sunriseTime,
                        sunsetTime = sunsetTime
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "解析天级预报失败")
            if (forecasts.isEmpty()) {
                // 添加默认数据
                addDefaultDailyForecasts(forecasts)
            }
        }

        return forecasts
    }

    /**
     * 添加默认的天级预报数据
     *
     * @param forecasts 预报列表
     */
    private fun addDefaultDailyForecasts(forecasts: MutableList<DailyForecast>) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        forecasts.add(
            DailyForecast(
                day = "昨天",
                date = yesterday.toString(),
                weatherCondition = "多云",
                highTemperature = 20.0,
                lowTemperature = 10.0,
                precipitation = 0.0,
                precipitationProbability = 0.0,
                windSpeed = 2.0,
                humidity = 0.4,
                sunriseTime = "06:00",
                sunsetTime = "18:00"
            )
        )

        forecasts.add(
            DailyForecast(
                day = "今天",
                date = today.toString(),
                weatherCondition = "晴",
                highTemperature = 22.0,
                lowTemperature = 12.0,
                precipitation = 0.0,
                precipitationProbability = 0.0,
                windSpeed = 2.0,
                humidity = 0.3,
                sunriseTime = "06:00",
                sunsetTime = "18:00"
            )
        )

        for (i in 1..7) {
            val date = today.plusDays(i.toLong())
            val dayName = if (i == 1) "明天" else {
                when (date.dayOfWeek.value) {
                    1 -> "周一"
                    2 -> "周二"
                    3 -> "周三"
                    4 -> "周四"
                    5 -> "周五"
                    6 -> "周六"
                    7 -> "周日"
                    else -> "未知"
                }
            }

            forecasts.add(
                DailyForecast(
                    day = dayName,
                    date = date.toString(),
                    weatherCondition = "多云",
                    highTemperature = 22.0 - i,
                    lowTemperature = 12.0 - i,
                    precipitation = 0.0,
                    precipitationProbability = 0.0,
                    windSpeed = 2.0,
                    humidity = 0.4,
                    sunriseTime = "06:00",
                    sunsetTime = "18:00"
                )
            )
        }
    }

    /**
     * 将彩云天气的skycon转换为中文天气描述
     *
     * @param skycon 天气现象代码
     * @return 中文天气描述
     */
    private fun getSkyconDescription(skycon: String): String {
        return when (skycon) {
            "CLEAR_DAY" -> "晴"
            "CLEAR_NIGHT" -> "晴"
            "PARTLY_CLOUDY_DAY" -> "多云"
            "PARTLY_CLOUDY_NIGHT" -> "多云"
            "CLOUDY" -> "阴"
            "LIGHT_HAZE" -> "轻度雾霾"
            "MODERATE_HAZE" -> "中度雾霾"
            "HEAVY_HAZE" -> "重度雾霾"
            "LIGHT_RAIN" -> "小雨"
            "MODERATE_RAIN" -> "中雨"
            "HEAVY_RAIN" -> "大雨"
            "STORM_RAIN" -> "暴雨"
            "FOG" -> "雾"
            "LIGHT_SNOW" -> "小雪"
            "MODERATE_SNOW" -> "中雪"
            "HEAVY_SNOW" -> "大雪"
            "STORM_SNOW" -> "暴雪"
            "DUST" -> "浮尘"
            "SAND" -> "沙尘"
            "WIND" -> "大风"
            else -> "未知"
        }
    }

    /**
     * 创建默认天气数据
     *
     * @param province 省份
     * @param city 城市
     * @param district 区县
     * @return 默认的天气数据对象
     */
    @SuppressLint("DefaultLocale")
    private fun createDefaultWeatherData(province: String, city: String, district: String): WeatherData {
        val hourlyForecasts = List(24) { index ->
            val hour = (LocalDateTime.now().hour + index) % 24
            HourlyForecast(
                hour = String.format("%02d:00", hour),
                temperature = (20 - (index / 3)).toDouble(),
                weatherCondition = "mock 多云",
                precipitation = 0.0,
                windSpeed = 2.0,
                windDirection = 180.0,
                humidity = 0.4
            )
        }

        val dailyForecasts = mutableListOf<DailyForecast>()
        addDefaultDailyForecasts(dailyForecasts)

        return WeatherData(
            province = province,
            city = city,
            district = district,
            currentTemperature = 22.0,
            weatherCondition = "mock 多云",
            humidity = 12.0,
            highTemperature = 23.0,
            lowTemperature = 7.0,
            windSpeed = 5.0,
            apparentTemperature = 20.0,
            pressure = 1013.25,
            visibility = 10.0,
            precipitation = 0.0,
            sunriseTime = "06:45",
            sunsetTime = "18:56",
            forecastKeypoint = "mock 未来两小时不会下雨，放心出门吧",
            hourlyForecast = hourlyForecasts,
            dailyForecast = dailyForecasts,
            airQuality = AirQuality(
                index = 57.0,
                level = "mock 良",
                pm10 = 65.0,
                pm25 = 30.0,
                no2 = 24.0,
                so2 = 7.0,
                co = 0.6,
                o3 = 156.0
            )
        )
    }
}

/**
 * 实现"五舍六入"并保留一位小数
 *
 * @param value 原始数值
 * @return 处理后的数值，保留一位小数
 */
fun roundToOneDecimalWithCustomRule(value: Double): Double {
    // 将数值放大100倍，获取小数点后两位
    val multiplied = value * 100.0
    val intValue = multiplied.toInt()

    // 获取小数点后第二位
    val secondDecimal = intValue % 10

    // 根据"五舍六入"规则处理
    val rounded = when {
        secondDecimal < 5 -> intValue / 10 // 小于5，舍去
        secondDecimal >= 6 -> intValue / 10 + 1 // 大于或等于6，进位
        else -> intValue / 10 // 等于5，保持不变
    }

    // 转换回保留一位小数的结果
    return rounded / 10.0
}
