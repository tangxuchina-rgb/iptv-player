# IPTV 播放器 · Android APK 打包工程

把 `iptv-player.html` 网页播放器封装成一个可安装到安卓手机的 APK（WebView 壳）。
**本机无需安装 Android SDK** —— 推送到 GitHub 后由 GitHub Actions 自动编译出 APK。

---

## 一、三步拿到 APK

### 1. 在 GitHub 新建仓库
登录 https://github.com，点右上角 **+ → New repository**，建一个空仓库（public 或 private 都行），**不要**勾选初始化 README。

### 2. 上传本目录
在本目录（`android-iptv/`）下打开终端，执行（把 `你的用户名/你的仓库名` 换成实际值）：

```bash
git init
git add .
git commit -m "IPTV player"
git branch -M main
git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

### 3. 等待并下载 APK
推送后 GitHub 会自动开始编译：

1. 打开仓库页面 → 顶部 **Actions** 标签
2. 看到名为 **Build APK** 的工作流正在跑（约 2–4 分钟）
3. 跑完点进去，页面底部 **Artifacts** 区域下载 **IPTV-Player-APK**
4. 解压得到的 `app-release.apk` / `app-debug.apk` 即可安装到手机

> 也可以在 Actions 页面点 **Run workflow** 手动触发重新编译。

---

## 二、安装到手机

1. 把 APK 传到手机（数据线 / 微信文件传输 / 云盘都行）
2. 用手机文件管理器点击安装
3. 首次安装需在系统设置里**允许「安装未知来源应用」**
4. 安装后打开，点「导入播放列表」加载你的 m3u 源即可

---

## 三、工程说明

| 文件 | 作用 |
|---|---|
| `app/src/main/assets/iptv-player.html` | 播放器本体（你的网页，已放进来） |
| `app/src/main/java/com/iptvplayer/app/MainActivity.java` | WebView 壳：全屏视频、文件选择、返回键处理 |
| `app/src/main/AndroidManifest.xml` | 权限与入口配置（已允许明文 http 源） |
| `.github/workflows/build-apk.yml` | GitHub Actions 自动编译脚本 |

**已内置的适配：**
- 允许 HLS（m3u8）与明文 http 源（IPTV 常见）
- 关闭「需要用户手势才能播放」，支持自动播放
- 视频全屏时自动横屏 + 常亮
- `<input type="file">` 支持，可从手机里选 m3u 文件
- 返回键：先退全屏 → 再退网页历史 → 最后退出应用

**应用信息：** 包名 `com.iptvplayer.app`，最低支持 Android 5.0（API 21），目标 API 34。

---

## 四、本地编译（可选，需自行安装环境）

如果你有 Android Studio，直接用 Android Studio 打开本目录即可编译。
命令行方式需自行安装 JDK 17 + Android SDK，然后：

```bash
gradle assembleRelease
```

产物在 `app/build/outputs/apk/`。

---

## 五、常见问题

**Q：release 和 debug 用哪个？**
两者都用 debug 签名，能直接安装，装哪个都行。release 未经混淆优化，体积略小。

**Q：APK 签名能上架应用商店吗？**
不能。当前用的是 debug 签名，仅供自用/测试。要上架需配置自己的签名密钥（release signing config）。

**Q：能播 4K 或 H.265 频道吗？**
取决于手机芯片的硬件解码能力。中高端机型一般支持 H.265/HEVC；老旧机型可能只有 H.264。

**Q：播放源从哪来？**
本工程不自带任何频道源。请使用自己合法持有的 IPTV 账号或已获授权的服务。
