package com.wkq.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/2 13:52
 *
 *@Desc:  定位获取的工具类
 */
object LocationKit : LocationListener {

    // -------------------- 全局字段 --------------------
    private var locationManager: LocationManager? = null                 // 系统 LocationManager
    private lateinit var appContext: Context                              // 保存 ApplicationContext，避免内存泄漏
    private var currentConfig: LocationConfig = LocationConfig()          // 当前定位配置
    private val handler = Handler(Looper.getMainLooper())                 // 主线程 Handler，用于超时回调
    private var timeoutRunnable: Runnable? = null                         // 定位超时处理
    var callback: ((LocationResult) -> Unit)? = null                     // 外部回调定位结果

    private val lock = Any()                                              // 同步锁，防止并发回调冲突
    private var lastLocation: Location? = null                             // 上一次有效位置，用于过滤微抖动/大跳变
    private var singleLocationReceived = false                             // SINGLE 模式是否已经收到一次有效回调

    // -------------------- 初始化 --------------------
    /**
     * 初始化 LocationKit
     * @param context ApplicationContext 或 Activity
     * @param config 可选配置对象
     *
     * 注意：必须调用 init() 后才能 startLocation()
     */
    fun init(context: Context, config: LocationConfig = LocationConfig()) {
        appContext = context.applicationContext          // 统一使用 ApplicationContext
        locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        currentConfig = config
    }

    // -------------------- 开始定位 --------------------
    /**
     * 开始定位
     * 回调 listener 返回 LocationResult
     */
    fun startLocation(listener: (LocationResult) -> Unit) {
        if (locationManager == null) {
            listener(LocationResult(false, createDefaultLocation(), "LocationManager 初始化失败"))
            return
        }
        if (!hasPermission()) {
            listener(LocationResult(false, createDefaultLocation(), "缺少定位权限"))
            return
        }

        // 2. 判断是否有可用 Provider
        val providers = locationManager?.getProviders(true).orEmpty()
        if (providers.isEmpty()) {
            listener(LocationResult(false, createDefaultLocation(), "未启用定位服务，使用默认值"))
            return
        }

        callback = listener
        singleLocationReceived = false
        lastLocation = null

        val type = currentConfig.getLocationType()
        val minTimeMs = currentConfig.getMinTimeMs()
        val minDistanceM = currentConfig.getMinDistanceM()

        // 根据定位类型请求不同 Provider
        when (type) {
            LocationType.FAST -> {
                val last = getValidLastKnownLocation() ?: createDefaultLocation()
                processLocation(last, force = true)   // 立即回调最后位置
                requestAllProviders(minTimeMs, minDistanceM)
            }

            LocationType.FUSION, LocationType.SINGLE -> requestAllProviders(minTimeMs, minDistanceM)
            LocationType.LOCATION_NET -> requestProvider(
                LocationManager.NETWORK_PROVIDER, minTimeMs, minDistanceM
            )

            LocationType.LOCATION_GPS -> requestProvider(
                LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM
            )

            else -> requestAllProviders(minTimeMs, minDistanceM)
        }

        // 设置超时回调
        timeoutRunnable = Runnable {
            synchronized(lock) {
                if (!singleLocationReceived) {
                    val cached = getValidLastKnownLocation()
                    if (cached != null) {
                        processLocation(cached, force = true)
                    } else {
                        // 缓存无效，返回默认值
                        processLocation(createDefaultLocation(), force = true)

                        // ✅ 触发重试
                        retryIfNoLocation(minTimeMs, minDistanceM)
                    }
                }
            }
        }
        handler.postDelayed(timeoutRunnable!!, currentConfig.getTimeout())
    }

