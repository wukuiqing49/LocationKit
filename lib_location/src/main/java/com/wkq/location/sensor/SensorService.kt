package com.wkq.location.sensor


import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.wkq.location.LocationSensorFusionManager


/**
 * 前台服务 + 传感器采集 + 通知开关提醒
 */
class SensorService : Service() {

    private lateinit var sensorHelper: SensorHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // 检测通知是否被禁用
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openNotificationSettings()
        }

        // Android 8.0+ 前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createForegroundNotification()
            startForeground(1, notification)
        }
       var  fusionManager = LocationSensorFusionManager(this)
        fusionManager.start(object : LocationCallback {


            override fun onLocationUpdate(info: SensorPointInfo) {

            }

            override fun onSensorSupportStatus(
                sensors: List<SensorStatus>
            ) {

            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        sensorHelper.stop()
    }

    /**
     * 创建前台通知
     */
    private fun createForegroundNotification(): Notification {
        val channelId = "sensor_service_channel"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Sensor Service",
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }

            return Notification.Builder(this, channelId)
                .setContentTitle("Sensor Service")
                .setContentText("正在运行中")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build()
        } else {
            return Notification.Builder(this)
                .setContentTitle("Sensor Service")
                .setContentText("正在运行中")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build()
        }
    }



    private fun openNotificationSettings() {
        val intent = Intent()
        intent.action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0 及以上
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        } else {
            // Android 8.0 以下
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }

        // 包名参数
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

}
