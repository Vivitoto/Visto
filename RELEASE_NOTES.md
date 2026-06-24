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
