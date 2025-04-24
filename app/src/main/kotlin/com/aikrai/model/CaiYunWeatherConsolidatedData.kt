package com.aikrai.model

import androidx.annotation.Keep

/**
 * 彩云天气 API 响应数据模型
 *
 * @property status 返回状态
 * @property api_version API 版本
 * @property api_status API 状态
 * @property lang 语言
 * @property unit 单位（例如：metric）
 * @property tzshift 时区偏移（单位：秒）
 * @property timezone 时区（例如：Asia/Shanghai）
 * @property server_time 服务器时间（Unix时间戳）
 * @property location 地理位置（经度和纬度）
 * @property result 天气结果数据
 */
@Keep
data class CaiYunWeatherConsolidatedData(
    val status: String,
    val api_version: String,
    val api_status: String,
    val lang: String,
    val unit: String,
    val tzshift: Int,
    val timezone: String,
    val server_time: Long,
    val location: List<Double>,
    val result: CaiYunResult
)

/**
 * 天气结果数据
 *
 * @property alert 预警信息（可选）
 * @property realtime 实时天气数据（可选）
 * @property minutely 分钟级降水数据（可选）
 * @property hourly 小时级降水数据（可选）
 * @property daily 天级降水数据（可选）
 * @property primary 主要数据标识
 * @property forecast_keypoint 天气预报关键点
 */
@Keep
data class CaiYunResult(
    val alert: CaiYunAlert?,
    val realtime: CaiYunRealtime?,
    val minutely: CaiYunMinutely?,
    val hourly: CaiYunHourly?,
    val daily: CaiYunDaily?,
    val primary: Int,
    val forecast_keypoint: String
)

/**
 * 预警信息
 *
 * @property status 预警信息的状态
 * @property content 预警内容列表
 * @property adcodes 区域代码列表
 */
@Keep
data class CaiYunAlert(
    val status: String,
    val content: List<AlertContent>,
    val adcodes: List<Adcode>
) {
    /**
     * 预警内容
     *
     * @property province 省份
     * @property status 预警状态
     * @property code 预警代码
     * @property description 预警描述
     * @property regionId 地区 ID
     * @property county 县区
     * @property pubtimestamp 发布时间戳
     * @property latlon 经纬度
     * @property city 城市
     * @property alertId 预警 ID
     * @property title 预警标题
     * @property adcode 区域代码
     * @property source 预警信息来源
     * @property location 地点
     * @property request_status 请求状态
     */
    @Keep
    data class AlertContent(
        val province: String,
        val status: String,
        val code: String,
        val description: String,
        val regionId: String,
        val county: String,
        val pubtimestamp: Long,
        val latlon: List<Double>,
        val city: String,
        val alertId: String,
        val title: String,
        val adcode: String,
        val source: String,
        val location: String,
        val request_status: String
    )

    /**
     * 区域代码
     *
     * @property adcode 区域代码
     * @property name 区域名称
     */
    @Keep
    data class Adcode(
        val adcode: Int,
        val name: String
    )
}

/**
 * 实时天气数据
 *
 * @property status 状态
 * @property temperature 地表 2 米气温
 * @property humidity 地表 2 米湿度相对湿度(%)
 * @property cloudrate 总云量(0.0-1.0)
 * @property skycon 天气现象
 * @property visibility 地表水平能见度
 * @property dswrf 向下短波辐射通量(W/M2)
 * @property wind 风信息
 * @property pressure 地面气压
 * @property apparent_temperature 体感温度
 * @property precipitation 降水信息
 * @property air_quality 空气质量信息
 * @property life_index 生活指数信息
 */
