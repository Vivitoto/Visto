# Visto 原图加载与 WebDAV 特殊路径优化计划

## 范围

用户指定先做：

1. 原图加载体验优化：文件大小、加载中、失败提示。
2. WebDAV 特殊路径测试：中文、空格、`+`、`#`、`%`、带 path 的 base URL。

暂不做：Gradle/CI 验证、推送、Release/APK 更新、上传或任何外部发布。

## 设计

### 原图加载 UI

- 默认仍然不自动下载原图，保持流量安全。
- 未加载状态显示：文件名、`加载原图 · 文件大小` 按钮、轻量说明。
- 加载中显示明确文案：`正在加载原图…`。
- 加载失败显示明确文案：`原图加载失败`、文件名/大小、`重试`按钮。
- 手动加载状态按 item.path 保留，左右滑回来不重复回到未加载页。

### WebDAV 路径测试

- 单元测试覆盖：
  - href normalizer 对 `%23`、`%25`、中文、空格、`+` 的解码；
  - encoded base path stripping；
  - PROPFIND / media GET 对 decoded path segment 的安全编码；
  - base URL 自身带 path segment 时的拼接。

## 验证

- 新增/调整 Kotlin 单元测试。
- 本地至少跑：`./local-check.sh`、`git diff --check`。
- 若本机仍无 Java，则只记录 Gradle 单元测试跳过，后续再补 CI/JDK 验证。
