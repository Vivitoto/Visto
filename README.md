# Visto

Visto 是一个 **只读 WebDAV 相册查看器**,用于在 Android 设备上浏览 WebDAV 服务器里的照片、视频和文件夹。

> 当前版本：`v0.1.5`
> 包名:`app.visto`

[English README](README.en.md)

## 功能范围

Visto v0.1 主要用于安全地浏览远端媒体:

- 添加一个 WebDAV 账号
- 浏览远端文件夹
- 将 WebDAV 文件夹作为相册来源
- 查看图片和视频
- 浏览缩略图、GIF 标识、视频标识
- 手动加载原图,避免默认消耗大量流量
- 本地缓存缩略图和目录元数据
- 支持清理本地缓存

## 安全边界

Visto 当前是只读应用。v0.1 不提供这些远端写操作:

- 不上传文件
- 不删除远端文件
- 不移动文件
- 不重命名文件
- 不创建远端目录
- 不修改 WebDAV 服务器内容

也就是说,Visto 只读取 WebDAV 内容,用于浏览和查看。

## 下载

当前推荐 APK 在 GitHub Release:

- https://github.com/Vivitoto/Visto/releases/tag/latest

请下载 `visto-v0_1_5.apk`。

## 构建

本地构建需要:

- Java 17
- Android SDK
- Gradle Wrapper

常用命令:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

正式 APK 由 GitHub Actions 构建并使用 Visto 专用 release key 签名。

## 项目文档

设计和实现计划:

- `docs/plans/2026-06-03-visto-design.md`
- `docs/plans/2026-06-03-visto-tasks.md`
- `docs/plans/2026-06-04-visto-original-load-and-webdav-paths.md`