@Keep
data class CaiYunRealtime(
    val status: String,
    val temperature: Double,
    val humidity: Double,
    val cloudrate: Double,
    val skycon: String,
    val visibility: Double,
    val dswrf: Double,
    val wind: Wind,
    val pressure: Double,
    val apparent_temperature: Double,
    val precipitation: Precipitation,
    val air_quality: AirQuality,
    val life_index: LifeIndex
) {
    /**
     * 风信息
     *
     * @property speed 地表 10 米风速
     * @property direction 地表 10 米风向
     */
    @Keep
    data class Wind(
        val speed: Double,
        val direction: Double
    )

    /**
     * 降水信息
     *
     * @property local 本地降水信息
     * @property nearest 最近降水信息
     */
    @Keep
    data class Precipitation(
        val local: LocalPrecipitation,
        val nearest: NearestPrecipitation
    )

    /**
     * 本地降水信息
     *
     * @property status 状态
     * @property datasource 数据来源
     * @property intensity 本地降水强度
     */
    @Keep
    data class LocalPrecipitation(
        val status: String,
        val datasource: String,
        val intensity: Double
    )

    /**
     * 最近降水信息
     *
     * @property status 状态
     * @property distance 最近降水带与本地的距离
     * @property intensity 最近降水处的降水强度
     */
    @Keep
    data class NearestPrecipitation(
        val status: String,
        val distance: Double,
        val intensity: Double
    )

    /**
     * 空气质量信息
     *
     * @property pm25 PM25 浓度(μg/m3)
     * @property pm10 PM10 浓度(μg/m3)
     * @property o3 臭氧浓度(μg/m3)
     * @property so2 二氧化硫浓度(μg/m3)
     * @property no2 二氧化氮浓度(μg/m3)
     * @property co 一氧化碳浓度(mg/m3)
     * @property aqi 空气质量指数
     * @property description 描述信息
     */
    @Keep
    data class AirQuality(
        val pm25: Int,
        val pm10: Int,
        val o3: Int,
        val so2: Int,
        val no2: Int,
        val co: Double,
        val aqi: AQI,
        val description: Description
    )

    /**
     * 空气质量指数
     *
     * @property chn 国标 AQI
     * @property usa 美国 AQI
     */
    @Keep
    data class AQI(
        val chn: Int,
        val usa: Int
    )

    /**
     * 描述信息
     *
     * @property chn 中文描述
     * @property usa 英文描述
     */
    @Keep
    data class Description(
        val chn: String,
        val usa: String
    )

    /**
     * 生活指数信息
     *
     * @property ultraviolet 紫外线指数
     * @property comfort 舒适度指数
     */
    @Keep
    data class LifeIndex(
        val ultraviolet: Ultraviolet,
        val comfort: Comfort
    ) {
        /**
         * 紫外线指数
         *
         * @property index 紫外线指数
         * @property desc 描述
         */
        @Keep
        data class Ultraviolet(
            val index: Double,
            val desc: String
        )

        /**
         * 舒适度指数
         *
         * @property index 舒适度指数
         * @property desc 描述
         */
        @Keep
        data class Comfort(
            val index: Double,
            val desc: String
        )
    }
}

/**
 * 分钟级降水数据字段
 *
 * @property status 分钟级预报状态
 * @property datasource 数据源
 * @property precipitation_2h 未来2小时每分钟的雷达降水强度
 * @property precipitation 未来1小时每分钟的雷达降水强度
 * @property probability 未来两小时每半小时的降水概率
 * @property description 预报描述
 */
@Keep
data class CaiYunMinutely(
    val status: String,
    val datasource: String,
    val precipitation_2h: List<Int>,
    val precipitation: List<Int>,
    val probability: List<Int>,
    val description: String
)

/**
 * 小时级降水数据字段
 *
 * @property status 小时级别预报状态
 * @property description 小时级别预报的天气描述
 * @property precipitation 降水数据
 * @property temperature 地表2米气温
 * @property apparentTemperature 体感温度
 * @property wind 地表10米风向和风速
 * @property humidity 地表2米相对湿度
 * @property cloudrate 云量
 * @property skycon 天气现象
 * @property pressure 地面气压
 * @property visibility 地表水平能见度
 * @property dswrf 向下短波辐射通量
 * @property airQuality 空气质量
 */
