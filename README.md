
![效果图](picture/image_1.png)
![效果图2](picture/image.png)


# 定位

项目中已使用暂未发现问题

## 介绍
由于某度某德定位开始收费,开始研究Android自带的定位,用于替换项目中简单的定位和获取位置信息的功能.实现了定位和获取位置信息的功能


## 功能
- 定位
- 位置信息

## 1.定位

- GPS定位
- 网络定位
- Android缓存位置获取

## 2:位置信息
由于Google 服务被禁止,除华为(HMS)能通过自身服务能获取位置信息,其他手机只能通过经纬度获度不能获取位置信息.所以位置信息需要自己想办法获取服务.

研究了一下网上的方案,采取GeoNames生成数据库来模糊定位(精准度到10Km左右).

有需要可以联系作者根据需求生成数据库的方式


## 4. 使用方式

#### 网络库引用

```
//位置信息辅助类(GeoNames,补充,非必选)
implementation 'com.github.wukuiqing49:LocationAddress:v1.1.3'

// 获取经纬度信息
 implementation 'com.github.wukuiqing49:Location:v1.1.3'
```
#### 4.1 动态申请权限

```
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

#### 4.2 开启定位


```
      val locationSingleHelper = AndroidLocationManager(
            context = this,            // Activity 或 Context
            timeout = 5000L,           // 超时 5 秒
            singleUpdate = true ,// 单次定位
            useBroadcast =  true, // 广播方式得到结果
            callback = callback
        )

     locationSingleHelper?.startLocation()
```


#### 4.3 得到定位结果


```
    private val callback: (location: Location?) -> Unit = { location ->
        val geoHelper = GeoDbHelper(this)
        // 附近的位置
        val nearest = geoHelper.getNearestPlace(location!!.latitude, location!!.longitude) // 北京经纬度
        // 附近的位置列表
        val list = geoHelper.getNearbyPlaces(location!!.latitude, location!!.longitude) // 北京经纬度

        list.forEach { Log.d("定位状态", "附近位置: $it") }
        binding.tvAddress.text ="位置信息:${nearest.toString()}"

        binding.tvLocation.text = "类型: ${location?.provider} \n" + "纬度: ${location?.latitude}, 经度: ${location?.longitude}"
    }

```



## 5:生效机型
| 序号 | 品牌    | 机型       | 系统版本                             | 备注 | 结果 |
|----|-------|----------|----------------------------------|----|----|
| 1  | 华为    | ELE-AL00 | Android 10                       | 生效 | ✓  |
| 2  | OPPO  | PCAM00   | Android 11                       | 生效 | ✓  |
| 3  | HONOR | OXF-AN00 | Android 12(EMU 14.2.0) | 生效 | ✓  |

...





欢迎大家提交结果,和提出问题

持续更新，长期维护

