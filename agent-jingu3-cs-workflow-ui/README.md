# agent-jingu3-cs-workflow-ui

独立 **BPMN 管理前端**（Vue 3 + Vite + bpmn-js），调用服务端 `GET/POST /api/v1/bpmn/**`（Flowable 引擎）。

## 与主仓库关系

- **不**参与 Maven 聚合；与 `agent-jingu3-cs-client`（Electron）并列。
- 对话里的 **JSON 工作流**（`mode=WORKFLOW` + `workflowId` → `workflows/*.json`）与本 UI **无关**，二者短期并存。

## 本地运行

1. 启动后端（默认 `http://localhost:8080`，见 `agent-jingu3-cs-server`）。
2. 复制 `.env.example` 为 `.env.development`（或直接使用仓库内已有 `.env.development`）。
3. 安装与开发：

```bash
cd agent-jingu3-cs-workflow-ui
npm ci
npm run dev
```

浏览器打开 Vite 提示的地址（默认 `http://localhost:5173`）。跨域由服务端 `Jingu3ApiCorsConfiguration` 对 `/api/v1/**` 放行。

## 安全提示

MVP **无登录**；`/api/v1/bpmn/**` 在开发配置下可被匿名部署与启停流程。**仅限内网/开发环境**；生产环境必须加认证与权限。
