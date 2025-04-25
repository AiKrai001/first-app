package com.aikrai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aikrai.location.LocationInfo
import kotlinx.coroutines.delay

/**
 * 城市搜索界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchScreen(
    searchResults: List<LocationInfo>,
    isSearching: Boolean,
    onBackClick: () -> Unit,
    onSearch: (String) -> Unit,
    onCitySelected: (LocationInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // 当搜索词变化时，执行搜索
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(500) // 添加延迟，减少频繁调用API
            onSearch(searchQuery)
        }
    }

    // 获取状态栏高度
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Scaffold(
        modifier = Modifier.padding(top = statusBarPadding.calculateTopPadding() * 0.5f),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("添加城市") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                modifier = Modifier.padding(top = 0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("输入城市名称") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )

            // 内容区域
            when {
                // 搜索中
                isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // 有搜索结果
                searchQuery.isNotBlank() && searchResults.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(searchResults) { location ->
                            SearchResultItem(location = location, onClick = { onCitySelected(location) })
                        }
                    }
                }
                // 搜索无结果
                searchQuery.isNotBlank() && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("没有找到相关城市，请尝试其他关键词")
                    }
                }
                // 初始状态或搜索词为空，显示热门城市
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "热门城市",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(getPopularCities()) { city ->
                                PopularCityItem(city = city, onClick = {
                                    searchQuery = city
                                    onSearch(city)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(location: LocationInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "位置",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                // 显示主要位置信息 - 使用粉色高亮显示地区名称
                Text(
                    text = buildAnnotatedString {
                        if (location.district.isNotEmpty()) {
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFFE91E63),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(location.district)
                            }
                        } else if (location.city.isNotEmpty()) {
                            append(location.city)
                        } else {
                            append(location.province)
                        }
                    },
                    fontSize = 16.sp
                )

                // 显示位置的完整路径，但不包含"中国"前缀
                Text(
                    text = buildAnnotatedString {
                        append(location.province)
                        append(location.city)

                        // 如果地址包含额外信息，则添加
                        if (location.address.isNotEmpty() &&
                            location.address != location.district &&
                            location.address != location.city
                        ) {
                            append(" ")
                            append(location.address)
                        }
                    },
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }

    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun PopularCityItem(city: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = city,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

private fun getPopularCities(): List<String> {
    return listOf(
        "北京", "上海", "广州", "深圳", "杭州",
        "南京", "武汉", "成都", "重庆", "西安",
        "苏州", "天津", "长沙", "青岛", "厦门"
    )
} 