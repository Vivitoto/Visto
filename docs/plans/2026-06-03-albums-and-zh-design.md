# 2026-06-03 Visto 相册 + 中文化设计

## 背景

v0.1 是一个目录浏览器：打开 WebDAV 账号根目录，一层层点进去看。
Vito 希望主入口变成"相册"，每个相册 = 一个 WebDAV 路径快捷方式。
打开相册时，把该路径**递归**下所有媒体拿出来，并按**原始子文件夹**分组显示。
顺便把 UI 整体改成中文。

## 边界

继续遵守 v0.1 只读约束：

- 不上传、不删除、不重命名、不移动
- 不写 EXIF、不建时间线、不聚合相似度
- 不动 WebDAV 上任何文件

相册功能：

- 单 WebDAV 账号
- 每个相册：相册名 + 一个 WebDAV 路径
- 进入相册：递归遍历该路径下所有文件，按子文件夹分组
- 仍然支持点开图片/视频进入查看器
- 仍然只读

## 数据模型

新增 Room 实体 `album_source`：

```kotlin
@Entity(
    tableName = "album_source",
    foreignKeys = [ForeignKey(entity = DavAccountEntity, parentColumns=["id"], childColumns=["accountId"], onDelete=CASCADE)],
    indices = [Index(["accountId", "rootPath"], unique=true)],
)
data class AlbumSourceEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val accountId: Long,
    val displayName: String,    // 用户取的相册名
    val rootPath: String,       // WebDAV 上的根路径，例如 /Photos/Family
    val createdAt: Long,
    val updatedAt: Long,
)
```

Room version bump 1 → 2，迁移用 `fallbackToDestructiveMigration`（v0.1 阶段无生产数据）。

## UI 层级

```
[相册列表页]  ← 启动首屏
   │
   ├─ 添加相册（弹窗：相册名 + WebDAV 路径选择/输入）
   │
   ├─ 点相册 → [相册详情页（分组媒体）]
   │             │
   │             ├─ 点媒体 → [查看器]
   │             └─ 返回相册列表
   │
   └─ 设置 → [设置页]（账号信息 + 缓存清理 + 切到旧版"目录浏览"开关）
```

旧的 `BrowserScreen` 作为"目录浏览"模式保留，从设置进入。
首屏不再是 `BrowserScreen`，而是新的 `AlbumListScreen`。

## 关键算法：递归收集 + 分组

`AlbumLoader.load(client, accountId, rootPath)`：

1. BFS 遍历 `rootPath` 下所有目录，每个目录做一次 `PROPFIND depth:1`
2. 限制最大深度 8 防止爆炸
3. 把遇到的 **媒体文件**（image / animated / video）收集起来
4. 按 `file.parentPath` 分组成 `List<AlbumSection>`
5. `AlbumSection.title` = `parentPath` 相对 `rootPath` 的相对路径
   - `rootPath` 本身的文件 → 分组标题 = `/`（或 UI 显示"根目录"）
   - 子目录文件 → 标题如 `2024/Trip`

服务器响应慢/限制并发的情况：

- 并发限制：用协程 channel + 4 个 worker，避免一次性打开 100+ 个 PROPFIND
- 渐进式输出：每完成一个目录就 emit 一次进度 + 当前已知 sections，UI 可以边加载边出
- 出错不致命：单个目录失败只跳过该目录，把错误累加到 `AlbumUiState.warnings`

## UI 详情

**相册列表页**

- 顶部：`相册`
- 右上：`+`（添加相册）、`⚙`（设置）
- 列表项：
  - 左：相册名（粗体）
  - 下：路径（小字灰色）
  - 长按：删除（带确认）
- 空状态：
  - "还没有相册"
  - "添加 WebDAV 上的路径，开始浏览"
  - 大号 `+ 添加相册` 按钮

**添加相册弹窗**

- 输入：相册名（默认填路径最后一段）
- 输入：WebDAV 路径
  - 旁边一个"浏览…"按钮 → 打开一个简化的目录选择器（基于现有 BrowserScreen，但只能选文件夹）
- 保存按钮：写 Room
- 校验：路径必须以 `/` 开头；路径需 PROPFIND 一次确保存在

**相册详情页**

- 顶部：相册名 + 返回 + 刷新
- 主体：`LazyColumn`，每段：
  - 段标题：分组名（`根目录` / 相对子路径）+ 该段媒体数量
  - 媒体网格（用现有 `MediaTile`）
- 加载：顶部进度条，"已加载 N 个文件夹"
- 错误：底部一行警告气泡，"3 个文件夹访问失败，已跳过"
- 媒体顺序：先按段（按子路径字母序），段内按文件名

**查看器**

复用现有 `ViewerScreen`。打开时把当前相册全部媒体扁平化成一个列表传进去，确保左右滑能跨段切换。

**设置页**

新增段：

- `浏览模式`
  - `相册`（默认）
  - `按目录浏览（旧版）` —— 进去就是当前 `BrowserScreen`
- 账号信息、清缓存保留

## 中文化

把所有硬编码英文换成中文（先不上 strings.xml，保持简单）：

- `Connect a WebDAV account` → `连接 WebDAV 账号`
- `Display name` → `显示名称`
- `Server URL (https://...)` → `服务器地址 (https://...)`
- `Username` → `用户名`
- `Password / app token` → `密码 / 应用令牌`
- `Root path` → `根路径`
- `Folders` → `文件夹`
- `Media` → `媒体`
- `Settings` → `设置`
- `Clear local thumbnails` → `清除本地缩略图`
- `This folder is empty.` → `此目录为空`
- `Connection succeeded.` → `连接成功`
- 错误文案 (`AccountErrorMessages`) 全部中文化

## TDD 计划

每个任务 = 写测试 → 跑红 → 实现 → 跑绿 → 提交。

1. **album_source 实体 + DAO**
   - 测试：CRUD（插入、按账号列出、唯一约束、级联删除）
2. **AlbumLoader 递归收集**
   - 测试：用 MockWebServer mock 多层 PROPFIND，断言 sections 分组、文件归属、深度上限、单目录失败不阻塞
3. **AlbumListReducer / AlbumDetailReducer**
   - 纯函数状态：处理加载/进度/错误/添加/删除
4. **中文化文案常量**
   - 测试：断言 `AccountErrorMessages.network()` 返回中文
5. **Compose 屏幕**
   - 通过 CI 编译验证（v0.1 无 instrumented test）
6. **MainActivity 接入**
   - 启动后查询 album_source 数量：>0 → 相册列表页；=0 且无账号 → 账号页；=0 且有账号 → 相册列表页空态

## 不做

- 跨相册去重
- 收藏 / 评分
- 智能封面（用第一张媒体即可）
- 后台预加载
- 任何对 WebDAV 的写操作

## 风险

- **递归遍历慢**：大目录树可能要发几十次 PROPFIND。缓解：并发 4、深度 8、进度反馈、可中断（返回时取消 scope）。
- **PROPFIND Depth: infinity 不可靠**：不少 WebDAV 服务器禁用。我们坚持 Depth: 1 + 自己 BFS。
- **重复刷新**：相册详情每次进入都重新走 BFS。Phase 后续可以加 Room 缓存，v0.2 先不做。
