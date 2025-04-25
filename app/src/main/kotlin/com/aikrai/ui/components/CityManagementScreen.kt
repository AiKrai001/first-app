package com.aikrai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aikrai.location.LocationInfo
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * 城市管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityManagementScreen(
    cities: List<CityWeatherInfo>,
    onBackClick: () -> Unit,
    onAddCityClick: () -> Unit,
    onCityClick: (CityWeatherInfo) -> Unit,
    onCityDelete: ((CityWeatherInfo) -> Unit)? = null,
    onCityReorder: ((List<CityWeatherInfo>) -> Unit)? = null
) {
    // 获取状态栏高度
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    // 创建可变状态列表来管理城市
    val citiesList = remember { mutableStateListOf<CityWeatherInfo>() }

    // 监听外部cities列表的变化并更新内部状态
    LaunchedEffect(cities) {
        citiesList.clear()
        citiesList.addAll(cities)
    }

    // 创建可重排序状态
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            citiesList.apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            // 拖拽结束时通知外部数据已变化
            onCityReorder?.invoke(citiesList.toList())
        }
    )

    // 创建一个处理删除操作的函数，既更新UI又通知外部
    val handleDelete = { city: CityWeatherInfo ->
        // 从内部列表中删除
        citiesList.remove(city)
        // 通知外部
        onCityDelete?.invoke(city)
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
            )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                // 添加顶部padding确保顶部栏不被状态栏遮挡
                .padding(top = statusBarPadding.calculateTopPadding() * 0.5f),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "管理城市",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onAddCityClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加城市",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .reorderable(reorderableState),
                state = reorderableState.listState
            ) {
                items(citiesList, { it.hashCode().toString() }) { city ->
                    ReorderableItem(reorderableState, key = city.hashCode().toString()) { isDragging ->
                        CityWeatherItem(
                            city = city,
                            onClick = { onCityClick(city) },
                            onDelete = if (onCityDelete != null) {
                                { handleDelete(city) }
                            } else null,
                            isDragging = isDragging,
                            reorderableState = reorderableState
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

/**
 * 城市天气项
 */
@Composable
fun CityWeatherItem(
    city: CityWeatherInfo,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isDragging: Boolean = false,
    reorderableState: org.burnoutcrew.reorderable.ReorderableLazyListState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
            .background(if (isDragging) Color.Gray.copy(alpha = 0.2f) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 拖拽手柄图标
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "拖动排序",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(end = 8.dp)
                .detectReorderAfterLongPress(reorderableState)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (city.district == city.city) city.city else "${city.district}, ${city.city}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = city.province,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${city.temperature.toInt()}°C",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = city.weatherCondition,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        if (onDelete != null) {
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(8.dp) // 增大点击区域
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 城市天气信息数据类
 */
data class CityWeatherInfo(
    val province: String,
    val city: String,
    val district: String,
    val temperature: Double,
    val weatherCondition: String
)

/**
 * 扩展函数：从LocationInfo转换为CityWeatherInfo
 */
fun LocationInfo.toCityWeatherInfo(temperature: Double, weatherCondition: String): CityWeatherInfo {
    return CityWeatherInfo(
        province = this.province,
        city = this.city,
        district = this.district,
        temperature = temperature,
        weatherCondition = weatherCondition
    )
} 