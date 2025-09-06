package com.wkq.locationkit

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wkq.address.GeoDbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * @Author: wkq
 * @Date: 2025/09/02
 * @Desc: 生命周期安全的地理位置解析工具（Geocoder 优先，数据库兜底）
 *
 * 功能：
 * 1. 单地址解析：根据经纬度获取详细地址信息。
 * 2. 附近位置列表解析：根据经纬度获取附近的地址信息列表。
 * 3. 支持 Kotlin 协程和 Java 回调方式。
 * 4. 生命周期安全：当传入 LifecycleOwner 时，Activity/Fragment 销毁时会自动取消后台任务。
 *
 * 使用说明：
 * - 推荐在协程中使用 suspend 方法：getAddress / getNearbyAddresses
 * - Java 或不在协程环境中使用异步方法：getAddressAsync / getNearbyAddressesAsync
 * - 支持本地数据库兜底，确保在无网络或 Geocoder 不可用时仍能获取地址信息。
 */
object LocationResolverHelper {

    /**
     * 解析结果数据结构
     * @param address 完整详细地址，可能为空
     * @param city 城市名称，例如 "北京市"
     * @param province 省/州，例如 "北京市" 或 "广东省"
     * @param country 国家名称，例如 "中国"
     * @param latitude 纬度
     * @param longitude 经度
     */
    data class LocationInfo(
        val address: String?,
        val city: String?,
        val province: String?,
        val country: String?,
        val latitude: Double,
        val longitude: Double
    )

    /**
     * Java 回调接口：单地址解析
     */
    interface AddressCallback {
        /**
         * @param result 解析后的 LocationInfo，如果解析失败则为 null
         */
        fun onAddressResult(result: LocationInfo?)
    }

    /**
     * Java 回调接口：附近位置列表解析
     */
    interface NearbyCallback {
        /**
         * @param results 解析后的地址列表，可能为空列表
         */
        fun onNearbyResult(results: List<LocationInfo>)
    }

    // ------------------- 单地址解析 -------------------

    /**
     * Kotlin 挂起函数方式获取单地址信息
     *
     * @param context 上下文对象
     * @param latitude 纬度（-90 ~ 90）
     * @param longitude 经度（-180 ~ 180）
     * @param maxResults 最大返回条数，默认 1
     * @param locale 本地化设置，默认系统语言
     * @return LocationInfo 或 null（解析失败或坐标不合法）
     */
    suspend fun getAddress(
        context: Context,
        latitude: Double,
        longitude: Double,
        maxResults: Int = 1,
        locale: Locale = Locale.getDefault()
    ): LocationInfo? = withContext(Dispatchers.IO) {
        getAddressInternal(context, latitude, longitude, maxResults, locale)
    }

    /**
     * Java / 生命周期安全方式获取单地址信息
     *
     * @param lifecycleOwner 可选，如果传入，任务在对应 Lifecycle 销毁时会自动取消
     * @param callback 回调结果，会在主线程返回
     */
    @JvmStatic
    fun getAddressAsync(
        context: Context,
        latitude: Double,
        longitude: Double,
        maxResults: Int = 1,
        locale: Locale = Locale.getDefault(),
        lifecycleOwner: LifecycleOwner? = null,
        callback: AddressCallback
    ) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            // 坐标非法时立即返回 null
            callback.onAddressResult(null)
            return
        }

        val scope = lifecycleOwner?.lifecycleScope ?: CoroutineScope(Dispatchers.IO)
        val job = scope.launch(Dispatchers.IO) {
            val result = getAddressInternal(context, latitude, longitude, maxResults, locale)
            withContext(Dispatchers.Main) {
                if (lifecycleOwner == null ||
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                ) {
                    callback.onAddressResult(result)
                }
            }
        }

        // 生命周期安全：Activity/Fragment 销毁时取消任务
        lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                job.cancel()
            }
        })
    }

    /**
     * 内部单地址解析实现
     * 优先使用系统 Geocoder，不可用时使用 GeoDbHelper 数据库兜底
     */
    private fun getAddressInternal(
        context: Context,
        latitude: Double,
        longitude: Double,
        maxResults: Int,
        locale: Locale
    ): LocationInfo? {
        // 系统 Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, locale)
                val addresses: List<Address>? =
                    geocoder.getFromLocation(latitude, longitude, maxResults)
                val address = addresses?.firstOrNull()
                formatLocationInfo(address)
                    ?.let { return it.copy(latitude = latitude, longitude = longitude) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 数据库兜底
        return try {
            val geoDb = GeoDbHelper(context)
            val nearest = geoDb.getNearestPlaceResult(latitude, longitude)
            nearest?.let {
                LocationInfo(
                    address = listOfNotNull(it.name, it.admin2, it.admin1).joinToString(" "),
                    city = it.admin2,
                    province = it.admin1,
                    country = "中国",
                    latitude = it.lat,
                    longitude = it.lon
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 格式化系统 Geocoder 返回的 Address 为 LocationInfo
     */
    private fun formatLocationInfo(address: Address?): LocationInfo? {
        if (address == null) return null

        val addressLines = mutableListOf<String>()
        for (i in 0..address.maxAddressLineIndex) {
            val line = address.getAddressLine(i)?.trim()
            if (!line.isNullOrBlank()) {
                addressLines.add(line)
            }
        }

        val fullAddress = if (addressLines.isNotEmpty()) {
            addressLines.joinToString(" ")
        } else {
            listOfNotNull(address.locality, address.adminArea, address.countryName)
                .joinToString(" ")
        }

        return LocationInfo(
            address = fullAddress.takeIf { it.isNotBlank() },
            city = address.locality,
            province = address.adminArea,
            country = address.countryName,
            latitude = address.latitude,
            longitude = address.longitude
        )
    }

    // ------------------- 附近位置列表解析 -------------------

    /**
     * Kotlin 挂起函数方式获取附近位置列表
     *
     * @param radiusKm 查询半径，单位公里
     * @param maxResults 最大返回条数
     * @return List<LocationInfo>，可能为空列表
     */
    suspend fun getNearbyAddresses(
        context: Context,
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 1.0,
        maxResults: Int = 10
    ): List<LocationInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocationInfo>()

        // 系统 Geocoder 查询
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocation(latitude, longitude, maxResults)
                addresses?.forEach { addr ->
                    formatLocationInfo(addr)?.let { results.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 数据库兜底
        if (results.isEmpty()) {
            try {
                val geoDb = GeoDbHelper(context)
                val nearby = geoDb.getNearbyPlacesResult(latitude, longitude, radiusKm, maxResults)
                nearby.forEach { geo ->
                    results.add(
                        LocationInfo(
                            address = listOfNotNull(geo.name, geo.admin2, geo.admin1).joinToString(" "),
                            city = geo.admin2,
                            province = geo.admin1,
                            country = "中国",
                            latitude = geo.lat,
                            longitude = geo.lon
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        results
    }

    /**
     * Java / 生命周期安全方式获取附近位置列表
     */
    @JvmStatic
    fun getNearbyAddressesAsync(
        context: Context,
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 1.0,
        maxResults: Int = 10,
        lifecycleOwner: LifecycleOwner? = null,
        callback: NearbyCallback
    ) {
        val scope = lifecycleOwner?.lifecycleScope ?: CoroutineScope(Dispatchers.IO)
        val job = scope.launch(Dispatchers.IO) {
            val results = getNearbyAddresses(context, latitude, longitude, radiusKm, maxResults)
            withContext(Dispatchers.Main) {
                if (lifecycleOwner == null ||
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                ) {
                    callback.onNearbyResult(results)
                }
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                job.cancel()
            }
        })
    }
}
