package com.aikrai.location

import android.content.Context
import android.os.Looper
import com.tencent.map.geolocation.TencentLocation
import com.tencent.map.geolocation.TencentLocationListener
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class TencentLocationService(context: Context) {
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
                Timber.i("定位成功: ${location.address}")
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
                    Timber.e(errorMessage)
                    deferred.complete(Result.failure(Exception(errorMessage)))

                    // 完成后停止定位监听
                    locationManager?.removeUpdates(this)
                }
            }

            override fun onStatusUpdate(name: String, status: Int, desc: String) {
                // 监控定位组件的状态
                Timber.d("定位组件状态: $name, 状态码: $status, 描述: $desc")
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
                    Timber.d("定位请求已成功发起，等待回调...")
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

}