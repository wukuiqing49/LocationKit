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
 * 支持：
 * 1. 单地址查询
 * 2. 附近位置列表查询
 * 3. Kotlin 协程和 Java 回调
 */
object LocationGeocoderHelper {

    /**
     * 返回的数据结构
     */
    data class LocationInfo(
        val address: String?,  // 详细地址
        val city: String?,     // 城市
        val province: String?, // 省/州
        val country: String?,  // 国家
        val latitude: Double,
        val longitude: Double
    )

    /**
     * Java 回调接口
     */
    interface AddressCallback {
        fun onAddressResult(result: LocationInfo?)
    }

    interface NearbyCallback {
        fun onNearbyResult(results: List<LocationInfo>)
    }

    // ------------------- 单地址查询 -------------------

    /**
     * Kotlin 挂起函数方式
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
     * Java / 生命周期安全方式
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

        lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                job.cancel()
            }
        })
    }

    private fun getAddressInternal(
        context: Context,
        latitude: Double,
        longitude: Double,
        maxResults: Int,
        locale: Locale
    ): LocationInfo? {
        // 1. 系统 Geocoder
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

        // 2. GeoDbHelper 数据库兜底
        return try {
            val geoDb = GeoDbHelper(context)
            val nearest = geoDb.getNearestPlaceResult(latitude, longitude)
            nearest?.let {
                LocationInfo(
                    address = listOfNotNull(it.name, it.admin2, it.admin1).joinToString(" "),
                    city = it.admin2,
                    province = it.admin1,
                    country = "中国", // 国内 GeoNames 数据库默认中国
                    latitude = it.lat,
                    longitude = it.lon
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatLocationInfo(address: Address?): LocationInfo? {
        if (address == null) return null

        // 1. 遍历所有地址线，拼接完整地址（解决多地址线分散问题）
        val addressLines = mutableListOf<String>()
        for (i in 0..address.maxAddressLineIndex) {
            val line = address.getAddressLine(i)?.trim()
            if (!line.isNullOrBlank()) {
                addressLines.add(line)
            }
        }
        // 2. 若地址线为空，用“城市+省份+国家”兜底
        val fullAddress = if (addressLines.isNotEmpty()) {
            addressLines.joinToString(" ") // 地址线间用空格分隔（可根据需求改“-”“，”）
        } else {
            listOfNotNull(address.locality, address.adminArea, address.countryName)
                .joinToString(" ")
        }

        return LocationInfo(
            address = fullAddress.takeIf { it.isNotBlank() },
            city = address.locality, // 城市（如“北京市”“上海市”）
            province = address.adminArea, // 省份/州（如“北京市”“广东省”）
            country = address.countryName, // 国家（如“中国”“United States”）
            latitude = address.latitude,
            longitude = address.longitude
        )
    }

    // ------------------- 附近位置列表 -------------------

    suspend fun getNearbyAddresses(
        context: Context,
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 1.0,
        maxResults: Int = 10
    ): List<LocationInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocationInfo>()

        // Geocoder 查询附近 POI
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
                            address = listOfNotNull(geo.name, geo.admin2, geo.admin1).joinToString(
                                " "
                            ),
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