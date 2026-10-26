# NAS Min API 接口规范

> 🤖 **AI 生成声明**：本文档由大语言模型生成，服务于实验性归档项目。

本项目为单端极简实验，仅保留 `8080` 一个 HTTP 端口。没有 WebDAV、FTP、UDP 协同或任何多端通讯协议。

---

## 📡 基础约定

- **Base URL**：`http://<ip>:8080`
- **Content-Type**：
  - 常规请求：`application/x-www-form-urlencoded` 或 `multipart/form-data`
  - 文件上传：`application/octet-stream`
- **鉴权方式 (Auth)**：
  - 除首页 `/`（即 `index.html`）外，所有 HTTP 请求必须携带 Header：`Authorization: Basic Base64(账号:密码)`。
  - 系统仅内置 `admin`（默认 `admin`）与 `guest`（默认 `guest`）双账号。
  - 鉴权失败将返回 `401 Unauthorized`（纯文本 `401`）。
- **沙箱隔离**：
  - `guest` 账号的根目录被强制锁定在 `/Public`。
  - `safeFile()` 强制剥离路径前导斜杠（`replaceAll("^/+", "")`），再通过 `getCanonicalPath` 校验，彻底防止 `../` 越权穿越。
- **响应格式**：除下载和纯文本预览外，所有响应均为 JSON 格式。

---

## 📁 一、 文件与目录管理接口

### 1. 获取目录列表
- **URL**: `/api/list`
- **Method**: `GET`
- **Params**: `path` (String, 必填) - 目标目录的路径，如 `/` 或 `/Movies`。
- **Response**: `200 OK`
```json
{
  "path": "/Movies/",
  "files": [
    {
      "name": "Inception.mp4",
      "dir": false,
      "size": 1024000
    },
    {
      "name": "Subs",
      "dir": true,
      "size": 4096
    }
  ]
}
```

### 2. 文件下载
- **URL**: `/api/download`
- **Method**: `GET`
- **Params**: `path` (String, 必填) - 文件的绝对路径。
- **Response**: `200 OK`，返回文件流，带有 `Content-Disposition` 触发浏览器下载。

### 3. 文件上传 (流式 PUT)
- **URL**: `/api/upload`
- **Method**: `PUT`
- **Params**: 
  - `path` (String, 必填) - 目标目录路径。
  - `name` (String, 必填) - 上传的文件名。
- **Headers**: `Content-Type: application/octet-stream`，必须携带准确的 `Content-Length`。
- **Body**: 纯二进制文件流。
- **Response**: `200 OK`，返回字符串 `OK`。

### 4. 获取文本预览
- **URL**: `/api/preview`
- **Method**: `GET`
- **Params**: `path` (String, 必填)
- **Response**: `200 OK`，返回纯文本内容（UTF-8 编码）。文件限制 2MB 以内。

### 5. 新建文件夹
- **URL**: `/api/mkdir`
- **Method**: `POST`
- **Body**: `path=/当前目录&name=新目录名`
- **Response**: `200 OK`，返回 `OK` 或 `Fail`。

### 6. 重命名
- **URL**: `/api/rename`
- **Method**: `POST`
- **Body**: `path=/旧路径&newName=新名字`
- **Response**: `200 OK`，返回 `OK` 或 `Fail`。

### 7. 递归删除
- **URL**: `/api/delete`
- **Method**: `POST`
- **Body**: `path=/目标路径`
- **Response**: `200 OK`，返回 `OK` 或 `Fail`。支持递归删除文件夹。

### 8. 在线保存文本
- **URL**: `/api/save`
- **Method**: `POST`
- **Body**: `path=/文件路径&content=新的内容`
- **Response**: `200 OK`，返回 `OK`。覆盖写入文件。

### 9. 在线解压 ZIP
- **URL**: `/api/unzip`
- **Method**: `POST`
- **Body**: `path=/目标zip文件路径.zip`
- **Response**: `200 OK`，返回 `OK` 或 `Fail`。（内置了路径穿越校验，防 Zip Slip 漏洞）

---

## 🔍 二、 搜索接口

### 全局递归搜索
- **URL**: `/api/search`
- **Method**: `GET`
- **Params**: `q` (String, 必填) - 搜索关键字。
- **Response**: `200 OK`，返回包含 `name`, `path`, `dir`, `size` 等属性的 JSON 数组。
- **限制**：最多返回 200 条，递归深度上限 4 层，忽略隐藏目录。

---

## ⚙️ 三、 系统接口

### 1. 系统状态查询
- **URL**: `/api/status`
- **Method**: `GET`
- **Response**: `200 OK`
```json
{
  "running": true,
  "isDefaultPwd": true
}
```
- `isDefaultPwd`：动态查询 SQLite 并比对加盐哈希。若返回 `true`，表示 `admin` 仍在使用默认密码，前端会弹出红条警告。

### 2. 修改管理员密码
- **URL**: `/api/password`
- **Method**: `POST`
- **Body**: `password=新密码`
- **Response**: `200 OK`，返回 `OK`。新密码需至少 4 位。