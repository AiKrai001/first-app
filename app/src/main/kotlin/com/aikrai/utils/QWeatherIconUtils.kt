package com.aikrai.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * QWeather图标工具类
 * 用于将天气状况映射到对应的Material图标
 */
object QWeatherIconUtils {

    /**
     * 根据天气状况获取对应的图标
     * @param weatherCondition 天气状况文本，如"晴"，"多云"等
     * @return 对应的Material图标
     */
    fun getWeatherIcon(weatherCondition: String): ImageVector {
        return when (weatherCondition) {
            "晴" -> Icons.Default.WbSunny
            "多云" -> Icons.Default.WbCloudy
            "阴" -> Icons.Default.Cloud
            "小雨" -> Icons.Default.Grain
            "中雨" -> Icons.Default.Grain
            "大雨" -> Icons.Default.Grain
            "暴雨" -> Icons.Default.Thunderstorm
            "雷阵雨" -> Icons.Default.Thunderstorm
            "小雪" -> Icons.Default.AcUnit
            "中雪" -> Icons.Default.AcUnit
            "大雪" -> Icons.Default.AcUnit
            "雾" -> Icons.Default.FilterDrama
            "霾" -> Icons.Default.Air
            else -> Icons.Default.Cloud
        }
    }

    /**
     * 根据天气状况获取对应的图标颜色
     * @param weatherCondition 天气状况文本
     * @return 对应的颜色
     */
    fun getWeatherIconTint(weatherCondition: String): Color {
        return when (weatherCondition) {
            "晴" -> Color(0xFFFFA500) // 橙色
            "多云" -> Color(0xFFBBDEFB) // 淡蓝色
            "阴" -> Color(0xFFB0BEC5) // 灰蓝色
            "小雨" -> Color(0xFF4FC3F7) // 浅蓝色
            "中雨" -> Color(0xFF29B6F6) // 蓝色
            "大雨" -> Color(0xFF039BE5) // 深蓝色
            "暴雨", "雷阵雨" -> Color(0xFF0277BD) // 更深蓝色
            "小雪", "中雪", "大雪" -> Color(0xFFCFD8DC) // 淡灰蓝色
            "雾" -> Color(0xFFB0BEC5) // 灰蓝色
            "霾" -> Color(0xFF90A4AE) // 深灰蓝色
            else -> Color.White
        }
    }
}