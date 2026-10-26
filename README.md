# Android NAS Lite (Experimental)

> ⚠️ **警告：本项目为实验性归档项目。作者已完成极限裁剪实验，后续不再进行任何功能更新、Bug 修复或维护。**

> 🤖 **AI 生成声明**：本仓库 99% 以上的代码（包含 Java、HTML、CSS、CI 脚本）均由大语言模型（LLM）直接生成，人类开发者仅负责提出“极限裁剪”需求、真机测试与发版决策。完整声明请参阅 [`docs/AI_DISCLOSURE.md`](docs/AI_DISCLOSURE.md)。

这个仓库是 [Android NAS](https://github.com/styisme/android-nas) 的**极限精简实验**。

为了测试“一个局域网文件服务器到底能精简到什么程度”，我剥离了完整版中 99% 的代码、多端生态与安全防线，仅保留了最核心的 HTTP 文件流转能力。

## 🎯 实验目标与结果

- **代码体积**：仅保留 6 个核心 Java 类 + 1 个单文件 `index.html`。
- **外部依赖**：仅保留 1 个（`nanohttpd 2.2.0`）。
- **网络端口**：仅监听 1 个（`8080`）。
- **前端资源**：纯原生 HTML/CSS/JS，0 外部 CDN 请求。

---

## ✅ 仅保留的能力

这 6 个类实现了以下功能：
- **纯文件流转**：列表、上传、下载、新建文件夹、重命名、删除。
- **文本处理**：文本在线预览与编辑（限制 2MB）、ZIP 在线解压。
- **基础检索**：全局搜索（深度 4，上限 200）。
- **鉴权**：`admin` / `guest` 双账号 Basic Auth（密码加盐哈希），`guest` 被锁定在 `/Public`。
- **极简前端**：单页 Web 端，无 PWA、无 Service Worker。
- **基础保活**：前台服务 + `WakeLock` + `WifiLock` + 开机自启。

---

## ❌ 绝对不包含的功能

作为极简实验，完整版（及以前的 Lite 版）中的以下能力已被**彻底物理删除**，且**永远不会有**：
- 手机客户端 / TV 端 / 桌面端 / 小程序 / 手表端。
- WebDAV / FTP / DLNA / UDP 跨端协同发现。
- 2FA 两步验证 / 隐藏暗号 / 算数验证码 / 局域网锁 / 防 CC 限流 / IP 白名单。
- 大文件秒传 / 分片上传 / 多选 ZIP 打包下载。
- Webhook 推送 / 电池温度告警 / 审计日志 / 端口大盘 / 测速。
- 远程控制 / Root 全盘 / Web Shell / 终极自毁 / 短信中心。

---

## 🛠 构建方式

### 方式一：GitHub Actions 自动构建
本仓库保留了唯一的一个 CI 流水线（`.github/workflows/build.yml`）。
Push 代码到 `main` 或 `master` 分支后，Actions 会自动构建并上传 `nasmin-server` 产物（即 Debug 签名的 APK）。

### 方式二：本地源码编译
```bash
chmod +x gradlew
./gradlew assembleDebug --no-daemon
```
产物位置：`app/build/outputs/apk/debug/app-debug.apk`
*编译环境：JDK 8 + Android SDK (compileSdk 30 / buildTools 30.0.3)。*

---

## 📖 文档说明

由于项目已归档，这里仅保留了最基础的说明文档，均已调整为**实验环境**的口吻：
- 📄 **[docs/AI_DISCLOSURE.md](docs/AI_DISCLOSURE.md)**：实验代码的 AI 参与比例与开发局限性声明。
- 📄 **[docs/DEVELOPMENT_CONSTRAINTS.md](docs/DEVELOPMENT_CONSTRAINTS.md)**：本实验项目的物理限制、无法做到的测试、以及敏感模块清单。
- 📄 **[docs/API_DOC.md](docs/API_DOC.md)**：当前极简版实际仅存的 8080 端口 HTTP API 规范。

---

## ⚠️ 安全警告

因为是实验项目，**请不要将其用于任何敏感数据或公网环境**：
1. **明文传输**：只支持 HTTP，没有 HTTPS。
2. **无防爆破**：默认密码为 `admin / admin`，无任何防暴力破解或限流机制。
3. **无越权保护外的防护**：虽然有路径沙箱（前导斜杠剥离 + `getCanonicalPath` 校验），但没有 2FA 等现代安全措施。

---

## 协议

本项目遵循 [MIT License](LICENSE)。