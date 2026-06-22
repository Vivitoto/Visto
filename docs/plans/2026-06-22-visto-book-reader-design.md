# Visto WebDAV 小说阅读 — 设计文档

Date: 2026-06-22
Status: Confirmed by Vito

## 1. 目标

在现有 Visto WebDAV 只读浏览器中增加小说阅读能力。用户连接 WebDAV 后，不仅能浏览图片/视频，还能阅读 TXT 小说（后续支持 EPUB）。

## 2. 导航变更

当前底部导航：`相册 | 浏览 | 设置`

新布局：`相册 | 书架 | 浏览 | 设置`

`HomeTab` 枚举加 `BOOKSHELF`。图标用 Material Icons `Book`。

## 3. 文件类型扩展

`MediaType` 枚举新增：

- `TEXT_BOOK` — 纯文本：`.txt`, `.md`
- `EPUB_BOOK` — EPUB 压缩包（v1 只标记类型，不实现阅读器）

`MediaTypeDetector` 加扩展名映射。

浏览器分组调整为三个 section：文件夹 / 媒体 / 书籍。

## 4. 数据层

### 4.1 新增 Entity：`BookProgressEntity`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, autoGenerate) | 自增 |
| accountId | Long | FK → dav_account.id |
| path | String | WebDAV 文件路径 |
| name | String | 文件名（显示用） |
| sizeBytes | Long? | 最后一次阅读时的文件大小 |
| etag | String? | 最后一次阅读时的 ETag |
| encoding | String | 文本编码（如 "UTF-8"） |
| chapterIndex | Int | 当前阅读的章节序号（0-based） |
| chapterTitle | String? | 当前章节标题 |
| pageOffset | Int | 当前章节内页码（0-based） |
| totalChapters | Int | 章节总数 |
| fontSizeSp | Int | 字号（sp，默认 18） |
| lineSpacing | Float | 行距倍率（默认 1.5） |
| theme | String | "light" / "dark" / "cream" |
| lastReadAt | Long | 最后阅读时间（epoch ms） |
| addedAt | Long | 加入书架时间 |

索引：`(accountId, path)` unique

### 4.2 新增 DAO：`BookProgressDao`

- `upsert(entity)` — 插入或替换
- `getByPath(accountId, path): BookProgressEntity?`
- `getAllByAccount(accountId): Flow<List<BookProgressEntity>>` — lastReadAt 倒序
- `delete(accountId, path)` — 从书架移除
- `deleteByAccount(accountId)` — 切换账号清空

### 4.3 DB Migration v2 → v3

创建 `book_progress` 表 + 索引。

## 5. 核心库

新增包：`core/book/`

### 5.1 TextEncodingDetector

职责：从 `ByteArray` 开头（前 4KB）探测文本编码。

策略：
1. 先查 BOM（UTF-8: EF BB BF, UTF-16 LE/BE: FF FE / FE FF）
2. 若无 BOM，按字节特征判断 GBK/GB18030（中文字节范围 0x81-0xFE + 0x40-0xFE）
3. 默认回退 UTF-8

不引入外部编码检测库（jchardet/juniversalchardet），手写轻量检测器覆盖常见中文编码，降低依赖复杂度。

### 5.2 ChapterParser

职责：从完整文本中识别章节边界。

```kotlin
data class Chapter(
    val index: Int,
    val title: String,
    val startOffset: Int,    // 字符起始位置
    val endOffset: Int,      // 字符结束位置（不含下一章）
)
```

匹配规则（v1）：
- `第[零一二三四五六七八九十百千万0-9]+[章节回卷]`
- `Chapter\s+\d+`（不区分大小写）
- 允许标题后续跟随任意文字

未匹配到任何章节时返回 1 个默认 Chapter `全文`（startOffset=0, endOffset=text.length）。

### 5.3 TextPaginator

职责：将给定章节的文本字串按屏幕尺寸、字号、行距分页。

```kotlin
data class Page(
    val startChar: Int,
    val endChar: Int,
    val text: String,
)

fun paginate(
    text: String,
    maxWidthPx: Float,
    maxHeightPx: Float,
    fontSizeSp: Float,
    lineSpacing: Float,
    density: Float,
): List<Page>
```

使用 `android.graphics.Paint` 计算文本分行分页。不画 UI，只返回 Page 列表。

### 5.4 BookTextLoader

职责：从 WebDAV GET → 字节数组 → 编码检测 → 字符串 → 缓存到本地文件。

```kotlin
suspend fun load(
    webDavClient: WebDavClient,
    path: String,
    cacheDir: File,
): BookTextResult

data class BookTextResult(
    val text: String,
    val encoding: String,
    val sizeBytes: Long,
    val cachedFile: File,  // 本地缓存路径
)
```

## 6. 阅读器 UI

新增包：`ui/reader/`

### 6.1 ReaderScreen

主阅读器页面，全屏展示。

布局：
- 顶部状态栏（轻触显示/隐藏）：章节标题
- 正文区域：当前页文本，Canvas 或 Text 渲染
- 底部进度栏（轻触显示/隐藏）：`当前章节 23/142 页`
- 底部工具栏：目录按钮 / 设置按钮 / 返回按钮

