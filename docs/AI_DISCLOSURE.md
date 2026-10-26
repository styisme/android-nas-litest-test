# AI 生成声明

本项目（NAS Min / Android NAS Lite 实验版）是剥离了完整版所有扩展模块与复杂安全机制的**极限裁剪实验**。

为了开源协作的透明度，特此披露 AI 在本项目中的参与程度：**本仓库 99% 以上的代码（包含 Java、HTML、CSS、CI 脚本）均由大语言模型（LLM）直接输出。** 人类开发者仅作为“产品经理”提出了极限精简的需求，并负责真机测试与发版决策。

---

## AI 参与的具体部分

### 1. 核心业务代码 (6 个 Java 类)
- `NasHttpServer.java`：NanoHTTPD 路由封装、基础文件流转 API（列表/上传/下载/新建/重命名/删除/搜索/解压）、路径沙箱（`safeFile`）。
- `MainActivity.java`：本地控制中心（服务启停、IP 展示、修改密码）。
- `HttpServerService.java`：前台服务常驻、`WakeLock` / `WifiLock` 极简保活。
- `AuthManager.java`：基于 Basic Auth 的双账号鉴权与 `ThreadLocal` 并发隔离。
- `DatabaseManager.java`：SQLite 账号存储与 SHA-256 加盐哈希。
- `BootReceiver.java`：开机自启广播。

### 2. 极简 Web 前端 (1 个 HTML)
- `index.html`：单文件纯原生 HTML/CSS/JS，无任何第三方 CDN、无 PWA、无 Service Worker。仅保留基础文件列表、上传、下载、文本预览/编辑、删除等核心交互。

### 3. 构建配置与 CI
- 根目录与 `app/` 下的 Gradle 配置、Manifest 清单。
- `.github/workflows/build.yml`：单 Job 的极简 CI 流水线，仅负责编译 Debug APK 并上传产物。

---

## 开发者（人类）负责的核心工作

- **产品定义与极限裁剪**：决定将原本多端、多防线的项目直接砍到只剩 6 个类和 1 个 HTML，只保留纯文件流转能力。
- **编译与执行**：在 Termux / Pydroid 环境下执行生成脚本，使用 Mgit 提交并推送。
- **真机测试**：验证上传/下载、目录浏览、`../` 越权拦截、`admin` / `guest` 权限隔离是否生效。
- **安全审查**：确认 `safeFile()` 已强制剥离前导斜杠（防全站 404 / 沙箱逃逸），确认 `WRITE_EXTERNAL_STORAGE` 适配了 API 8。

---

## 已知的局限性

1. **无现代安全防线**：移除了 2FA、隐藏暗号、算数验证码、防 CC 限流、IP 白名单。仅靠默认的 Basic Auth 与加盐哈希保护。
2. **无高级传输**：移除了大文件秒传、分片续传、Range 流媒体、多选 ZIP 打包下载，仅保留基础的流式 PUT 上传与单文件下载。
3. **AI 易犯的经典错误**：AI 生成极简代码时，极易漏掉 `getCanonicalPath` 的防穿越校验，或写错 `new File(parent, child)` 的绝对路径拼接规则。人类在真机测试时曾发现 `safeFile()` 前导斜杠未剥离导致的全站 404 回归，已在当前归档代码中修复。
4. **CI 编译通过 ≠ 功能正确**：由于没有单元测试，CI 仅能拦截语法错误，所有文件流转功能必须依靠人类在物理设备上实测。

---

## 归档声明

本项目为**实验性归档项目**，已完成“旧手机变文件服务器”的极限裁剪验证。代码将保持现状，**不再接受任何新功能、安全加固或 Bug 修复**。

---

## 协议

本项目遵循 MIT 协议。