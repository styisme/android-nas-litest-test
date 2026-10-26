# 开发环境约束与责任边界说明

本文档说明本仓库（NAS Min / Android NAS Lite 实验版）的实际开发条件与责任边界。按实际情况勾选，不美化，不假装完成了未执行的流程。

> **📌 本文件在根目录与 `docs/` 目录各存一份，内容完全一致。**

---

## 一、开发环境（物理限制）

- [ ] 有本地 IDE / SDK，可本地编译
- [ ] 有本地 diff 工具，可逐行审阅
- [ ] 可跑 lint / 单元测试
- [ ] 走标准 PR 流程（fork → PR → review）
- [x] **仅靠云端 CI 编译（无本地工具链）**
- [x] **仅靠移动端开发（Termux + Pydroid + Mgit）**
- [x] **走直推 master（无 PR 流程）**

> 未勾选的条目，代表**本仓库物理上无法执行**，不是"忽略了"。

---

## 二、实际执行的机制

### 披露与归档
- [x] 提供 `docs/AI_DISCLOSURE.md`，说明 AI 参与范围与比例。
- [x] 明确声明本项目为**实验性归档项目**，不再跟进新功能与安全补丁。

### 极简裁剪
- [x] 已将完整版的所有高危功能、扩展模块、多端代码**物理删除**（非开关隐藏）。
- [x] 仅保留最基础的 `Basic Auth` 鉴权与 `safeFile` 路径沙箱。
- [x] 删除了所有复杂的现代安全防线（无 2FA、无防爆破、无隐藏暗号、无验证码）。

### 门禁与测试
- [x] **CI 编译通过才发版**（GitHub Actions `build.yml` 是唯一的自动化门禁）。
- [x] **真机测试**（在物理设备上验证文件上传/下载、目录浏览、越权拦截）。

> 未勾选的条目，代表**本仓库没有条件做**，不是"忘了做"。

---

## 三、诚实声明与教训

1. **AI 生成的极简代码极易漏掉安全校验**：在极限裁剪时，AI 往往会专注于“把路由改通”，从而不小心删掉 `safeFile()` 的前导斜杠剥离（`replaceAll("^/+", "")`），导致 `new File(rootDir, path)` 引发全站 404 或路径穿越。人类必须在真机上特别验证 `../` 越权是否被拦截。
2. **CI 编译通过 ≠ 功能正确**：极简版没有任何单元测试，CI 只能拦截语法和依赖缺失，无法拦截运行时的路径越界或逻辑错误。
3. **没有前端外部依赖**：`index.html` 是纯手写/单文件生成的，这意味着没有 CDN 带来的兼容性风险，但也意味着前端交互完全依赖原生 JS 的稳定性。

---

## 四、敏感模块清单

按当前极简代码中实际存在的模块列出：

| 模块 | 验证状态 | 说明 |
|---|---|---|
| `NasHttpServer.safeFile()` | ✅ 真机测试 | 核心路径沙箱。已确认强制剥离前导斜杠 + `getCanonicalPath` 校验。 |
| `AuthManager.java` | ✅ 真机测试 | 基础鉴权与 `ThreadLocal` 并发隔离，无复杂安全防线。 |
| `DatabaseManager.hash()` | ✅ 真机测试 | SHA-256 + 随机盐密码哈希。 |
| `DatabaseManager.getUserRoot()` | ✅ 真机测试 | 账号根目录映射，确认 `guest` 被锁死 `/Public`。 |
| `.github/workflows/build.yml` | ✅ CI 验证通过 | 极简单 Job CI，仅负责编译 Debug APK。 |

> **已移除模块（不在本仓库中）**：
> `RemoteDesktopManager`、`AdvancedKeepAliveManager`、`MediaScraper`、`ThumbnailManager`、`SystemMonitor`、`DlnaServer`、`LocalSendServer`、`FreeTunnelManager`、`SshTunnelManager`、`SilentPlayer`、`AlarmReceiver`、`OnePixelActivity`、`RootManager`、`NasAccessibilityService`、`SmsManager`、`TotpManager`、`WebDavServer`、`FtpServer` 等。

---

## 五、我们承认这不完美

形式化的 PR 流程、逐行代码审阅、本地静态分析，这些在本仓库中**全部缺位**。
我们用「**诚实披露 + CI 门禁 + 物理删除 + 真机验证**」承担了实质责任。
作为归档实验，它完成了“极限精简”的使命，但不应被用于任何严肃的生产环境。

---

## 六、给极限开发者的建议

如果你也在极端约束下做极限裁剪，这套最小责任清单供参考：
1. **越简的代码越要测越权**（`../` 和绝对路径拼接是 AI 最常翻车的地方）。
2. **能物理删除就不要用开关隐藏**（开关会留在一堆死代码和无用逻辑）。
3. **诚实披露 AI 参与度**，不要假装是手写的。
4. **不要给归档项目打补丁**，确认实验目标达成后直接封存。