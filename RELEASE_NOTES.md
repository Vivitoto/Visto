Visto 1.1.18

- 修复 v1.1.17 编译失败：移除不兼容的 ImageDecoder.setDecodeFrame，WebP 保留 WebPDecoder 解码，GIF 使用 ImageDecoder 首帧。
- 下载 30s 超时、编码后 WebP 头校验、GRID 15→10 帧、PREVIEW 保持 60 帧。

## 版本

- 应用版本：1.1.18，Android versionCode 为 39。

---

Visto 1.1.17

- 优雅重构动图缩略图解码管线：GIF 和 WebP 统一使用平台 ImageDecoder（API 31+ 多帧提取，API 28-30 安全首帧），仅 API 26-27 使用 legacy 解码器。
- 消除 webp-android 解码器的 JNI 崩溃面（95%+ 设备不再调用 WebPDecoder）。
- 网格缩略图减帧（15→10），查看页预览保持 60 帧丝滑。
- 下载增加 30s 超时防护，编码后校验 WebP 文件头防损坏缓存。

## 版本

- 应用版本：1.1.17，Android versionCode 为 38。

---

Visto 1.1.16

- 紧急修复 v2：将动图转码固定到专用单线程（newSingleThreadContext），替换 Dispatchers.IO + Semaphore 方案。
- 原因：webp-android JNI 库内部使用线程绑定的 JNIEnv，即使 Semaphore 串行化协程，不同 IO 线程上调用仍会触发 native thread-local 冲突。专用单线程确保所有 JNI 调用发生在同一线程上。
- GIF 解码增加容错：Movie.draw() 在 Android 12+ 上对某些 GIF 可能抛 native 异常，现在捕获并保留已解码的帧继续编码。

## 版本

- 应用版本：1.1.16，Android versionCode 为 37。

---

Visto 1.1.15

- 紧急修复：webp/GIF 动图相册闪退问题，将 JNI 转码改为严格单线程串行，消除 native 层多线程冲突。

## 版本

- 应用版本：1.1.15，Android versionCode 为 36。

---

Visto 1.1.14

- 修复：清空缩略图缓存后打开 webp/GIF 动图相册导致应用卡死闪退的问题。
- 动图缩略图转码增加并发限制（最多 4 个同时运行），帧数上限根据场景调整（相册 15 帧 / 查看器 60 帧）。
- 动图缩略图转码失败时降级为静态缩略图，不再回退原始动图 URL，避免内存抖动。
- 查看器动图预览提升画质：分辨率 1024px、WebP 质量 85%、最多 60 帧。
- 动图查看页恢复「加载原图」功能，autoLoadOriginalImages 开关对动图同样生效。

## 版本

- 应用版本：1.1.14，Android versionCode 为 35。

---

Visto 1.1.13

- 书架页新增视图快速切换下拉菜单，可直接选择列表、标准网格或紧凑网格。
- 相册详情页新增视图快速切换下拉菜单，可直接选择文件夹视图或不同密度的网格视图。
- 保留原有循环切换按钮行为，新增下拉只作为更快的补充入口。

## 版本

- 应用版本：1.1.13，Android versionCode 为 34。

---

Visto 1.1.12

- 新增书籍目录管理：书架右上角进入「书籍目录」，可查看已添加 WebDAV 目录、添加新目录、手动重新扫描、移除目录配置。
- 新增 `book_source` 本地表并升级数据库到 v8，持久化书籍目录来源、上次扫描时间和扫描结果统计。
- 扫描书籍目录时保留已有阅读进度和阅读外观设置，仅更新书名、大小和 etag 等文件元数据。
- 移除书籍目录只删除本地目录配置，不删除书架书籍、阅读进度或 WebDAV 上的文件。
- 书籍目录管理 UI 使用 Visto Material 主题，支持深色模式；长路径最多显示两行并省略截断。
- 阅读器呼出菜单底部按钮加宽，提升点击舒适度。

## 版本

- 应用版本：1.1.12，Android versionCode 为 33。

---

Visto 1.1.11

- 修复阅读器顶部菜单胶囊标题：现在显示书籍主标题，而不是章节标题；标题统一走书架书名逻辑，去除支持的书籍扩展名，并将 `《书名》 副标题/作者` 显示为不带书名号的主标题。
- 修复重新打开书籍时恢复到偏早页面的问题：新增持久化当前页起始字符位置 `pageStartChar`，恢复阅读进度时按文本锚点定位，而不是只按易受分页视口影响的页码索引定位。
- 升级本地数据库到 v7，并添加 `book_progress.pageStartChar` 迁移。

## 版本

- 应用版本：1.1.11，Android versionCode 为 32。

---

Visto 1.1.10

- 修复 CI 编译：将 `kotlin.math.maxOf` 替换为 Dp 原生 `coerceAtLeast`，避免 CI 编译器无法解析 `maxOf` 对 Dp 类型的重载。
- 修复分页器行高计算：使用 `paint.fontSpacing` 替代 `fontMetrics.descent - fontMetrics.ascent`，消除 leading 导致的额外行高误差。
- 更新测试用例以匹配新版默认边距值。

## 版本

- 应用版本：1.1.10，Android versionCode 为 31。

---

Visto 1.1.9

- 重新设计阅读器页边距：滑块从 0 开始，0 表示基准间距（上 12dp / 下 52dp / 左右 8dp），滑块值在基准之上追加。底部基准由胶囊底间隙(12dp) + 胶囊高度(≈28dp) + 正文与胶囊间隙(12dp) 构成。
- 底部胶囊不再凭空浪费大片空白：正文与胶囊间距从 24dp 缩减到 12dp，胶囊贴底从 14dp 缩减到 12dp，整体底部空白大幅收窄。
- 修复分页器行高计算：改用字体自然行高 `fontMetrics.descent - fontMetrics.ascent` 替代 `paint.textSize`，使分页器每行高度与 Compose 显示行高一致，不再出现页底漏放 2–3 行的问题。
- 更新布局和分页回归测试。

## 版本

- 应用版本：1.1.9，Android versionCode 为 30。