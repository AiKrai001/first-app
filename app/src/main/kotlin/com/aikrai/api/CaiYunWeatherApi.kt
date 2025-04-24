package com.aikrai.api

import androidx.annotation.Keep
import com.aikrai.model.CaiYunWeatherConsolidatedData
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 彩云天气API服务接口
 *
 * 单独放在一个文件中以避免混淆问题
 */
@Keep
interface CaiYunWeatherApi {
    /**
     * 获取天气综合数据
     *
     * 方法签名经过简化处理，以避免泛型混淆问题
     */
    @Keep
    @GET("v2.6/{token}/{location}/weather")
    suspend fun getWeatherData(
        @Path("token") token: String,
        @Path("location") location: String,
        @Query("alert") alert: Boolean?,
        @Query("dailysteps") dailysteps: Int?,
        @Query("hourlysteps") hourlysteps: Int?
    ): CaiYunWeatherConsolidatedData
} 