@Keep
data class CaiYunHourly(
    val status: String,
    val description: String,
    val precipitation: List<Precipitation>,
    val temperature: List<Temperature>,
    val apparent_temperature: List<ApparentTemperature>,
    val wind: List<Wind>,
    val humidity: List<Humidity>,
    val cloudrate: List<Cloudrate>,
    val skycon: List<Skycon>,
    val pressure: List<Pressure>,
    val visibility: List<Visibility>,
    val dswrf: List<Dswrf>,
    val air_quality: AirQuality
) {
    /**
     * 小时级降水数据
     *
     * @property datetime 时间
     * @property value 降水量
     * @property probability 降水概率
     */
    @Keep
    data class Precipitation(
        val datetime: String,
        val value: Double,
        val probability: Int
    )

    /**
     * 温度数据
     *
     * @property datetime 时间
     * @property value 气温
     */
    @Keep
    data class Temperature(
        val datetime: String,
        val value: Double
    )

    /**
     * 体感温度数据
     *
     * @property datetime 时间
     * @property value 体感温度
     */
    @Keep
    data class ApparentTemperature(
        val datetime: String,
        val value: Double
    )

    /**
     * 风数据
     *
     * @property datetime 时间
     * @property speed 风速
     * @property direction 风向
     */
    @Keep
    data class Wind(
        val datetime: String,
        val speed: Double,
        val direction: Double
    )

    /**
     * 湿度数据
     *
     * @property datetime 时间
     * @property value 相对湿度
     */
    @Keep
    data class Humidity(
        val datetime: String,
        val value: Double
    )

    /**
     * 云量数据
     *
     * @property datetime 时间
     * @property value 云量
     */
    @Keep
    data class Cloudrate(
        val datetime: String,
        val value: Double
    )

    /**
     * 天气现象数据
     *
     * @property datetime 时间
     * @property value 天气现象
     */
    @Keep
    data class Skycon(
        val datetime: String,
        val value: String
    )

    /**
     * 气压数据
     *
     * @property datetime 时间
     * @property value 气压
     */
    @Keep
    data class Pressure(
        val datetime: String,
        val value: Double
    )

    /**
     * 能见度数据
     *
     * @property datetime 时间
     * @property value 能见度
     */
    @Keep
    data class Visibility(
        val datetime: String,
        val value: Double
    )

    /**
     * 向下短波辐射通量数据
     *
     * @property datetime 时间
     * @property value 向下短波辐射通量
     */
    @Keep
    data class Dswrf(
        val datetime: String,
        val value: Double
    )

    /**
     * 空气质量数据
     *
     * @property aqi AQI数据
     * @property pm25 PM2.5浓度
     */
    @Keep
    data class AirQuality(
        val aqi: List<Aqi>,
        val pm25: List<Pm25>
    ) {
        /**
         * AQI数据
         *
         * @property datetime 时间
         * @property value AQI值
         */
        @Keep
        data class Aqi(
            val datetime: String,
            val value: AqiValue
        ) {
            /**
             * AQI值
             *
             * @property chn 国内AQI
             * @property usa 美国AQI
             */
            @Keep
            data class AqiValue(
                val chn: Int,
                val usa: Int
            )
        }

        /**
         * PM2.5浓度数据
         *
         * @property datetime 时间
         * @property value PM2.5浓度
         */
        @Keep
        data class Pm25(
            val datetime: String,
            val value: Int
        )
    }
}

/**
 * 天级降水数据字段
 *
 * @property status 天级预报状态
 * @property astro 日出日落数据
 * @property precipitation 降水数据
 * @property temperature 温度数据
 * @property wind 风数据
 * @property humidity 湿度数据
 * @property cloudrate 云量数据
 * @property pressure 气压数据
 * @property visibility 能见度数据
 * @property dswrf 向下短波辐射通量数据
 * @property air_quality 空气质量数据
 * @property skycon 天气现象数据
 * @property life_index 生活指数数据
 */
