package com.wkq.locationkit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.lifecycle.lifecycleScope
import com.wkq.base.activity.BasicVbActivity
import com.wkq.base.util.showToast
import com.wkq.locationkit.databinding.ActivityLocationShowBinding
import com.wkq.location.LocationConfig
import com.wkq.location.LocationKit
import com.wkq.location.LocationType
import kotlinx.coroutines.launch

class LocationShowActivity : BasicVbActivity<ActivityLocationShowBinding>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, LocationShowActivity::class.java))
        }
    }

    private val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private var hasPermission = false

    override fun initView() {
        binding.tvName.text = DeviceInfoUtils.getPhoneInfo(this)
        hasPermission = isGranted(permissions)
        if (hasPermission)  startLocation()

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
        LocationKit.startLocation(this) { result ->
            lifecycleScope.launch {
                if (result.success && result.location != null) {

                    val loc: Location? = result.location
                    if (loc==null)return@launch
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
        }
    }
}
