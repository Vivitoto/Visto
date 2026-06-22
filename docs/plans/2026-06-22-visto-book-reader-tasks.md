# Visto WebDAV 小说阅读 — 实现计划

Date: 2026-06-22
Design: `docs/plans/2026-06-22-visto-book-reader-design.md`

## 执行约定

- 每个 task 2-5 分钟：写测试 → 看失败 → 实现 → 看通过 → 提交
- TDD 强制：禁止先写实现代码
- 验证命令：`cd Visto && ./gradlew testDebugUnitTest`
- 编译验证：`./gradlew assembleDebug`

---

## Phase 0 — 数据层（DB + Entity + DAO）

### Task 0.1 — 新增 BookProgressEntity

**Test:** 创建实体、检查字段/注解/索引/外键与设计一致。

**Implementation:**
- 创建 `data/db/BookProgressEntity.kt`
- 表名 `book_progress`
- FK → `dav_account.id` ON DELETE CASCADE
- 唯一索引 `(accountId, path)`

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 0.2 — 新增 DB Migration v2→v3

**Test:** 用 Room `MigrationTestHelper` 验证 migration v2→v3 后 `book_progress` 表存在。

**Implementation:**
- `VistoMigrations.kt` 加 `MIGRATION_2_3`
- `VistoDatabase.version` → 3
- `VistoDatabase.create()` 注册新 migration

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 0.3 — 新增 BookProgressDao

**Test:** 用 Room in-memory DB 测：upsert、getByPath、getAllByAccount（Flow）、delete、deleteByAccount。

**Implementation:**
- 创建 `data/db/BookProgressDao.kt`
- 5 个方法

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 0.4 — 注册 DAO 到 VistoDatabase

**Test:** 验证 `VistoDatabase` 可以解析 `BookProgressDao` 的类型。

**Implementation:**
- `VistoDatabase` 加 `abstract fun bookProgressDao(): BookProgressDao`

**Verify:** `./gradlew testDebugUnitTest`

---

## Phase 1 — 核心库（编码 / 章节 / 分页 / 加载）

### Task 1.1 — TextEncodingDetector

**Test:**
- UTF-8 with BOM → UTF-8
- GBK without BOM → GBK
- UTF-8 without BOM → UTF-8
- Pure ASCII → UTF-8
- 空字节 → UTF-8

**Implementation:**
- 创建 `core/book/TextEncodingDetector.kt`
- BOM 检测 + 字节范围判断 GBK/GB18030
- 不引入外部库

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 1.2 — ChapterParser

**Test:**
- `第1章 开端` → 1 chapter
- `第一章 开端\n第二章 进入` → 2 chapters
- `第零章`/`第二百三十四回`/`第3节` → 正确识别
- `Chapter 1\nChapter 2` → 按 Chapter \d+ 匹配
- 无章节标记文本 → 1 个默认 Chapter `全文`

**Implementation:**
- 创建 `core/book/ChapterParser.kt`
- `data class Chapter(index, title, startOffset, endOffset)`
- 正则：`第[零一二三四五六七八九十百千万\\d]+[章节回卷][^\n]*` + `Chapter\\s+\\d+[^\n]*`

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 1.3 — TextPaginator

**Test:**
- 空文本 → 1 个空页
- 短文本（一行宽） → 1 页
- 长文本 → 多页，每页字符不重叠
- 字号增大 → 页数增多
- 行距变化 → 页数变化

**Implementation:**
- 创建 `core/book/TextPaginator.kt`
- `data class Page(startChar, endChar, text)`
- 用 `android.graphics.Paint` 做分行分页计算
- `fun paginate(text, maxWidth, maxHeight, fontSize, lineSpacing, density): List<Page>`

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 1.4 — BookTextLoader

**Test:**
- 模拟 WebDAV GET 返回 UTF-8 文本 → 正确加载 + 缓存
- 模拟 GBK 文本 → 编码检测 → 正确解码
- 已有缓存且 ETag 不变 → 跳过下载
- 已有缓存但 ETag 变了 → 重新下载
- 本地缓存文件存在且可读

**Implementation:**
- 创建 `core/book/BookTextLoader.kt`
- `data class BookTextResult(text, encoding, sizeBytes, cachedFile)`
- `suspend fun load(webDavClient, path, cacheDir): BookTextResult`
- 缓存目录：`cacheDir/books/{path_hash}.txt`

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 1.5 — 扩展 MediaType

**Test:**
- `.txt` → TEXT_BOOK
- `.md` → TEXT_BOOK
- `.epub` → EPUB_BOOK
- `text/plain` MIME → TEXT_BOOK
- `application/epub+zip` MIME → EPUB_BOOK
- `.jpg` 不受影响 → IMAGE

