
package com.wkq.location


/**
 * 定位任务配置类
 *
 * 每次定位任务都可以独立配置参数，不会影响其他任务。
 * 支持链式调用（Builder 风格），并提供详细注释说明。
 */
class LocationConfig {

    /** 定位超时时间（毫秒），默认 6000L */
    private var timeout: Long = 6000L

    /** 连续两次定位的最小时间间隔（毫秒），默认 5000ms */
    private var minTimeMs: Long = 5000L

    /** 连续两次定位的最小距离（米），默认 1 米 */
    private var minDistanceM: Float = 1f

    /** 是否开启抖动过滤，默认 false */
    private var isFilter: Boolean = false

    /** 抖动过滤最小纬度 过滤细微跳变 */
    // 纬度每度 ≈ 111,000 米（平均值）  0.0001°×111,000米/°≈11.1米
    // 经度米数=111,000×cos(纬度) 0.0001×111,000×cos(39.9°)≈8.5米
    private var filterMin = 5f


    /** 过滤大的跳变 允许的位置跳变最大距离（米），超过此值认为异常，默认 200 米 */
    private var filterMax: Float = 200f

    /** 定位失败时的默认纬度（北京 39.9042） */
    private var defaultLatitude: Double = 39.9042

    /** 定位失败时的默认经度（北京 116.4074） */
    private var defaultLongitude: Double = 116.4074

    /** 是否通过广播发送定位结果，默认 false */
    private var useBroadcast: Boolean = false


    /** 定位类型，默认使用融合定位 */
    private var locationType: LocationType = LocationType.FUSION

    // ================= 设置方法（链式调用） =================

    /** 设置定位超时时间 */
    fun setTimeout(value: Long) = apply { this.timeout = value }

    /** 设置连续两次定位最小时间间隔 */
    fun setMinTimeMs(value: Long) = apply { this.minTimeMs = value }

    /** 设置连续两次定位最小距离 */
    fun setMinDistanceM(value: Float) = apply { this.minDistanceM = value }
    fun setFilter(isFilter: Boolean) = apply { this.isFilter = isFilter }



    fun setFilterMin(value: Float) = apply { this.filterMin = value }

    fun getFilterMin() = filterMin


    /** 设置默认纬度（定位失败时使用） */
    fun setDefaultLatitude(value: Double) = apply { this.defaultLatitude = value }

    /** 设置默认经度（定位失败时使用） */
    fun setDefaultLongitude(value: Double) = apply { this.defaultLongitude = value }

    /** 设置允许的位置跳变最大距离 */
    fun setFilterMax(value: Float) = apply { this.filterMax = value }

    /** 设置是否通过广播发送定位结果 */
    fun setUseBroadcast(value: Boolean) = apply { this.useBroadcast = value }

    /** 设置定位类型 */
    fun setLocationType(value: LocationType) = apply { this.locationType = value }

    // ================= 获取方法 =================

    /** 获取定位超时时间 */

    fun getTimeout(): Long {
        // 优先返回用户自定义超时，如果没有则按模式默认值
        return timeout ?: when (getLocationType()) {
            LocationType.FAST -> 3000L
            LocationType.SINGLE,
            LocationType.FUSION -> timeout
            else -> timeout
        }
    }

    /** 获取连续两次定位最小时间间隔 */
    fun getMinTimeMs(): Long = minTimeMs

    /** 获取连续两次定位最小距离 */
    fun getMinDistanceM(): Float = minDistanceM
    /** 获取抖动开关 */
    fun getFilter(): Boolean = isFilter



    /** 获取默认纬度 */
    fun getDefaultLatitude(): Double = defaultLatitude

    /** 获取默认经度 */
    fun getDefaultLongitude(): Double = defaultLongitude

    /** 获取允许的位置跳变最大距离 */
    fun getFilterMax(): Float = filterMax

    /** 是否通过广播发送定位结果 */
    fun isUseBroadcast(): Boolean = useBroadcast

    /** 获取定位类型 */
    fun getLocationType(): LocationType = locationType

    companion object {
        /**
         * 获取默认配置实例
         * 可直接使用，也可通过链式调用修改参数
         */
        fun default(): LocationConfig = LocationConfig()
    }
}
