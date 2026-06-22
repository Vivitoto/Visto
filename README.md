# Visto

Visto 是一个面向 Android 的 **只读 WebDAV 媒体与阅读浏览器**。它用于在移动设备上安全浏览 WebDAV 服务器中的相册、图片、视频、文件夹和书籍文件，并提供本地阅读进度与阅读外观设置。

> 当前版本：`v1.1.0`
>
> 包名：`app.visto`

[English README](README.en.md)

## 功能范围

Visto 当前聚焦于安全、轻量的远端内容浏览：

- 添加和管理 WebDAV 账号
- 浏览远端文件夹
- 将 WebDAV 文件夹作为相册来源
- 查看图片、GIF 和视频
- 浏览缩略图、GIF 标识、视频标识
- 手动加载原图，避免默认消耗大量流量
- 本地缓存缩略图、目录元数据和书籍文本缓存
- 清理本地缓存
- 浏览已打开过的书籍书架
- 支持书架列表 / 3 列 / 5 列布局切换
- 为书籍生成本地占位封面，并显示阅读进度
- 阅读纯文本 / Markdown 类文本内容
- 保存每本书的阅读进度、字体、字号、行距、文字颜色和背景样式
- 支持导入本地 `.ttf` / `.otf` 字体用于阅读器显示

## 安全边界

Visto 当前按只读应用设计，不提供这些远端写操作：

- 不上传文件
- 不删除远端文件
- 不移动文件
- 不重命名文件
- 不创建远端目录
- 不修改 WebDAV 服务器内容

也就是说，Visto 只读取 WebDAV 内容，用于浏览、查看和阅读。应用保存的阅读进度、显示设置和缓存都位于本机。

## 隐私与本地数据

本 README 不包含 WebDAV 地址、用户名、密码、令牌、密钥、本机路径或其他敏感信息。应用中的账号凭据、阅读记录、显示偏好和缓存数据仅用于本机功能。

## 下载

当前推荐 APK 在 GitHub Release：

- https://github.com/Vivitoto/Visto/releases/tag/latest

请下载最新版本的 `visto-*.apk`。

## 构建

本地构建需要：

- Java 17
- Android SDK
- Gradle Wrapper

常用命令：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

正式 APK 由 GitHub Actions 构建并签名。

## 项目文档

设计和实现计划：

- `docs/plans/2026-06-03-visto-design.md`
- `docs/plans/2026-06-03-visto-tasks.md`
- `docs/plans/2026-06-04-visto-original-load-and-webdav-paths.md`
