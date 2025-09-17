package com.wkq.location.sensor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.pow
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
    var markerBitmap: Bitmap? = null      // 动画移动图标
    var startBitmap: Bitmap? = null       // 起点图标
    var endBitmap: Bitmap? = null         // 终点图标

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val path = Path()

    private var animatedIndex = 0
    private var animatedFraction = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (trackPoints.isEmpty()) return

        // 经纬度范围
        val minLat = trackPoints.minOf { it.lat }
        val maxLat = trackPoints.maxOf { it.lat }
        val minLng = trackPoints.minOf { it.lng }
        val maxLng = trackPoints.maxOf { it.lng }

        val latRange = (maxLat - minLat).coerceAtLeast(0.000001)
        val lngRange = (maxLng - minLng).coerceAtLeast(0.000001)

        // 屏幕坐标映射（留 5% 边距）
        fun toScreenX(lng: Double) = ((lng - minLng) / lngRange * (width * 0.9f) + width * 0.05f).toFloat()
        fun toScreenY(lat: Double) = ((maxLat - lat) / latRange * (height * 0.9f) + height * 0.05f).toFloat()

        // 绘制轨迹
        path.reset()
        for ((i, point) in trackPoints.withIndex()) {
            val x = toScreenX(point.lng)
            val y = toScreenY(point.lat)
            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }
        canvas.drawPath(path, pathPaint)

        // 绘制开始图标
        trackPoints.firstOrNull()?.let {
            val x = toScreenX(it.lng)
            val y = toScreenY(it.lat)
            startBitmap?.let { bmp ->
                canvas.drawBitmap(bmp, x - bmp.width / 2f, y - bmp.height / 2f, null)
            }
        }

        // 绘制结束图标
        trackPoints.lastOrNull()?.let {
            val x = toScreenX(it.lng)
            val y = toScreenY(it.lat)
            endBitmap?.let { bmp ->
                canvas.drawBitmap(bmp, x - bmp.width / 2f, y - bmp.height / 2f, null)
            }
        }

        // 绘制动画 marker
        if (trackPoints.size > 1 && markerBitmap != null) {
            val currentPoint = getAnimatedPosition(trackPoints)
            val x = toScreenX(currentPoint.lng)
            val y = toScreenY(currentPoint.lat)
            canvas.save()
            // 旋转中心 marker 中心
            canvas.rotate(currentPoint.heading, x, y)
            canvas.drawBitmap(
                markerBitmap!!,
                x - markerBitmap!!.width / 2f,
                y - markerBitmap!!.height / 2f,
                null
            )
            canvas.restore()
        }
    }

    private fun getAnimatedPosition(points: List<SensorPointInfo>): SensorPointInfo {
        if (animatedIndex >= points.size - 1) return points.last()
        val p0 = points[animatedIndex]
        val p1 = points[animatedIndex + 1]
        val x = p0.lat + (p1.lat - p0.lat) * animatedFraction
        val y = p0.lng + (p1.lng - p0.lng) * animatedFraction
        val heading = interpolateAngle(p0.heading, p1.heading, animatedFraction)
        return SensorPointInfo(x, y, p0.altitude, heading, p0.speed)
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
            val dt = 16L
            val distance = distanceBetween(p0.lat, p0.lng, p1.lat, p1.lng)
            if (distance < 0.01) { // 微小距离直接跳过
                animatedIndex++
                animatedFraction = 0f
                invalidate()
                postDelayed(this, dt)
                return
            }
            val speed = (p0.speed.coerceAtLeast(0.5f)).toDouble()
            val fractionIncrement = (speed * dt / 1000.0) / distance
            animatedFraction += fractionIncrement.toFloat()
            if (animatedFraction >= 1f) {
                animatedFraction = 0f
                animatedIndex++
            }
            invalidate()
            if (animatedIndex < trackPoints.size - 1) postDelayed(this, dt)
        }
    }

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).pow(2.0) + Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
