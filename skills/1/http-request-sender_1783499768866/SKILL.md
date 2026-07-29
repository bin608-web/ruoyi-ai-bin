---
name: http-request-sender
description: 当需要发送 HTTP GET 或 POST 请求并支持自定义请求头时使用此技能。适用于 API 测试、自动化数据抓取、服务健康检查等场景。
---

# HTTP 请求发送器

## Overview

通过执行 `scripts/main.py` 脚本发送 HTTP GET 或 POST 请求，支持自定义 headers，并将响应状态码、响应头和响应体返回给调用方。

## 使用场景

- 快速验证 REST API 端点是否可达
- 携带自定义认证头（如 `Authorization`）访问受保护资源
- 简单数据抓取或表单提交
- 调试外部服务响应内容

## 工作流程

1. 接收用户指定的 URL、HTTP 方法（GET 或 POST）、自定义请求头（可选）和请求体（POST 时可选）
2. 调用 `scripts/main.py` 脚本并传入相应参数
3. 脚本执行 HTTP 请求并打印 JSON 格式结果（状态码、响应头、响应体）
4. 将脚本输出解析并返回给用户

## 输入/输出

- 输入：
  - `url` (必需)：目标 URL
  - `method` (必需)：`GET` 或 `POST`
  - `headers` (可选)：JSON 字符串形式的自定义请求头，例如 `'{"Authorization": "Bearer token"}'`
  - `body` (可选)：请求体字符串，仅在 POST 时使用
- 输出：
  - JSON 格式的响应，包含 `status_code`、`headers` 和 `body` 三个字段

## 脚本说明

脚本位于 `scripts/main.py`，通过命令行参数接收输入：

```
python scripts/main.py --url "https://example.com" --method GET --headers '{"accept": "application/json"}'
```

- `--url`：请求 URL
- `--method`：`GET` 或 `POST`
- `--headers`：JSON 格式的请求头（可选）
- `--body`：请求体文本（可选）

脚本使用标准库 `urllib.request` 发送请求，不依赖第三方包。响应头以字典形式返回，响应体以文本形式返回。

## 注意事项

- 请求超时设置为 10 秒，超时会抛出异常并返回错误信息
- 脚本不会跟随重定向，若需要跟随请使用其他技能或工具
- 请确保传入的 headers 为合法 JSON，否则解析失败
- 对于大体积响应，输出可能会被截断，建议仅用于轻量级请求