@Keep
data class CaiYunDaily(
    val status: String,
    val astro: List<Astro>,
    val precipitation_08h_20h: List<Precipitation>,
    val precipitation_20h_32h: List<Precipitation>,
    val precipitation: List<Precipitation>,
    val temperature: List<Temperature>,
    val temperature_08h_20h: List<Temperature>,
    val temperature_20h_32h: List<Temperature>,
    val wind: List<Wind>,
    val wind_08h_20h: List<Wind>,
    val wind_20h_32h: List<Wind>,
    val humidity: List<Humidity>,
    val cloudrate: List<Cloudrate>,
    val pressure: List<Pressure>,
    val visibility: List<Visibility>,
    val dswrf: List<Dswrf>,
    val air_quality: AirQuality,
    val skycon: List<Skycon>,
    val skycon_08h_20h: List<Skycon>,
    val skycon_20h_32h: List<Skycon>,
    val life_index: LifeIndex
) {
    /**
     * 日出日落时间
     *
     * @property date 日期
     * @property sunrise 日出时间
     * @property sunset 日落时间
     */
    @Keep
    data class Astro(
        val date: String,
        val sunrise: Sunrise,
        val sunset: Sunset
    ) {
        /**
         * 日出时间
         *
         * @property time 日出时间
         */
        @Keep
        data class Sunrise(
            val time: String
        )

        /**
         * 日落时间
         *
         * @property time 日落时间
         */
        @Keep
        data class Sunset(
            val time: String
        )
    }

    /**
     * 日级降水数据
     *
     * @property date 日期
     * @property max 最大降水量
     * @property min 最小降水量
     * @property avg 平均降水量
     * @property probability 降水概率
     */
    @Keep
    data class Precipitation(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double,
        val probability: Int
    )

    /**
     * 日级温度数据
     *
     * @property date 日期
     * @property max 最高气温
     * @property min 最低气温
     * @property avg 平均气温
     */
    @Keep
    data class Temperature(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double
    )

    /**
     * 日级风数据
     *
     * @property date 日期
     * @property max 最大风速
     * @property min 最小风速
     * @property avg 平均风速
     */
    @Keep
    data class Wind(
        val date: String,
        val max: WindDetails,
        val min: WindDetails,
        val avg: WindDetails
    ) {
        /**
         * 风详情
         *
         * @property speed 风速
         * @property direction 风向
         */
        @Keep
        data class WindDetails(
            val speed: Double,
            val direction: Double
        )
    }

    /**
     * 日级湿度数据
     *
     * @property date 日期
     * @property max 最大相对湿度
     * @property min 最小相对湿度
     * @property avg 平均相对湿度
     */
    @Keep
    data class Humidity(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double
    )

    /**
     * 日级云量数据
     *
     * @property date 日期
     * @property max 最大云量
     * @property min 最小云量
     * @property avg 平均云量
     */
    @Keep
    data class Cloudrate(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double
    )

    /**
     * 日级气压数据
     *
     * @property date 日期
     * @property max 最大气压
     * @property min 最小气压
     * @property avg 平均气压
     */
    @Keep
    data class Pressure(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double
    )

    /**
     * 日级能见度数据
     *
     * @property date 日期
     * @property max 最大能见度
     * @property min 最小能见度
     * @property avg 平均能见度
     */
    @Keep
    data class Visibility(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double
    )

    /**
     * 日级辐射通量数据
     *
     * @property date 日期
     * @property max 最大辐射通量
     * @property min 最小辐射通量
     * @property avg 平均辐射通量
     */
    @Keep
    data class Dswrf(
        val date: String,
        val max: Double,
        val min: Double,
        val avg: Double
    )

    /**
     * 日级空气质量数据
     *
     * @property aqi AQI数据
     * @property pm25 PM2.5浓度
     */
    @Keep
    data class AirQuality(
        val aqi: List<Aqi>,
        val pm25: List<Pm25>
    )

    /**
     * 日级AQI数据
     *
     * @property date 日期
     * @property max AQI最大值
     * @property avg AQI平均值
     * @property min AQI最小值
     */
    @Keep
    data class Aqi(
        val date: String,
        val max: AqiValue,
        val avg: AqiValue,
        val min: AqiValue
    ) {
        /**
         * AQI值
         *
         * @property chn 中国国标AQI
         * @property usa 美国国标AQI
         */
        @Keep
        data class AqiValue(
            val chn: Int,
            val usa: Int
        )
    }

    /**
     * 日级PM2.5浓度数据
     *
     * @property date 日期
     * @property max PM2.5浓度最大值
     * @property avg PM2.5浓度平均值
     * @property min PM2.5浓度最小值
     */
    @Keep
    data class Pm25(
        val date: String,
        val max: Int,
        val avg: Int,
        val min: Int
    )

    /**
     * 天气现象数据
     *
     * @property date 日期
     * @property value 天气现象
     */
    @Keep
    data class Skycon(
        val date: String,
        val value: String
    )

    /**
     * 生活指数
     *
     * @property ultraviolet 紫外线指数
     * @property carWashing 洗车指数
     * @property dressing 穿衣指数
     * @property comfort 舒适度指数
     * @property coldRisk 感冒指数
     */
    @Keep
    data class LifeIndex(
        val ultraviolet: List<LifeIndexDetail>,
        val carWashing: List<LifeIndexDetail>,
        val dressing: List<LifeIndexDetail>,
        val comfort: List<LifeIndexDetail>,
        val coldRisk: List<LifeIndexDetail>
    ) {
        /**
         * 生活指数详情
         *
         * @property date 日期
         * @property index 指数值
         * @property desc 指数描述
         */
        @Keep
        data class LifeIndexDetail(
            val date: String,
            val index: String,
            val desc: String
        )
    }
}