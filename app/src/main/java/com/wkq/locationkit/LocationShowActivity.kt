package com.wkq.locationkit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BasicVbActivity
import com.wkq.base.util.showToast
import com.wkq.location.LocationSensorFusionManager
import com.wkq.locationkit.databinding.ActivityLocationShowBinding
import com.wkq.location.address.LocationConfig
import com.wkq.location.LocationKit
import com.wkq.location.address.LocationType
import com.wkq.location.sensor.LocationCallback
import com.wkq.location.sensor.SensorCheckUtils
import com.wkq.location.sensor.SensorPointInfo
import com.wkq.location.sensor.SensorService
import com.wkq.location.sensor.SensorStatus
import kotlinx.coroutines.launch

class LocationShowActivity : BasicVbActivity<ActivityLocationShowBinding>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, LocationShowActivity::class.java))
        }
    }

    private val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,

        )
    private lateinit var fusionManager: LocationSensorFusionManager

    private val permissionsService = listOf(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION

    )
    private var hasPermission = false

    override fun initView() {
        binding.tvName.text = DeviceInfoUtils.getPhoneInfo(this)
        hasPermission = isGranted(permissions)
        if (hasPermission) startLocation()

        binding.btLocation.setSafeClickListener {
            if (!hasPermission) {
                requestLocation()
                return@setSafeClickListener
            }
            startLocation()
        }

        binding.btLocationProcess.setSafeClickListener {
            LocationConfigTestActivity.startActivity(this)
        }
        binding.btDraw.setSafeClickListener {

            binding.trackView.trackPoints = fusionManager.trackPoints
            binding.trackView.markerBitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_mylocation) // 可配置图标
            binding.trackView.startBitmap = BitmapFactory.decodeResource(resources, android.R.drawable.star_off) // 可配置图标
            binding.trackView.endBitmap = BitmapFactory.decodeResource(resources, android.R.drawable.star_on) // 可配置图标
            binding.trackView.startAnimation()
        }
        fusionManager = LocationSensorFusionManager(this)

        binding.btSensor.setOnClickListener {
            if (isGranted(permissionsService)) {
                startSensor()
            } else {
                requestPermissionsLauncher(10011, permissionsService as MutableList<String>)
            }


        }
    }


    fun startSensor() {
        SensorCheckUtils.getCommonSensorStatus(this)

        fusionManager.start(object : LocationCallback {
            override fun onLocationUpdate(info: SensorPointInfo) {
                // 可以在 UI 上显示
                showToast(
                    "位置: ${info.lat}, ${info.lng}, 高度: ${info.altitude}, 方向: ${info.heading}, 速度: ${info.speed}"
                )

            }

            override fun onSensorSupportStatus(
                sensors: List<SensorStatus>
            ) {
//                sensors.forEach { sensor ->
//                    when (sensor.type) {
//                        "RotationVector" -> {
//                            if (!sensor.supported) {
//                                showToast("传感器支持: 不支持压力传感器")
//                            }
//                            Log.d("传感器支持", "支持旋转矢量传感器")
//                        }
//
//                        "StepDetector" -> {
//                            if (!sensor.supported) {
//                                showToast("传感器支持: 不支持计步传感器")
//                            }
//                        }
//
//                        "Pressure" -> {
//                            Log.d("传感器支持", "支持压力传感器")
//                            if (!sensor.supported) {
//                                showToast("传感器支持: 不支持压力传感器")
//                            }
//                        }
//
//                        "LinearAcceleration" -> {
//                            Log.d("传感器支持", "支持线性加速度传感器")
//                            if (!sensor.supported) {
//                                showToast("传感器支持: 不支持线性加速度传感器")
//                            }
//                        }
//                    }
//                }
            }
        })
    }

    override fun initData() {

    }

    private fun startLocation() {
        // 动态构建 LocationConfig
        val config = LocationConfig().apply {
            setLocationType(LocationType.FUSION) // 可根据需要选择模式
            setMinTimeMs(1000L)
            setMinDistanceM(1f)
            setFilter(true)
            setFilterMin(1f)
            setFilterMax(100f)
            setDefaultLatitude(39.90923)
            setDefaultLongitude(116.397428)
            setTimeout(5000L)
        }

        // 初始化 LocationKit
        LocationKit.init(this, config)

        // 启动定位
        LocationKit.startLocation() { result ->
            lifecycleScope.launch {
                if (result.success && result.location != null) {

                    val loc: Location? = result.location
                    if (loc == null) return@launch
                    binding.tvLocation.text =
                        "类型: ${loc.provider}\n纬度: ${loc.latitude}, 经度: ${loc.longitude}"

                    // 可选：解析地址
                    val address = LocationResolverHelper.getAddress(
                        this@LocationShowActivity,
                        loc.latitude,
                        loc.longitude
                    )
                    binding.tvAddress.text =
                        "位置: ${address?.address}\n城市: ${address?.city}\n省份: ${address?.province}\n国家: ${address?.country}"
                } else {
                    binding.tvLocation.text = "定位失败: ${result.msg}"
                    binding.tvAddress.text = ""
                }
            }
        }
    }

    private fun requestLocation() {
        requestPermissionsLauncher(10010, permissions as MutableList<String>)
    }

    override fun authorized(permissionType: Int, permissionList: MutableList<String>) {
        if (permissionType == 10010) {
            hasPermission = true
            showToast("权限已授予")
        } else if (permissionType == 10011) {
            startSensor()
            showToast("权限已授予")
        }
    }

    private fun startSensorService() {
        val intent = Intent(this, SensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }
    }


}



