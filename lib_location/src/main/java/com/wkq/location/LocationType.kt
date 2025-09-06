package com.wkq.location

/**
 * 定位类型枚举
 *
 * 表示不同的定位模式，可用于动态控制定位策略。
 */
enum class LocationType {

    /** 快速定位（优先速度，可能精度较低） */
    FAST,

    /** 单次定位（使用融合定位，仅获取一次） */
    SINGLE,

    /** 融合定位（GPS + 网络综合） */
    FUSION,

    /** 仅使用单次网络定位 */
    LOCATION_NET,

    /** 仅使用单次 GPS 定位 */
    LOCATION_GPS,
}