    // -------------------- 停止定位 --------------------
    /** 停止定位并移除回调 */
    fun stopLocation() {
        locationManager?.removeUpdates(this)
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    // -------------------- Provider 请求 --------------------
    /** 请求指定 Provider */
    @SuppressLint("MissingPermission")
    private fun requestProvider(provider: String, time: Long, distance: Float) {
        locationManager?.let {
            if (it.isProviderEnabled(provider)) {
                it.requestLocationUpdates(provider, time, distance, this)
            }
        }
    }

    /** 请求所有可用 Provider（GPS/网络/PASSIVE） */
    private fun requestAllProviders(minTimeMs: Long, minDistanceM: Float) {
        // 获取可用 Provider
        val providers = locationManager?.getProviders(true).orEmpty()
        val finalProviders = if (providers.isEmpty()) {
            // ROM 返回空时，强制使用 NETWORK + PASSIVE
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        } else providers

        finalProviders.forEach { provider -> requestProvider(provider, minTimeMs, minDistanceM) }
    }

    @SuppressLint("MissingPermission")
    private fun getValidLastKnownLocation(): Location? {
        if (!hasPermission()) return null
        val now = System.currentTimeMillis()
        var best: Location? = null
        locationManager?.getProviders(true)?.forEach { provider ->
            val loc = locationManager?.getLastKnownLocation(provider) ?: return@forEach
            if (now - loc.time > 60_000) return@forEach
            if (best == null || loc.accuracy < best.accuracy) best = loc
        }
        return best
    }

    private var retryCount = 0
    private val maxRetry = 2

    private fun retryIfNoLocation(minTimeMs: Long, minDistanceM: Float) {
        if (retryCount >= maxRetry) return
        retryCount++
        handler.postDelayed({
            if (!singleLocationReceived) {
                requestAllProviders(minTimeMs, minDistanceM)
            }
        }, 2000)
    }

    // -------------------- 权限检查 --------------------
    /** 检查定位权限 */
    private fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // -------------------- 系统缓存位置 --------------------
    /** 获取系统缓存的最后一次定位 */
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (!hasPermission()) return null
        locationManager?.let {
            val providers = it.getProviders(true)
            var best: Location? = null
            for (p in providers) {
                val l = it.getLastKnownLocation(p) ?: continue
                if (best == null || l.accuracy < best.accuracy) best = l
            }
            return best
        }
        return null
    }

    /** 默认位置（定位失败时使用） */
    private fun createDefaultLocation(): Location {
        val loc = Location("default")
        loc.latitude = currentConfig.getDefaultLatitude()
        loc.longitude = currentConfig.getDefaultLongitude()
        return loc
    }

    // -------------------- 工具方法 --------------------
    /** Haversine公式计算两点距离（单位：米） */
    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(
                dLon / 2
            ).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (R * c).toFloat()
    }

    /** 微抖动 + 大跳变过滤 */
    private fun isValidLocation(last: Location?, current: Location): Boolean {
        last ?: return true
        val distance =
            distanceBetween(last.latitude, last.longitude, current.latitude, current.longitude)
        if (distance < currentConfig.getFilterMin()) return false
        if (distance > currentConfig.getFilterMax()) return false
        return true
    }

    // -------------------- 核心处理逻辑 --------------------
    /**
     * 核心处理逻辑
     * @param force 是否强制回调（忽略微抖动/大跳变过滤）
     */
    fun processLocation(location: Location, force: Boolean = false) {
        synchronized(lock) {
            if (singleLocationReceived) return

            val type = currentConfig.getLocationType()

            if (!force && !isValidLocation(lastLocation, location)) return

            if (!force && type == LocationType.FUSION && lastLocation != null) {
                if (location.accuracy >= lastLocation!!.accuracy) return
            }

            if (!force && lastLocation != null && lastLocation!!.latitude == location.latitude && lastLocation!!.longitude == location.longitude) return

            lastLocation = location
            callback?.invoke(LocationResult(true, location, "定位成功"))

            if (type == LocationType.SINGLE) {
                singleLocationReceived = true
                stopLocation()
            }

            timeoutRunnable?.let { handler.removeCallbacks(it) }

            // ✅ 广播定位事件（全局）
            if (currentConfig.isUseBroadcast()) {
                val intent = Intent(LocationBroadcast.ACTION_LOCATION_UPDATE)
                intent.putExtra(LocationBroadcast.EXTRA_LOCATION, location)
                appContext.sendBroadcast(intent)
            }
        }
    }

    // -------------------- LocationListener 回调 --------------------
    override fun onLocationChanged(location: Location) {
        processLocation(location)
    }

    override fun onLocationChanged(locations: List<Location>) {
        if (locations.isNotEmpty()) processLocation(locations[0])
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("定位状态", "$provider 已启用")
    }

    override fun onProviderDisabled(provider: String) {
        callback?.invoke(LocationResult(false, createDefaultLocation(),"$provider 被禁用"))
    }
}

