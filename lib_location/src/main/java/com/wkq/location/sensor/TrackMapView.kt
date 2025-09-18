package com.wkq.location.sensor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/17 9:54
 *
 *@Desc:
 */

class TrackMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var trackPoints: List<SensorPointInfo> = emptyList()
    var markerBitmap: Bitmap? = null
    var startBitmap: Bitmap? = null
    var endBitmap: Bitmap? = null

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val path = Path()

    private var animatedIndex = 0
    private var animatedFraction = 0f
    private val animationInterval = 16L // ms

    private val minSpeed = 0.5f    // m/s，最小动画速度
    private val speedMultiplier = 1.5f // 倍速，可调
    private val minDistanceThreshold = 0.5 // m，微小距离合并

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (trackPoints.isEmpty()) return

        val minLat = trackPoints.minOf { it.lat }
        val maxLat = trackPoints.maxOf { it.lat }
        val minLng = trackPoints.minOf { it.lng }
        val maxLng = trackPoints.maxOf { it.lng }

        val latRange = (maxLat - minLat).coerceAtLeast(0.000001)
        val lngRange = (maxLng - minLng).coerceAtLeast(0.000001)

        fun toScreenX(lng: Double) = ((lng - minLng) / lngRange * (width * 0.9f) + width * 0.05f).toFloat()
        fun toScreenY(lat: Double) = ((maxLat - lat) / latRange * (height * 0.9f) + height * 0.05f).toFloat()

        // 绘制轨迹
        path.reset()
        for ((i, point) in trackPoints.withIndex()) {
            val x = toScreenX(point.lng)
            val y = toScreenY(point.lat)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, pathPaint)

        // 起点
        trackPoints.firstOrNull()?.let {
            val x = toScreenX(it.lng)
            val y = toScreenY(it.lat)
            startBitmap?.let { bmp ->
                canvas.drawBitmap(bmp, x - bmp.width / 2f, y - bmp.height / 2f, null)
            }
        }

        // 终点
        trackPoints.lastOrNull()?.let {
            val x = toScreenX(it.lng)
            val y = toScreenY(it.lat)
            endBitmap?.let { bmp ->
                canvas.drawBitmap(bmp, x - bmp.width / 2f, y - bmp.height / 2f, null)
            }
        }

        // 动画 marker
        if (trackPoints.size > 1 && markerBitmap != null) {
            val currentPoint = getAnimatedPosition(trackPoints)
            val x = toScreenX(currentPoint.lng)
            val y = toScreenY(currentPoint.lat)
            canvas.save()
            canvas.rotate(currentPoint.heading, x, y)
            canvas.drawBitmap(markerBitmap!!, x - markerBitmap!!.width / 2f, y - markerBitmap!!.height / 2f, null)
            canvas.restore()
        }
    }

    private fun getAnimatedPosition(points: List<SensorPointInfo>): SensorPointInfo {
        if (animatedIndex >= points.size - 1) return points.last()
        val p0 = points[animatedIndex]
        val p1 = points[animatedIndex + 1]

        // 平滑 heading
        val heading = interpolateAngle(p0.heading, p1.heading, animatedFraction)
        val lat = p0.lat + (p1.lat - p0.lat) * animatedFraction
        val lng = p0.lng + (p1.lng - p0.lng) * animatedFraction
        val alt = p0.altitude + (p1.altitude - p0.altitude) * animatedFraction
        val speed = p0.speed.coerceAtLeast(minSpeed)
        return SensorPointInfo(lat, lng, alt, heading, speed)
    }

    private fun interpolateAngle(start: Float, end: Float, fraction: Float): Float {
        var delta = (end - start + 360) % 360
        if (delta > 180) delta -= 360
        return (start + delta * fraction + 360) % 360
    }

    fun startAnimation() {
        if (trackPoints.size < 2) return
        animatedIndex = 0
        animatedFraction = 0f
        post(animationRunnable)
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (animatedIndex >= trackPoints.size - 1) return
            val p0 = trackPoints[animatedIndex]
            val p1 = trackPoints[animatedIndex + 1]
            var distance = distanceBetween(p0.lat, p0.lng, p1.lat, p1.lng)

            // 微小距离合并
            if (distance < minDistanceThreshold) {
                animatedIndex++
                animatedFraction = 0f
                postDelayed(this, animationInterval)
                invalidate()
                return
            }

            val speed = (p0.speed.coerceAtLeast(minSpeed) * speedMultiplier).toDouble()
            val fractionIncrement = (speed * animationInterval / 1000.0) / distance
            animatedFraction += fractionIncrement.toFloat()

            if (animatedFraction >= 1f) {
                animatedFraction = 0f
                animatedIndex++
            }

            invalidate()
            if (animatedIndex < trackPoints.size - 1) postDelayed(this, animationInterval)
        }
    }

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
