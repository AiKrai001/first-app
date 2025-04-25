package com.aikrai.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.tencent.map.geolocation.TencentLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


class LocationManager(private val context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 添加标志位，防止重复更新
    private val isUpdatingLocation = AtomicBoolean(false)

    // 是否已成功获取过位置
    private val hasLocationSucceeded = AtomicBoolean(false)

    // 添加超时控制
    private val locationTimeoutMillis = 8000L // 8秒超时

    // 腾讯定位服务
    private val tencentLocationService = TencentLocationService(context)

    private val _locationInfo = MutableStateFlow<LocationInfo?>(null)

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState = _locationState.asStateFlow()

    // 添加一个变量来存储最后一次获取到的位置信息
    private var _lastLocation: TencentLocation? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermissions()) {
            _locationState.value = LocationState.Error("未授权位置权限")
            return
        }

        try {
            // 如果已经在更新位置或已经成功获取过位置，则不再重复请求
            if (isUpdatingLocation.get()) {
                Timber.d("已经在获取位置信息，忽略此次请求")
                return
            }

            // 标记正在更新位置
            isUpdatingLocation.set(true)
            _locationState.value = LocationState.Loading

            // 开始尝试获取真实位置信息
            coroutineScope.launch {
                try {
                    // 使用腾讯定位SDK获取位置，添加超时控制
                    withTimeoutOrFallback()
                } catch (e: Exception) {
                    Timber.e(e, "位置更新过程中出错")
                    // 如果获取位置失败，则回退到默认位置
                    fallbackToDefaultLocation()
                } finally {
                    // 无论成功失败，都重置更新标志
                    isUpdatingLocation.set(false)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取位置失败")
            _locationState.value = LocationState.Error("获取位置失败: ${e.message}")
            isUpdatingLocation.set(false)
        }
    }

    private suspend fun withTimeoutOrFallback() {
        val locationJob = coroutineScope.launch {
            requestNewLocation()
        }

        // 添加超时控制
        delay(locationTimeoutMillis)

        if (!hasLocationSucceeded.get() && locationJob.isActive) {
            Timber.d("位置更新超时")
            locationJob.cancel()
            fallbackToDefaultLocation()
        }
    }

    private suspend fun requestNewLocation() {
        try {
            // 使用腾讯定位服务请求单次定位
            val locationResult = tencentLocationService.requestSingleLocation()

            locationResult.fold(
                onSuccess = { info ->
                    _lastLocation = tencentLocationService.getLastLocation()
                    _locationInfo.value = info
                    _locationState.value = LocationState.Success(info)
                    hasLocationSucceeded.set(true)
                },
                onFailure = { e ->
                    Timber.e(e, "获取位置信息失败")
                    if (!hasLocationSucceeded.get()) {
                        fallbackToDefaultLocation()
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "请求位置更新失败")
            fallbackToDefaultLocation()
        }
    }

    fun stopLocationUpdates() {
        tencentLocationService.release()
        coroutineScope.cancel()
        isUpdatingLocation.set(false)
        hasLocationSucceeded.set(false)
    }

    private fun fallbackToDefaultLocation() {
        // 改为使用上次缓存的位置，如果有的话
        val lastLoc = _lastLocation ?: tencentLocationService.getLastLocation()

        if (lastLoc != null) {
            // 使用上次获取的位置信息
            val locationInfo = LocationInfo.fromTencentLocation(lastLoc)
            _locationInfo.value = locationInfo
            _locationState.value = LocationState.Success(locationInfo)
            Timber.d("使用上次缓存的位置作为默认位置: ${locationInfo.district}")
        } else {
            // 如果没有上次的位置信息，则通知错误，让ViewModel处理
            _locationState.value = LocationState.Error("无法获取位置信息")
            Timber.e("无法获取位置信息且没有缓存位置")
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    sealed class LocationState {
        object Loading : LocationState()
        data class Success(val locationInfo: LocationInfo) : LocationState()
        data class Error(val message: String) : LocationState()
    }
}