**Implementation:**
- `MediaType` 加 `TEXT_BOOK`, `EPUB_BOOK`
- `MediaTypeDetector` 加扩展名和 MIME 映射

**Verify:** `./gradlew testDebugUnitTest`

---

## Phase 2 — 阅读器 UI

### Task 2.1 — ReaderSession 数据模型

**Test:**
- 构造合法 session
- reducer: 修改字号 → pagesForCurrentChapter 重新计算
- reducer: 切换章节 → currentPage 重置为 0
- reducer: 翻页 → currentPage 递增/递减
- reducer: 切换主题 → theme 更新

**Implementation:**
- 创建 `ui/reader/ReaderSession.kt`
- `data class ReaderSession` + `ReaderSessionReducer`
- 与现有 `AlbumDetailReducer` 风格一致

**Verify:** `./gradlew testDebugUnitTest`

---

### Task 2.2 — ReaderScreen

**Test:** 不写 UI 单元测试（Compose UI 用真机验证）。

**Implementation:**
- 创建 `ui/reader/ReaderScreen.kt`
- 全屏阅读界面
- 左右滑动手势翻页
- 中央点击切换工具栏
- 章节标题区域
- 底部进度栏：`23 / 142`
- 底部工具栏：目录 / 设置
- 状态：loading / error / content / empty
- 主题：LIGHT / DARK / CREAM 三套配色

**Verify:** `./gradlew assembleDebug`

---

### Task 2.3 — ReaderSettingsSheet

**Implementation:**
- 创建 `ui/reader/ReaderSettingsSheet.kt`
- Material3 ModalBottomSheet
- 字号滑块：14~28sp
- 行距按钮组：紧凑/标准/宽松
- 主题三选一

**Verify:** `./gradlew assembleDebug`

---

### Task 2.4 — ChapterListSheet

**Implementation:**
- 创建 `ui/reader/ChapterListSheet.kt`
- Material3 ModalBottomSheet
- 章节列表：当前章节高亮
- 点击跳转

**Verify:** `./gradlew assembleDebug`

---

### Task 2.5 — ReaderTheme 定义

**Implementation:**
- 创建 `ui/reader/ReaderTheme.kt`
- `enum class ReaderTheme { LIGHT, DARK, CREAM }`
- 每项：背景色、文字色、工具栏色

**Verify:** `./gradlew testDebugUnitTest`

---

## Phase 3 — 书架 UI

### Task 3.1 — BookshelfScreen

**Implementation:**
- 创建 `ui/bookshelf/BookshelfScreen.kt`
- `ui/bookshelf/BookshelfUiState.kt`
- LazyColumn 列表
- 每行：书名 + `第X章 · 已读Y%` + 时间
- 空状态提示
- 长按 BottomSheet：继续阅读 / 从书架移除

**Verify:** `./gradlew assembleDebug`

---

## Phase 4 — 集成

### Task 4.1 — 导航 + MainActivity 集成

**Implementation:**
- `HomeTab` 加 `BOOKSHELF`
- `VistoBottomBar` 加第 4 个 tab（Book 图标）
- `VistoRoot` / `MainActivity` 处理 BOOKSHELF 路由
- 未配置账号 → NoAccountHome
- 有账号 → BookshelfHost

**Verify:** `./gradlew assembleDebug`

---

### Task 4.2 — Browser 集成

**Implementation:**
- `BrowserStateBuilder` 增加 `books` 分组
- `BrowserScreen` 增加书籍 section
- 点击书籍 → 调用 BookTextLoader → 进入 ReaderScreen
- 退出阅读器 → 保存进度 → 书架自动添加

**Verify:** `./gradlew assembleDebug`

---

### Task 4.3 — 设置页缓存清理

**Implementation:**
- SettingsScreen / VistoApplication 加「清理书籍缓存」
- 删除 `cacheDir/books/` 目录下所有文件

**Verify:** `./gradlew assembleDebug`

---

## Phase 5 — 自检与收尾

### Task 5.1 — 全量测试 + 编译自检

```bash
cd Visto
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

修复所有编译和测试问题。

---

### Task 5.2 — 手动 checklist

- 添加 WebDAV 账号 → 浏览文件夹 → 找到 .txt
- 打开 .txt → 编码正确显示
- 章节识别 → 目录可用
- 翻页 → 左右滑动 / 点击
- 字号/行距/主题 → 设置生效
- 退出 → 进度保存
- 书架 → 显示/点击恢复/删除
- 缓存 → 设置页清理→ re-open 重新下载
