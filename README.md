# Airbods

**EN** · Control AirPods noise modes (ANC / Transparency / Adaptive) on Android phones without root — via AACP over Bluetooth L2CAP.

**中文** · 在安卓手机上（无需 Root）通过 AACP（L2CAP）切换 AirPods 降噪 / 通透 / Adaptive。

> Not affiliated with Apple. MIT licensed. Protocol helpers are reimplemented from public AACP documentation — no LibrePods GPL code is copied.

## Install

1. Download `Airbods-0.1.0.apk` from [Releases](https://github.com/chinatsu033/Airbods/releases).
2. If you previously installed **AirPodsMode** (`io.github.chinatsu033.airpodsmode`), you can keep it or uninstall it — **Airbods** uses a new package id and installs side-by-side.
3. Grant **Nearby devices / Bluetooth**, then **Notifications** (for background connection + last location). Location is optional and only needed for「最后位置」.

## Features

| Feature | Notes |
|--------|--------|
| Noise modes | ANC, Transparency, Adaptive |
| Adaptive strength | 0 / 50 / 100 chips |
| Conversational Awareness | Toggle when connected |
| Last location | GPS on disconnect while FGS runs |
| Quick Settings | **One** tile named **Airbods** → Material mode panel |
| Permissions | Does not crash if notifications denied; foreground-only connect still works |

## UI

Material 3 solid colors, Material You dynamic color, light/dark follows system. Rounded-square buttons (no liquid glass / wallpaper backgrounds).

## Package

- Application id: `com.chinatsu.airbods`
- Min SDK 29 · Target SDK 35

## Build

```bash
export AIRBODS_SIGNING_PROPERTIES=/path/to/signing.properties
./gradlew :app:assembleRelease
```

## Privacy

Bluetooth stays on-device. Location is stored only locally when you grant it and AirPods disconnect while the connection service is running.

---

## 中文说明

- **包名**：`com.chinatsu.airbods`（与旧版 AirPodsMode 可并存；可卸载旧应用）
- **快捷设置**：只保留一个名为 **Airbods** 的磁贴，点开即为纯色 Material 模式面板
- **最后位置**：首页卡片 / 设置入口；需通知 + 定位 + 前台服务连接后再断开耳机才会记录
- **权限**：拒绝通知不会闪退；应用打开时仍可前台连接并切换模式
