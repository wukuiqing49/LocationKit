package com.wkq.locationkit

import android.Manifest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BasicVbActivity
import com.wkq.base.util.showToast
import com.wkq.locationkit.databinding.ActivityLocationConfigBinding
import com.wkq.location.LocationBroadcast
import com.wkq.location.LocationConfig
import com.wkq.location.LocationKit
import com.wkq.location.LocationType
import kotlinx.coroutines.launch
import kotlin.apply
import kotlin.collections.all
import kotlin.jvm.java
import kotlin.text.toDoubleOrNull
import kotlin.text.toFloatOrNull
import kotlin.text.toLongOrNull

class LocationConfigTestActivity : BasicVbActivity<ActivityLocationConfigBinding>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, LocationConfigTestActivity::class.java))
        }
    }
    private lateinit var locationReceiver: LocationReceiver
    // 定位权限
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // 权限回调
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.entries.all { it.value }
            if (granted) {
                showToast("权限已授予")
                startLocation()
            } else {
                showToast("定位权限未授予，无法定位")
            }
        }

    override fun initView() {
        // 模式选择 Spinner
        val modes = listOf("SINGLE", "FUSION", "FAST", "GPS", "NETWORK")
        val adapter = ArrayAdapter(this,  android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spMode.adapter = adapter

        binding.btnStart.setOnClickListener {
            // 先检查权限
            if (hasLocationPermission()) {
                startLocation()
            } else {
                requestLocationPermission()
            }
        }

        binding.btnStop.setOnClickListener {
            LocationKit.stopLocation()
            binding.tvResult.text = "⏹ 定位已停止"
        }
    }

    // 检查定位权限
    private fun hasLocationPermission(): Boolean {
        return locationPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 动态请求权限
    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionLauncher.launch(locationPermissions)
        }
    }

    private fun startLocation() {
        // 构建 LocationConfig
        val config = LocationConfig().apply {
            setLocationType(
                when (binding.spMode.selectedItem.toString()) {
                    "SINGLE" -> LocationType.SINGLE
                    "FUSION" -> LocationType.FUSION
                    "FAST" -> LocationType.FAST
                    "GPS" -> LocationType.LOCATION_GPS
                    "NETWORK" -> LocationType.LOCATION_NET
                    else -> LocationType.FUSION
                }
            )
            setMinTimeMs(binding.etMinTime.text.toString().toLongOrNull() ?: 1000L)
            setUseBroadcast(binding.switchBroadcast.isChecked)
            setMinDistanceM(binding.etMinDistance.text.toString().toFloatOrNull() ?: 1f)
            setFilter(binding.switchFilter.isChecked)
            setFilterMin(binding.etFilterMin.text.toString().toFloatOrNull() ?: 1f)
            setFilterMax(binding.etFilterMax.text.toString().toFloatOrNull() ?: 100f)
            setDefaultLatitude(binding.etDefaultLat.text.toString().toDoubleOrNull() ?: 39.90923)
            setDefaultLongitude(binding.etDefaultLon.text.toString().toDoubleOrNull() ?: 116.397428)
            setTimeout(binding.etTimeout.text.toString().toLongOrNull() ?: 8000L)
        }

        LocationKit.init(this, config)
        LocationKit.startLocation(this) { result ->
            runOnUiThread {
                if (result.success && result.location != null) {
                    val loc = result.location!!
                    lifecycleScope.launch {
                        val address = LocationResolverHelper.getAddress(this@LocationConfigTestActivity, loc.latitude, loc.longitude)
                        binding.tvResult.text = buildString {
                            append("✅ 定位成功\n")
                            append("类型: ${loc.provider}\n")
                            append("时间: ${loc.time}\n")
                            append("经纬度: ${loc.latitude}, ${loc.longitude}\n")
                            append("地址: ${address?.address ?: "未知"}\n")
                            append("城市: ${address?.city ?: "未知"}\n")
                            append("省/州: ${address?.province ?: "未知"}\n")
                            append("国家: ${address?.country ?: "未知"}")
                        }
                    }
                } else {
                    binding.tvResult.text = "❌ 定位失败: ${result.msg}"
                }
            }
        }
    }

    override fun initData() {
        // 默认值初始化
        binding.etMinTime.setText("1000")
        binding.etMinDistance.setText("1")
        binding.etFilterMin.setText("1")
        binding.etFilterMax.setText("100")
        binding.etDefaultLat.setText("39.90923")
        binding.etDefaultLon.setText("116.397428")
        binding.etTimeout.setText("8000")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        locationReceiver = LocationReceiver()
        val filter = IntentFilter(LocationBroadcast.ACTION_LOCATION_UPDATE)

        if (Build.VERSION.SDK_INT >= 31) {
            registerReceiver(locationReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(locationReceiver, filter)
        }
    }


    override fun onStop() {
        super.onStop()
        unregisterReceiver(locationReceiver)
    }
}
