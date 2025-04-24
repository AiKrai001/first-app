package com.aikrai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aikrai.location.LocationInfo
import com.aikrai.model.AirQuality
import com.aikrai.model.DailyForecast
import com.aikrai.model.HourlyForecast
import com.aikrai.utils.QWeatherIconUtils

@Composable
fun LocationHeader(
    location: String,
    locationInfo: LocationInfo? = null,
    onAddLocationClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    locationCount: Int = 0,
    currentLocationIndex: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧加号按钮
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "添加位置",
            tint = Color.White,
            modifier = Modifier.clickable { onAddLocationClick() }
        )

        // 中间位置信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // 根据locationInfo判断是否显示定位图标
            if (locationInfo?.source?.contains("tencent_localization") == true) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "位置",
                    tint = Color.White
                )
            }
        }

        // 右侧菜单按钮
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "更多选项",
            tint = Color.White,
            modifier = Modifier.clickable { onMenuClick() }
        )
    }

    // 只有当位置数量大于1时才显示分页点
    if (locationCount > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(locationCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (index == currentLocationIndex) Color.White else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun CurrentWeather(
    currentTemperature: Double,
    weatherCondition: String,
    humidity: Double,
    highTemperature: Double,
    lowTemperature: Double,
    windSpeed: Double,
    apparentTemperature: Double = 0.0,
    precipitation: Double = 0.0,
    visibility: Double = 10.0,
    forecastKeypoint: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "风速 ${windSpeed}km/h, 湿度 ${String.format("%.1f", humidity)}%",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$currentTemperature",
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "°C",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = weatherCondition,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 15.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$highTemperature°C/$lowTemperature°C",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "体感温度: $apparentTemperature°C",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Text(
            text = forecastKeypoint,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun HourlyForecastCard(
    hourlyForecasts: List<HourlyForecast>,
    sunriseTime: String = "06:00",
    sunsetTime: String = "18:00"
) {
    if (hourlyForecasts.isEmpty()) return

    // 计算温度范围
    val maxTemp = hourlyForecasts.maxOf { it.temperature }
    val minTemp = hourlyForecasts.minOf { it.temperature }
    val tempRange = (maxTemp - minTemp).coerceAtLeast(5.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4B88CA)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 5.dp) // 调整为 7dp，减少 2dp
        ) {
            // 头部标题和日出日落时间
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "24 小时",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 13.dp)
                )
                Row {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "日出",
                        tint = Color.White,
                        modifier = Modifier.padding(top = 4.dp).size(18.dp)
                    )
                    Text(
                        text = " 日出 $sunriseTime",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "日落",
                        tint = Color.White,
                        modifier = Modifier.padding(top = 4.dp).size(18.dp)
                    )
                    Text(
                        text = " 日落 $sunsetTime",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 天气预报图表区域
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .height(135.dp) // 调整高度为 135.dp
                    .fillMaxWidth()
            ) {
                val chartHeight = 76.dp
                val pointSize = 6.dp
                val itemWidth = 70.dp

                // 计算总内容宽度
                val totalWidth = itemWidth * hourlyForecasts.size

                // 可滚动区域
                Box(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .width(totalWidth)
                        .height(135.dp) // 与外部 Box 高度一致
                ) {
                    // 绘制折线图
                    Canvas(
                        modifier = Modifier
                            .width(totalWidth)
                            .height(chartHeight)
                            .padding(bottom = 10.dp)
                    ) {
                        if (hourlyForecasts.size > 1) {
                            // 将每个点的坐标计算后存入一个 List
                            val segmentWidth = size.width / hourlyForecasts.size
                            val points = hourlyForecasts.mapIndexed { index, forecast ->
                                val normalizedTemp = ((forecast.temperature - minTemp) / tempRange)
                                    .coerceIn(0.0, 1.0)
                                    .toFloat()
                                val x = segmentWidth / 2 + index * segmentWidth
                                val y = size.height * (1f - normalizedTemp)
                                Offset(x, y)
                            }
                            // 构造 Path
                            val path = Path().apply {
                                createSmoothLine(points)
                            }
                            // 在 Canvas 上绘制平滑曲线
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(width = 1.dp.toPx()) // 如果需要线条更柔和，可以适当调大 stroke 宽度
                            )

                        }
                    }

                    // 每小时预报内容
                    hourlyForecasts.forEachIndexed { index, forecast ->
                        val canvasPaddingBottom = 10.dp
                        val canvasActualHeight = chartHeight - canvasPaddingBottom
                        val normalizedTemp = ((forecast.temperature - minTemp) / tempRange).coerceIn(0.0, 1.0)

                        val pointY = (canvasActualHeight.value * (1 - normalizedTemp.toFloat())).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(itemWidth)
                                .offset(x = itemWidth * index)
                        ) {
                            // 温度值和点的容器
                            Box(
                                modifier = Modifier
                                    .height(chartHeight)
                                    .fillMaxWidth()
                            ) {
                                // 温度值
                                Text(
                                    text = "${forecast.temperature.toInt()}°C",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = pointY - 22.dp)
                                )

                                // 温度点
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = pointY - 3.dp)
                                        .size(pointSize)
                                        .background(Color.White, CircleShape)
                                )
                            }

                            // 天气图标
                            val hour = forecast.hour.split(":")[0].toIntOrNull() ?: 0
                            val isDaytime = hour in 6..17
                            Icon(
                                imageVector = if (isDaytime) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = forecast.weatherCondition,
                                tint = if (isDaytime) Color.Yellow else Color(0xFFFFC107),
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 4.dp)
                            )

                            // 时间
                            Text(
                                text = forecast.hour,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 3.dp, bottom = 0.dp) // 调整为 0dp，减少 3dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastCard(dailyForecasts: List<DailyForecast>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4B88CA)  // 使用蓝色背景以匹配UI图
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 每日预报列表
            Column {
                dailyForecasts.forEachIndexed { index, forecast ->
                    // 判断是否是昨天（第一行）
                    val isYesterday = index == 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 星期
                        Text(
                            text = forecast.day,
                            color = if (isYesterday) Color.White.copy(alpha = 0.5f) else Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.width(40.dp)
                        )

                        // 天气文字描述
                        Text(
                            text = forecast.weatherCondition,
                            color = if (isYesterday) Color.White.copy(alpha = 0.5f) else Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.width(80.dp)
                        )

                        // 天气图标
                        Icon(
                            imageVector = QWeatherIconUtils.getWeatherIcon(forecast.weatherCondition),
                            contentDescription = forecast.weatherCondition,
                            tint = if (isYesterday)
                                Color.White.copy(alpha = 0.5f)
                            else
                                QWeatherIconUtils.getWeatherIconTint(forecast.weatherCondition),
                            modifier = Modifier.size(24.dp)
                        )

                        // 占位空间，使温度右对齐
                        Spacer(modifier = Modifier.weight(1f))

                        // 温度显示
                        Text(
                            text = "${forecast.highTemperature.toInt()}°C / ${forecast.lowTemperature.toInt()}°C",
                            color = if (isYesterday) Color.White.copy(alpha = 0.5f) else Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // 分隔线（除了最后一个元素）
                    if (forecast != dailyForecasts.last()) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AirQualityCard(airQuality: AirQuality) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x4D000000)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "空气质量",
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AQI指数显示
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 10.dp.toPx()

                        // 绘制背景圆环
                        drawArc(
                            color = Color.White.copy(alpha = 0.3f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(
                                width = 10.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // 计算进度（假设最大AQI为500）
                        val progress = airQuality.index / 500f
                        val sweepAngle = 270f * progress

                        // 绘制进度圆环
                        drawArc(
                            color = when {
                                airQuality.index <= 50 -> Color.Green
                                airQuality.index <= 100 -> Color.Yellow
                                airQuality.index <= 150 -> Color(0xFFFFA500) // Orange
                                else -> Color.Red
                            },
                            startAngle = 135f,
                            sweepAngle = sweepAngle.toFloat(),
                            useCenter = false,
                            style = Stroke(
                                width = 10.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${airQuality.index}",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AQI",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = airQuality.level,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 污染物指标
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PollutantItem(name = "PM10", value = "${airQuality.pm10}")
                        PollutantItem(name = "PM2.5", value = "${airQuality.pm25}")
                        PollutantItem(name = "NO₂", value = "${airQuality.no2}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PollutantItem(name = "SO₂", value = "${airQuality.so2}")
                        PollutantItem(name = "CO", value = "${airQuality.co}")
                        PollutantItem(name = "O₃", value = "${airQuality.o3}")
                    }
                }
            }
        }
    }
}

@Composable
fun PollutantItem(name: String, value: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 将一组 [Offset] 坐标转换为一条平滑的 Path
 */
fun Path.createSmoothLine(points: List<Offset>) {
    if (points.size < 2) return

    val firstPoint = points.first()
    moveTo(firstPoint.x, firstPoint.y)

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        // 取前后点的中点，作为控制点来实现较柔和的连接
        val midPoint = Offset(
            (prev.x + curr.x) / 2f,
            (prev.y + curr.y) / 2f
        )
        // 使用二次贝塞尔曲线，将前一点和当前点之间用 midPoint 作为过渡
        quadraticTo(prev.x, prev.y, midPoint.x, midPoint.y)
    }

    // 最后连接到最终点
    val lastPoint = points.last()
    lineTo(lastPoint.x, lastPoint.y)
}
