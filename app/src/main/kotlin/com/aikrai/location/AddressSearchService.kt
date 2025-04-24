package com.aikrai.location

import android.content.Context
import android.util.Log
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.httpresponse.BaseObject
import com.tencent.lbssearch.httpresponse.HttpResponseListener
import com.tencent.lbssearch.`object`.param.SuggestionParam
import com.tencent.lbssearch.`object`.result.SuggestionResultObject
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 地址搜索服务类，使用腾讯地图API进行地址解析
 */
class AddressSearchService(context: Context) {

    private val TAG = "AddressSearchService"
    private val tencentSearch: TencentSearch

    init {
        // 设置地图SDK隐私政策
        TencentMapInitializer.setAgreePrivacy(true)

        tencentSearch = TencentSearch(context)
    }

    /**
     * 将地址转换为地理坐标
     * @param address 要转换的地址，格式为省市区详细地址
     * @return 包含经纬度和地址信息的LocationInfo对象
     */
    suspend fun address2Geo(address: String): LocationInfo {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val suggestionParam = SuggestionParam()
                    .keyword(address)
                    .pageSize(1)

                tencentSearch.suggestion(suggestionParam, object : HttpResponseListener<BaseObject> {
                    override fun onSuccess(status: Int, obj: BaseObject?) {
                        if (obj is SuggestionResultObject) {
                            if (obj.data.isNotEmpty()) {
                                val firstResult = obj.data[0]
                                // 获取地址的经纬度
                                val lat = firstResult.latLng.latitude
                                val lng = firstResult.latLng.longitude

                                // 格式化地址信息，移除"中国"字样
                                var province = firstResult.province ?: ""
                                var city = firstResult.city ?: ""
                                var district = firstResult.district ?: ""

                                if (province.startsWith("中国")) {
                                    province = province.substring(2)
                                }

                                // 构建LocationInfo对象
                                val locationInfo = LocationInfo(
                                    province = province,
                                    city = city,
                                    district = district,
                                    address = firstResult.title ?: address,
                                    latitude = lat,
                                    longitude = lng,
                                    source = "tencent_suggestion"
                                )

                                Log.d(
                                    TAG,
                                    "地址解析成功: $address -> ${locationInfo.latitude}, ${locationInfo.longitude}"
                                )
                                continuation.resume(locationInfo)
                            } else {
                                val error = Exception("未找到地址: $address")
                                Log.e(TAG, "地址解析失败: ${error.message}")
                                continuation.resumeWithException(error)
                            }
                        } else {
                            val error = Exception("地址解析响应格式错误")
                            Log.e(TAG, "地址解析响应格式错误: $error")
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onFailure(statusCode: Int, message: String?, e: Throwable?) {
                        val error = Exception("地址解析失败: $statusCode, $message", e)
                        Log.e(TAG, "地址解析请求失败: $statusCode, $message", e)
                        continuation.resumeWithException(error)
                    }
                })
            }
        }
    }

    /**
     * 搜索与输入关键词相似的地址
     * @param keyword 搜索关键词
     * @return 地址结果列表
     */
    suspend fun searchSimilarAddresses(keyword: String): List<LocationInfo> {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val suggestionParam = SuggestionParam()
                    .keyword(keyword)
                    .region("中国")
                    .pageSize(10)

                tencentSearch.suggestion(suggestionParam, object : HttpResponseListener<BaseObject> {
                    override fun onSuccess(status: Int, obj: BaseObject?) {
                        if (obj is SuggestionResultObject) {
                            val results = mutableListOf<LocationInfo>()

                            for (suggestion in obj.data) {
                                // 格式化地址信息，移除"中国"字样
                                var province = suggestion.province ?: ""
                                var city = suggestion.city ?: ""
                                var district = suggestion.district ?: ""
                                var latitude = suggestion.latLng.latitude
                                var longitude = suggestion.latLng.longitude

                                if (province.startsWith("中国")) {
                                    province = province.substring(2)
                                }

                                if (province.isNotEmpty() && city.isNotEmpty()) {
                                    // 构建LocationInfo对象
                                    val locationInfo = LocationInfo(
                                        province = province,
                                        city = city,
                                        district = district,
                                        address = suggestion.title ?: "",
                                        latitude = latitude,
                                        longitude = longitude,
                                        source = "tencent_suggestion"
                                    )

                                    results.add(locationInfo)
                                }
                            }

                            Log.d(TAG, "搜索地址成功: $keyword, 结果数量: ${results.size}")
                            continuation.resume(results)
                        } else {
                            Log.e(TAG, "搜索地址响应格式错误")
                            continuation.resume(emptyList())
                        }
                    }

                    override fun onFailure(statusCode: Int, message: String?, e: Throwable?) {
                        Log.e(TAG, "搜索地址请求失败: $statusCode, $message", e)
                        continuation.resume(emptyList())
                    }
                })
            }
        }
    }

    /**
     * 生成地址变体用于地址搜索
     * 这是一个辅助方法，用于生成地址的不同形式，以增加匹配的可能性
     */
    private fun generateAddressVariations(location: LocationInfo): List<String> {
        val variations = mutableListOf<String>()

        // 添加不同格式的地址
        if (location.province.isNotEmpty() && location.city.isNotEmpty() && location.district.isNotEmpty()) {
            variations.add("${location.province}${location.city}${location.district}")
            variations.add("${location.city}${location.district}")

            if (location.address.isNotEmpty() && location.address != location.district) {
                variations.add("${location.city}${location.district}${location.address}")
            }
        }

        return variations
    }
} 