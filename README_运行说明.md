# 西湖论剑活动平台 · 运行说明

本说明对应本机已验证可运行的配置：**JDK 21 + Maven（IDEA 自带）+ Node 18+ + MySQL 8.0（免安装版）**，不需要 Docker、不需要 Nacos。

## 1. 环境与端口

| 组件 | 版本/位置 | 端口 |
|---|---|---|
| JDK | 21（`C:\Program Files\Java\jdk-21.0.10`） | — |
| Maven | IDEA 自带 `D:\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3` | — |
| Node | 18+（本机 v24） | — |
| MySQL | `D:\xhlj-env\mysql`（8.0.28 免安装） | 3306 |
| 前端 Vite | `sword-shield-web-ui` | 5173 |
| 网关 | `sword-shield-gateway` | 8080 |
| 用户/活动/内容/互动 | 四个业务服务 | 8101–8104 |
| 文件/后台/AI/支付 | 四个业务服务 | 8105–8108 |

> 默认账号：`demo / 123456`，管理员 `admin / admin123`。数据库：`sword_shield`，`root / 123456`。

## 2. 首次安装（只做一次）

```powershell
cd "D:\JAVA EE 课设\_ref_extract\sword_shield_web"

# 解压 MySQL、初始化数据目录、设置 root 密码、创建 sword_shield 库
powershell -ExecutionPolicy Bypass -File scripts\setup-mysql.ps1
```

如果 MySQL zip 不在默认位置：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup-mysql.ps1 -ZipPath "D:\path\to\mysql.zip"
```

## 3. 构建

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-local.ps1
```

- 脚本优先使用 `JAVA_HOME`/系统 Maven，找不到时使用 IDEA 自带 Maven；
- 使用 `scripts/maven-settings.xml`（阿里云镜像）加速依赖下载；
- 产物：每个服务 `target/sword-shield-*-0.1.0-SNAPSHOT-exec.jar`（可执行胖包）。

> 注意：必须用 **JDK 21** 运行。本机 PATH 默认是 JDK 25，Spring Boot 3.2 在 25 上可能启动失败；启动脚本已显式指定 21。

## 4. 启动

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-local.ps1
```

脚本会：

1. 检查 3306，未启动则拉起 MySQL；
2. 依次启动 8 个服务（`--spring.cloud.nacos.discovery.enabled=false`）；
3. 启动网关（`--spring.profiles.active=local`，静态路由到 8101–8108）；
4. 启动前端 `npm run dev`（首次自动 `npm install`）；
5. 输出访问地址。

打开 <http://localhost:5173>。

## 5. 停止

```powershell
# 只停应用（保留 MySQL）
powershell -ExecutionPolicy Bypass -File scripts\stop-local.ps1

# 连 MySQL 一起停
powershell -ExecutionPolicy Bypass -File scripts\stop-local.ps1 -StopMysql
```

手动停止：

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'sword-shield-' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## 6. 配置项

### 6.1 AI 大模型（可选）

默认使用离线规则引擎，不配置也能回答大会问题。要接真实模型：

```powershell
$env:AI_API_KEY="sk-你的Key"
$env:AI_BASE_URL="https://api.deepseek.com"          # 或通义 compatible-mode / 本地 vLLM
$env:AI_MODEL="deepseek-chat"
$env:AI_ENABLED="true"
```

然后重启 AI 服务（或整套服务）。验证：

```bash
curl http://localhost:8080/api/ai/status
# mode=llm 表示已启用真实模型；调用失败会自动降级到 mock
```

### 6.2 文件存储

默认本地磁盘 `uploads/`，零依赖。切 MinIO：

```powershell
$env:MINIO_ENABLED="true"
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_ACCESS_KEY="admin"
$env:MINIO_SECRET_KEY="admin123456"
```

### 6.3 数据库连接

各服务 `application.yml` 默认：

```yaml
url: jdbc:mysql://localhost:3306/sword_shield?...&serverTimezone=Asia/Shanghai
username: root
password: 123456
```

可用环境变量或启动参数覆盖，例如：

```powershell
java -jar ...-exec.jar --spring.datasource.password=你的密码
```

### 6.4 Nacos（可选）

默认关闭，网关走 `application-local.yml` 静态路由。若已启动 Nacos：

```powershell
# 启动服务时不加 --spring.cloud.nacos.discovery.enabled=false
# 网关不用 local profile，使用 application.yml 中的 lb:// 路由
```

## 7. 日志

- 业务日志：`logs/sword-shield-<service>.log`
- 错误日志：`logs/sword-shield-<service>-error.log`
- 每次请求带 `traceId`，可跨服务串联；
- 需要接 ELK 时加 `--spring.profiles.active=logstash`，默认不连接，避免本地刷错误。

## 8. 常见问题

| 现象 | 处理 |
|---|---|
| 启动报 `Unsupported class file major version` | 用了 JDK 25，改用 JDK 21（脚本已指定） |
| `Communications link failure` | MySQL 未启动或 3306 被占用，执行 `setup-mysql.ps1` 或看 `logs/mysql.err.log` |
| 注册/登录 401 | Token 过期（1 小时），重新登录 |
| 前端 5173 打不开 | `logs/web-ui.log` 查看 npm/esbuild 报错，重新 `npm install` |
| 报名提示“活动已结束” | 演示数据日期为 2026-12-18~20；如需改日期，改 `DemoActivitySeeder` 后清库重启 |
| 支付/订单数据想重置 | 停服务 → `DROP DATABASE sword_shield; CREATE DATABASE sword_shield ...` → 重启，Seeder 自动重建 |
| 端口被占用 | `netstat -ano | findstr :8080` 找到 PID 后 `taskkill /PID <pid> /F` |

## 9. 目录速览

```
sword_shield_web/
├─ sword-shield-common/      公共响应、异常、JWT、访问日志过滤器
├─ sword-shield-gateway/     网关 8080（JWT、路由、local 静态路由）
├─ sword-shield-user/        8101 注册登录、资料、签到积分
├─ sword-shield-activity/    8102 活动、报名、渠道统计
├─ sword-shield-content/     8103 资讯、展商、成果、资料
├─ sword-shield-interaction/ 8104 评论、点赞、分享
├─ sword-shield-file/        8105 本地/MinIO 文件
├─ sword-shield-admin/       8106 管理后台
├─ sword-shield-ai/          8107 大模型问答（可降级）
├─ sword-shield-payment/     8108 商品、订单、回调、对账
├─ sword-shield-web-ui/      Vue 3 + Vite 前端
├─ scripts/                  一键安装/构建/启动/停止
├─ uploads/                  本地文件存储（演示资料 PDF）
├─ logs/                     运行日志
└─ README_运行说明.md        本文件
```

## 10. 验收脚本

```bash
# 17 组接口回归，输出到 交付物/功能演示与数据库设计/evidence/api-transcript.md
bash "D:/JAVA EE 课设/tools/verify_flows.sh"
```
