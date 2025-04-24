package com.aikrai.location

import androidx.annotation.Keep
import com.tencent.map.geolocation.TencentLocation

// 通用位置信息模型
@Keep
data class LocationInfo(
    val province: String,
    val city: String,
    val district: String,
    val address: String,
    val latitude: Double = 0.0,  // 添加纬度
    val longitude: Double = 0.0, // 添加经度
    val source: String // 标记数据来源，如"amap"或"tencent"
) {
    companion object {
        // 从腾讯地图位置对象创建LocationInfo
        fun fromTencentLocation(location: TencentLocation): LocationInfo {
            return LocationInfo(
                province = location.province ?: "",
                city = location.city ?: "",
                district = location.district ?: "",
                address = location.address ?: "",
                latitude = location.latitude,
                longitude = location.longitude,
                source = "tencent_cached"
            )
        }
    }
}