# 天气App开发文档 (Android 13)

## 功能概述

- 使用腾讯地图SDK获取位置信息
- 使用彩云天气API获取天气数据
- 支持IP定位和关键词输入提示功能

## 技术实现

### 1. 腾讯地图SDK配置

```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="TencentMapSDK"
    android:value="YOUR_TENCENT_MAP_KEY"/>
```

### 2. 彩云天气API配置

```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="CaiYunWeatherAPI"
    android:value="YOUR_CAIYUN_API_KEY"/>
```

### 3. 腾讯位置服务Key配置要求

- 启用产品: WebServiceAPI和SDK
- WebServiceAPI域名白名单: 不填任何域名(不校验)
- 必须分配的功能:
    - IP定位
    - 关键词输入提示功能
    - 足够的调用额度

## 配置界面演示

<div style="display: flex; justify-content: space-between;">
  <img src="https://alist.aikrai.com/d/oss-aikrai-hk-pixel/picgo/766ba95ba972cad1084b5414deb21d4.jpg" width="48%" alt="腾讯位置服务配置界面1"/>
  <img src="https://alist.aikrai.com/d/oss-aikrai-hk-pixel/picgo/d189d9dab1c654e87776eeff56ffa0c.jpg" width="48%" alt="腾讯位置服务配置界面2"/>
</div>