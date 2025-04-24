package com.aikrai.location

import android.content.Context
import android.os.Looper
import android.util.Log
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.httpresponse.HttpResponseListener
import com.tencent.lbssearch.`object`.param.SearchParam
import com.tencent.lbssearch.`object`.result.SearchResultObject
import com.tencent.map.geolocation.TencentLocation
import com.tencent.map.geolocation.TencentLocationListener
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TencentLocationService(private val context: Context) {
    private val TAG = "TencentLocationService"

    private var locationManager: TencentLocationManager? = null
    private var locationRequest: TencentLocationRequest? = null
    private var locationListener: TencentLocationListener? = null

    init {
        // 设置用户同意隐私政策
        TencentLocationManager.setUserAgreePrivacy(true)

        // 设置地图SDK隐私政策
        TencentMapInitializer.setAgreePrivacy(true)

        // 获取TencentLocationManager实例
        locationManager = TencentLocationManager.getInstance(context)

        // 创建定位请求
        locationRequest = TencentLocationRequest.create()
            .apply {
                // 设置定位级别，默认为获取名称级别的信息
                requestLevel = TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA
                // 设置优先使用GPS
                isGpsFirst = true
            }
    }

    /**
     * 请求单次定位
     */
    suspend fun requestSingleLocation(): Result<LocationInfo> {
        val deferred = CompletableDeferred<Result<LocationInfo>>()

        locationListener = object : TencentLocationListener {
            override fun onLocationChanged(location: TencentLocation, error: Int, reason: String) {
                print("")
                Log.i(TAG, "定位成功: ${location.address}")
                if (error == TencentLocation.ERROR_OK) {
                    // 定位成功
                    val locationInfo = LocationInfo(
                        province = location.province ?: "",
                        city = location.city ?: "",
                        district = location.district ?: "",
                        address = location.address ?: "",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        source = "tencent_localization"
                    )

                    deferred.complete(Result.success(locationInfo))

                    // 完成后停止定位监听
                    locationManager?.removeUpdates(this)
                } else {
                    // 定位失败
                    val errorMessage = "定位失败: 错误码 $error, 原因: $reason"
                    Log.e(TAG, errorMessage)
                    deferred.complete(Result.failure(Exception(errorMessage)))

                    // 完成后停止定位监听
                    locationManager?.removeUpdates(this)
                }
            }

            override fun onStatusUpdate(name: String, status: Int, desc: String) {
                // 监控定位组件的状态
                Log.d(TAG, "定位组件状态: $name, 状态码: $status, 描述: $desc")
            }
        }

        withContext(Dispatchers.Main) {
            try {
                // 请求单次定位更新
                val result = locationManager?.requestSingleFreshLocation(
                    locationRequest,
                    locationListener,
                    Looper.getMainLooper()
                ) ?: -1

                if (result != 0) {
                    // 定位请求发起失败
                    val errorMessage = when (result) {
                        1 -> "设备缺少使用腾讯定位SDK需要的基本条件"
                        2 -> "配置的Key不正确"
                        3 -> "自动加载so文件失败"
                        4 -> "未设置或未同意用户隐私"
                        else -> "未知错误($result)"
                    }
                    deferred.complete(Result.failure(Exception("定位请求失败: $errorMessage")))
                } else {
                    // 定位请求成功发起，等待回调
                    Log.d(TAG, "定位请求已成功发起，等待回调...")
                }
            } catch (e: Exception) {
                deferred.complete(Result.failure(e))
            }
        }

        return deferred.await()
    }

    /**
     * 获取最后一次定位信息
     */
    fun getLastLocation(): TencentLocation? {
        return locationManager?.lastKnownLocation
    }

    /**
     * 清理资源
     */
    fun release() {
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        locationListener = null
        locationRequest = null
        locationManager = null
    }

    /**
     * 获取当前位置信息
     */
    suspend fun getCurrentLocation(): LocationInfo? = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val listener = object : TencentLocationListener {
                override fun onLocationChanged(location: TencentLocation, error: Int, reason: String?) {
                    locationManager?.removeUpdates(this)

                    if (error == TencentLocation.ERROR_OK) {
                        Log.i(
                            TAG,
                            "定位成功: ${location.city} ${location.district} (${location.latitude}, ${location.longitude})"
                        )
                        val locationInfo = LocationInfo(
                            province = location.province ?: "",
                            city = location.city ?: "",
                            district = location.district ?: "",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = location.address ?: "",
                            source = "tencent_localization"
                        )
                        continuation.resume(locationInfo)
                    } else {
                        Log.e(TAG, "定位失败: $error, $reason")
                        continuation.resume(null)
                    }
                }

                override fun onStatusUpdate(name: String?, status: Int, desc: String?) {
                    Log.i(TAG, "状态更新: $name, $status, $desc")
                }
            }

            val result = locationManager?.requestLocationUpdates(locationRequest, listener) ?: -1
            if (result != 0) {
                Log.e(TAG, "请求位置更新失败: $result")
                continuation.resume(null)
            } else {
                Log.d(TAG, "位置更新请求已发起")
            }
        }
    }

    /**
     * 根据关键词搜索地点
     * @param keyword 搜索关键词
     * @return 地点列表
     */
    suspend fun searchLocation(keyword: String): List<LocationInfo> = withContext(Dispatchers.IO) {
        val resultDeferred = CompletableDeferred<List<LocationInfo>>()

        try {
            val tencentSearch = TencentSearch(context)
            val searchParam = SearchParam()
            searchParam.keyword(keyword)
            // 使用boundary方法替代region
            // searchParam.boundary("nearby(39.908491,116.397452,1000,0)")

            tencentSearch.search(searchParam, object : HttpResponseListener<SearchResultObject> {
                override fun onSuccess(response: Int, searchResult: SearchResultObject) {
                    Log.i(TAG, "搜索成功，共找到 ${searchResult.data.size} 个结果")

                    val results = searchResult.data.mapNotNull { poi ->
                        try {
                            // 解析位置信息
                            val address = poi.address ?: ""

                            // 分割地址来获取省市区信息
                            val addressComponents = parseAddress(poi.title, address)

                            // 获取POI的经纬度
                            val latitude = try {
                                // poi.location?.lat ?: 0.0
                                0.0
                            } catch (e: Exception) {
                                0.0
                            }

                            val longitude = try {
                                // poi.location?.lng ?: 0.0
                                0.0
                            } catch (e: Exception) {
                                0.0
                            }

                            LocationInfo(
                                province = addressComponents.province,
                                city = addressComponents.city,
                                district = addressComponents.district,
                                latitude = latitude,
                                longitude = longitude,
                                address = address,
                                source = "tencent_search"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "解析搜索结果出错: ${e.message}")
                            null
                        }
                    }

                    resultDeferred.complete(results)
                }

                override fun onFailure(response: Int, error: String?, e: Throwable?) {
                    Log.e(TAG, "搜索失败: $error, $e")
                    resultDeferred.complete(emptyList())
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "搜索异常: ${e.message}")
            resultDeferred.complete(emptyList())
        }

        resultDeferred.await()
    }

    /**
     * 解析地址，提取省市区信息
     */
    private fun parseAddress(title: String?, address: String): AddressComponents {
        // 默认值
        var province = ""
        var city = ""
        var district = title ?: ""

        try {
            // 尝试从地址中提取省市区
            val addressParts = address.split(",", "，", " ").filter { it.isNotEmpty() }

            if (addressParts.isNotEmpty()) {
                // 对于中国地址，通常是 "省, 市, 区/县" 的格式
                if (addressParts.size >= 3) {
                    province = addressParts[0].trim()
                    city = addressParts[1].trim()
                    // 如果title为空，使用addressParts中的区/县
                    if (district.isEmpty()) {
                        district = addressParts[2].trim()
                    }
                } else if (addressParts.size == 2) {
                    province = addressParts[0].trim()
                    city = addressParts[1].trim()
                    // 如果district为空，使用city作为district
                    if (district.isEmpty()) {
                        district = city
                    }
                } else if (addressParts.size == 1) {
                    province = addressParts[0].trim()
                    city = province
                    // 如果district为空，使用province作为district
                    if (district.isEmpty()) {
                        district = province
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析地址出错: ${e.message}")
        }

        return AddressComponents(province, city, district)
    }

    data class AddressComponents(
        val province: String,
        val city: String,
        val district: String
    )
} 