交互：
- **左右滑动**：翻页（HorizontalPager 或手势检测）
- **中央点击**：切换工具栏可见性
- **边缘点击**：上一页 / 下一页
- **返回按钮** → 自动保存进度 → 回到上一个页面

### 6.2 ReaderSession

```kotlin
data class ReaderSession(
    val filePath: String,
    val fileName: String,
    val encoding: String,
    val fullText: String,
    val chapters: List<Chapter>,
    val currentChapterIndex: Int,
    val currentPage: Int,
    val pagesForCurrentChapter: List<Page>,
    val fontSizeSp: Int,
    val lineSpacing: Float,
    val theme: ReaderTheme,
    val isLoading: Boolean,
    val errorMessage: String?,
)
```

reducer 模式管理状态（与现有 AlbumDetailReducer 一致）。

### 6.3 ReaderSettingsSheet

Material3 ModalBottomSheet，选项：

- 字号：滑块（14sp ~ 28sp）
- 行距：按钮组（紧凑 1.2 / 标准 1.5 / 宽松 2.0）
- 主题：三选一按钮（白天 / 夜间 / 护眼）
- 预览：实时显示效果

### 6.4 ChapterListSheet

Material3 ModalBottomSheet，显示章节列表：

- 当前章节高亮
- 点击跳转
- 标题超长省略号

### 6.5 ReaderTheme

```kotlin
enum class ReaderTheme {
    LIGHT,   // 白底黑字
    DARK,    // 黑底浅灰字
    CREAM,   // 米色底深棕字（护眼）
}
```

## 7. 书架 UI

新增包：`ui/bookshelf/`

### 7.1 BookshelfScreen

书架列表，`LazyColumn`。

每行：书名 + 阅读进度摘要（`第X章 · 已读Y%`）+ 最后阅读时间。

空状态：提示「浏览中打开 TXT 文件后自动加入书架」。

交互：
- 点击 → 进入阅读器，自动恢复到上次阅读位置
- 长按 → BottomSheet：继续阅读 / 从书架移除

数据来源：`BookProgressDao.getAllByAccount(accountId)` Flow。

### 7.2 自动加入规则

- 用户在浏览中点开任意 `.txt` → 自动 upsert 到 book_progress
- 退出阅读器时更新进度
- 从书架移除 = delete 记录 + 清理缓存文件

## 8. 文件缓存策略

### 8.1 缓存位置

App 内部存储：`cacheDir/books/{accountId}/{path_hash}.txt`

### 8.2 清理策略

- 从书架移除时同步删除对应缓存文件
- 「设置 → 清理缓存」按钮清除所有书籍缓存
- 切换/删除 WebDAV 账号时清除该账号所有书籍缓存

### 8.3 重读判断

打开同一本书时：
1. 对比远程 ETag / sizeBytes 是否变化
2. 如果变化 → 重新下载并更新缓存
3. 如果不变 → 直接读本地缓存

## 9. 阅读进度保存策略

退出阅读器时自动保存：
- 当前章节序号
- 当前页序号
- 字号/行距/主题
- 章节总数
- 文件名、文件路径

打开已有进度的书时：
- 从缓存读文本（或重新下载）
- 跳转到上次章节 + 页码
- 恢复字号/行距/主题

## 10. 浏览器集成

现有 `BrowserScreen` 打开媒体和打开文件夹的逻辑保持不变。新增：

- `BrowserStateBuilder` 增加 `books` 分组（`mediaType in [TEXT_BOOK, EPUB_BOOK]` 且非目录）
- `BrowserScreen` 增加「书籍」section header + book rows
- 点书籍 → 进入 ReaderScreen（不是 ViewerScreen）

## 11. MainActivity 集成

- `selectedTab` 默认逻辑支持 `BOOKSHELF`
- 未添加账号时 `BOOKSHELF` → 显示 `NoAccountHome`
- 有账号时 `BOOKSHELF` → `BookshelfHost`

## 12. 依赖变更

不新增第三方依赖。所有功能基于现有栈实现：

- 编码检测：手写（基于字节范围判断）
- 文本分页：`android.graphics.Paint.breakText()` + `measureText()`
- 章节解析：手写正则
- UI：现有 Compose Material3

## 13. 非目标（v1 不做）

- EPUB 阅读器（只标记类型）
- 搜索全文 / 搜索章节
- 书签功能
- 语音朗读（TTS）
- 字体选择（使用系统默认）
- 漫画 / PDF 支持
- 书源 / 在线下载
- 竖排文字
- 双页模式

## 14. UI 风格约定

所有新增 UI 保持与现有 Visto 一致：
- Material3 主题，遵循 `VistoTheme`
- 行高、间距、边距与现有 Screen 对齐
- 空状态、加载状态、错误状态使用现有模式
- BottomSheet 使用 `ModalBottomSheet`
- 阅读器页面独立主题（阅读场景独立配色，不影响 App 全局主题）
