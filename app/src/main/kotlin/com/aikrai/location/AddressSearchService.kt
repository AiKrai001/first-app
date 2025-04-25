package com.aikrai.location

import android.content.Context
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.httpresponse.BaseObject
import com.tencent.lbssearch.httpresponse.HttpResponseListener
import com.tencent.lbssearch.`object`.param.SuggestionParam
import com.tencent.lbssearch.`object`.result.SuggestionResultObject
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * 地址搜索服务类，使用腾讯地图API进行地址解析
 */
class AddressSearchService(context: Context) {
    private val tencentSearch: TencentSearch

    init {
        // 设置地图SDK隐私政策
        TencentMapInitializer.setAgreePrivacy(true)

        tencentSearch = TencentSearch(context)
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
                                var province = suggestion.province ?: ""
                                var city = suggestion.city ?: ""
                                var district = suggestion.district ?: ""
                                var latitude = suggestion.latLng.latitude
                                var longitude = suggestion.latLng.longitude

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

                            Timber.d("搜索地址成功: $keyword, 结果数量: ${results.size}")
                            continuation.resume(results)
                        } else {
                            Timber.e("搜索地址响应格式错误")
                            continuation.resume(emptyList())
                        }
                    }

                    override fun onFailure(statusCode: Int, message: String?, e: Throwable?) {
                        Timber.e(e, "搜索地址请求失败: $statusCode, $message")
                        continuation.resume(emptyList())
                    }
                })
            }
        }
    }
}