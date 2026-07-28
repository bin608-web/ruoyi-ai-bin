# 用户 MCP 配置管理接口文档

## 1. 概述

用户 MCP 配置管理模块允许不同用户配置不同的 MCP 服务。每个用户可以针对同一个 MCP 工具配置不同的参数，如服务器地址、环境变量等。

## 2. 基础信息

- **模块路径**: `/mcp/user-config`
- **权限前缀**: `mcp:user-config`

## 3. 接口列表

### 3.1 查询用户 MCP 配置列表（分页）

**请求**
```
GET /mcp/user-config/list
```

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 否 | 用户 ID |
| toolId | Long | 否 | 工具 ID |
| configName | String | 否 | 配置名称（模糊查询） |
| status | String | 否 | 状态：ENABLED/DISABLED |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页数量，默认 10 |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功",
  "rows": [
    {
      "id": 1,
      "userId": 100,
      "toolId": 1,
      "toolName": "filesystem",
      "configName": "项目目录配置",
      "description": "用于访问项目文件",
      "configJson": "{\"command\": \"npx\", \"args\": [\"@modelcontextprotocol/server-filesystem\", \"/path/to/project\"]}",
      "status": "ENABLED",
      "priority": 10,
      "createTime": "2026-06-01 10:00:00",
      "updateTime": "2026-06-01 10:00:00"
    }
  ],
  "total": 1
}
```

---

### 3.2 查询当前用户的配置列表

**请求**
```
GET /mcp/user-config/my-list
```

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| toolId | Long | 否 | 工具 ID |
| status | String | 否 | 状态 |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 100,
      "toolId": 1,
      "toolName": "filesystem",
      "configName": "项目目录配置",
      "status": "ENABLED"
    }
  ]
}
```

---

### 3.3 获取配置详情

**请求**
```
GET /mcp/user-config/{id}
```

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 配置 ID |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "userId": 100,
    "toolId": 1,
    "toolName": "filesystem",
    "configName": "项目目录配置",
    "description": "用于访问项目文件",
    "configJson": "{\"command\": \"npx\", \"args\": [\"@modelcontextprotocol/server-filesystem\", \"/path/to/project\"]}",
    "status": "ENABLED",
    "priority": 10
  }
}
```

---

### 3.4 新增用户 MCP 配置

**请求**
```
POST /mcp/user-config
```

**请求体**
```json
{
  "userId": 100,
  "toolId": 1,
  "configName": "项目目录配置",
  "description": "用于访问项目文件",
  "configJson": "{\"command\": \"npx\", \"args\": [\"@modelcontextprotocol/server-filesystem\", \"/path/to/project\"]}",
  "status": "ENABLED",
  "priority": 10
}
```

**字段说明**
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户 ID |
| toolId | Long | 是 | 工具 ID |
| configName | String | 是 | 配置名称（1-100 字符） |
| description | String | 否 | 配置描述（最多 500 字符） |
| configJson | String | 否 | JSON 格式的配置信息 |
| status | String | 否 | 状态，默认 ENABLED |
| priority | Integer | 否 | 优先级，默认 100 |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 3.5 批量新增用户 MCP 配置

**请求**
```
POST /mcp/user-config/batch
```

**请求体**
```json
{
  "userId": 100,
  "toolIds": [1, 2, 3],
  "configName": "批量配置",
  "description": "批量添加工具配置",
  "configJson": "{\"env\": {\"DEBUG\": \"true\"}}",
  "status": "ENABLED",
  "priority": 50
}
```

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "成功添加 3 个配置"
}
```

---

### 3.6 修改用户 MCP 配置

**请求**
```
PUT /mcp/user-config
```

**请求体**
```json
{
  "id": 1,
  "configName": "更新后的配置名称",
  "description": "更新后的描述",
  "configJson": "{\"command\": \"npx\", \"args\": [\"@modelcontextprotocol/server-filesystem\", \"/new/path\"]}",
  "priority": 5
}
```

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 3.7 删除用户 MCP 配置

**请求**
```
DELETE /mcp/user-config/{ids}
```

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | Long[] | 是 | 配置 ID 数组，多个用逗号分隔 |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 3.8 删除当前用户的所有配置

**请求**
```
DELETE /mcp/user-config/my-all
```

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 3.9 更新配置状态

**请求**
```
PUT /mcp/user-config/{id}/status
```

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 配置 ID |

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 状态：ENABLED/DISABLED |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 3.10 获取当前用户可用的 MCP 工具列表

**请求**
```
GET /mcp/user-config/my-available-tools
```

**说明**
根据当前用户的启用配置，返回可用的 MCP 工具列表。

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "tools": [
      {
        "id": 1,
        "name": "filesystem",
        "description": "文件系统工具",
        "type": "LOCAL",
        "status": "ENABLED"
      },
      {
        "id": 2,
        "name": "playwright",
        "description": "浏览器自动化工具",
        "type": "LOCAL",
        "status": "ENABLED"
      }
    ]
  }
}
```

---

### 3.11 根据工具 ID 删除配置

**请求**
```
DELETE /mcp/user-config/tool/{toolId}
```

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| toolId | Long | 是 | 工具 ID |

**响应示例**
```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

## 4. 配置 JSON 格式说明

### 4.1 LOCAL 类型工具配置

```json
{
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/project"],
  "env": {
    "DEBUG": "true",
    "CUSTOM_VAR": "value"
  }
}
```

### 4.2 REMOTE 类型工具配置

```json
{
  "baseUrl": "http://localhost:8080/mcp",
  "headers": {
    "Authorization": "Bearer your-token-here",
    "X-Custom-Header": "value"
  },
  "timeout": 30000
}
```

## 5. 权限说明

| 权限码 | 说明 |
|--------|------|
| mcp:user-config:list | 查询配置列表 |
| mcp:user-config:query | 查询配置详情 |
| mcp:user-config:add | 新增配置 |
| mcp:user-config:edit | 修改配置 |
| mcp:user-config:remove | 删除配置 |
| mcp:user-config:export | 导出配置 |

## 6. 使用场景示例

### 6.1 场景：不同用户使用不同的 MCP 服务器

**用户 A 配置**
```json
{
  "userId": 100,
  "toolId": 1,
  "configName": "开发环境",
  "configJson": {
    "baseUrl": "http://dev-server:8080/mcp"
  }
}
```

**用户 B 配置**
```json
{
  "userId": 200,
  "toolId": 1,
  "configName": "生产环境",
  "configJson": {
    "baseUrl": "http://prod-server:8080/mcp"
  }
}
```

### 6.2 场景：用户自定义环境变量

```json
{
  "userId": 100,
  "toolId": 2,
  "configName": "调试模式",
  "configJson": {
    "command": "npx",
    "args": ["-y", "@example/mcp-server"],
    "env": {
      "DEBUG": "true",
      "LOG_LEVEL": "verbose"
    }
  }
}
```

## 7. 注意事项

1. **唯一性约束**: 同一用户对同一工具只能有一个配置
2. **状态管理**: 只有状态为 ENABLED 的配置才会生效
3. **优先级**: 当存在多个配置时，priority 值越小优先级越高
4. **JSON 格式**: configJson 必须是合法的 JSON 格式
5. **工具存在性**: 创建配置前需确保关联的工具存在
