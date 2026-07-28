# 用户 MCP 配置支持 - 实现总结

## 概述

本次实现为后端添加了 MCP 配置支持，允许不同用户配置不同的 MCP 服务。每个用户可以针对同一个 MCP 工具配置不同的参数，如服务器地址、环境变量等。

## 实现内容

### 1. 数据库表设计

**表名**: `user_mcp_config`

**主要字段**:
- `id`: 配置 ID（主键）
- `user_id`: 用户 ID
- `tool_id`: 关联的工具 ID
- `tool_name`: 工具名称（冗余字段）
- `config_name`: 配置名称
- `description`: 配置描述
- `config_json`: 用户自定义配置（JSON 格式）
- `status`: 状态（ENABLED/DISABLED）
- `priority`: 优先级

**SQL 文件**: `docs/script/sql/update/update-user-mcp-config.sql`

### 2. Java 实体类

#### 2.1 实体类 (Entity)
**文件**: `org.ruoyi.domain.entity.mcp.UserMcpConfig`
- 对应数据库表 `user_mcp_config`
- 继承 `BaseEntity`，包含创建/更新时间和用户信息

#### 2.2 业务对象 (BO)
**文件**: `org.ruoyi.domain.bo.mcp.UserMcpConfigBo`
- 用于接收前端请求参数
- 包含数据验证注解

#### 2.3 视图对象 (VO)
**文件**: `org.ruoyi.domain.vo.mcp.UserMcpConfigVo`
- 用于返回给前端的数据
- 包含 Excel 导出注解

### 3. Mapper 层

**文件**: `org.ruoyi.mapper.mcp.UserMcpConfigMapper`

**主要方法**:
- `selectEnabledByUserId(Long userId)`: 根据用户 ID 查询启用的配置
- `selectByUserIdAndToolId(Long userId, Long toolId)`: 根据用户和工具 ID 查询配置
- `deleteByUserId(Long userId)`: 删除用户的所有配置
- `deleteByToolId(Long toolId)`: 根据工具 ID 删除配置

### 4. Service 层

#### 4.1 接口
**文件**: `org.ruoyi.service.mcp.IUserMcpConfigService`

**主要方法**:
- `selectPageList()`: 分页查询配置列表
- `queryList()`: 查询配置列表（不分页）
- `selectById()`: 根据 ID 查询配置
- `selectByUserId()`: 根据用户 ID 查询配置
- `getUserAvailableTools()`: 获取用户可用的 MCP 工具列表
- `insert()`: 新增配置
- `insertBatch()`: 批量新增配置
- `update()`: 更新配置
- `deleteByIds()`: 删除配置
- `updateStatus()`: 更新配置状态

#### 4.2 实现类
**文件**: `org.ruoyi.service.mcp.impl.UserMcpConfigServiceImpl`

**核心逻辑**:
- 验证工具是否存在
- 检查配置唯一性（同一用户同一工具只能有一个配置）
- 支持批量操作
- 自动设置默认值和优先级

### 5. Controller 层

**文件**: `org.ruoyi.controller.mcp.UserMcpConfigController`

**接口路径**: `/mcp/user-config`

**主要接口**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /list | 分页查询配置列表 |
| GET | /my-list | 查询当前用户的配置列表 |
| POST | /export | 导出配置列表 |
| GET | /{id} | 获取配置详情 |
| POST | / | 新增配置 |
| POST | /batch | 批量新增配置 |
| PUT | / | 修改配置 |
| DELETE | /{ids} | 删除配置 |
| DELETE | /my-all | 删除当前用户的所有配置 |
| PUT | /{id}/status | 更新配置状态 |
| GET | /my-available-tools | 获取用户可用的 MCP 工具列表 |
| DELETE | /tool/{toolId} | 根据工具 ID 删除配置 |

### 6. MCP 客户端集成

#### 6.1 LangChain4jMcpToolProviderService 增强
**文件**: `org.ruoyi.mcp.service.core.LangChain4jMcpToolProviderService`

**新增方法**:
- `getUserEnabledToolsProvider(Long userId)`: 根据用户 ID 获取带用户配置的 ToolProvider
- `getUserMcpClient(Long userId, UserMcpConfigVo config)`: 根据用户配置创建 MCP 客户端

**核心逻辑**:
- 优先使用用户配置，如果没有则使用工具默认配置
- 支持用户覆盖工具的 command、args、env 等配置
- 自动过滤用户未启用的工具

#### 6.2 ChatServiceFacade 集成
**文件**: `org.ruoyi.service.chat.impl.ChatServiceFacade`

**新增方法**:
- `buildUserToolProvider(Long userId)`: 根据用户构建带用户 MCP 配置的 ToolProvider

**集成点**:
- 在思考模式中使用用户 MCP 配置
- 支持后续扩展到其他对话模式

## 使用说明

### 1. 数据库初始化

执行 SQL 文件创建表：
```bash
mysql -u root -p your_database < docs/script/sql/update/update-user-mcp-config.sql
```

### 2. 配置示例

#### 2.1 用户配置本地 MCP 工具

```json
{
  "userId": 100,
  "toolId": 1,
  "configName": "项目目录配置",
  "description": "用于访问项目文件",
  "configJson": {
    "command": "npx",
    "args": ["@modelcontextprotocol/server-filesystem", "/path/to/project"],
    "env": {
      "DEBUG": "true"
    }
  },
  "status": "ENABLED",
  "priority": 10
}
```

#### 2.2 用户配置远程 MCP 工具

```json
{
  "userId": 100,
  "toolId": 2,
  "configName": "开发环境",
  "configJson": {
    "baseUrl": "http://dev-server:8080/mcp",
    "headers": {
      "Authorization": "Bearer your-token"
    }
  },
  "status": "ENABLED"
}
```

### 3. API 调用示例

#### 3.1 添加用户配置
```bash
curl -X POST http://localhost:8080/mcp/user-config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "userId": 100,
    "toolId": 1,
    "configName": "测试配置",
    "configJson": "{\"env\": {\"DEBUG\": \"true\"}}",
    "status": "ENABLED"
  }'
```

#### 3.2 获取用户可用工具
```bash
curl -X GET http://localhost:8080/mcp/user-config/my-available-tools \
  -H "Authorization: Bearer your-token"
```

## 权限配置

需要在系统中配置以下权限：

| 权限码 | 说明 |
|--------|------|
| mcp:user-config:list | 查询配置列表 |
| mcp:user-config:query | 查询配置详情 |
| mcp:user-config:add | 新增配置 |
| mcp:user-config:edit | 修改配置 |
| mcp:user-config:remove | 删除配置 |
| mcp:user-config:export | 导出配置 |

## 注意事项

1. **唯一性约束**: 同一用户对同一工具只能有一个配置（数据库唯一索引）
2. **状态管理**: 只有状态为 ENABLED 的配置才会生效
3. **优先级**: 当存在多个配置时，priority 值越小优先级越高
4. **JSON 格式**: configJson 必须是合法的 JSON 格式
5. **工具存在性**: 创建配置前需确保关联的工具存在

## 扩展计划

1. 支持配置继承（全局配置 -> 租户配置 -> 用户配置）
2. 支持配置模板
3. 支持配置版本管理
4. 支持配置导入导出
5. 在普通对话模式中也使用用户 MCP 配置

## 相关文件清单

### Java 代码文件
- `src/main/java/org/ruoyi/domain/entity/mcp/UserMcpConfig.java`
- `src/main/java/org/ruoyi/domain/bo/mcp/UserMcpConfigBo.java`
- `src/main/java/org/ruoyi/domain/vo/mcp/UserMcpConfigVo.java`
- `src/main/java/org/ruoyi/mapper/mcp/UserMcpConfigMapper.java`
- `src/main/java/org/ruoyi/service/mcp/IUserMcpConfigService.java`
- `src/main/java/org/ruoyi/service/mcp/impl/UserMcpConfigServiceImpl.java`
- `src/main/java/org/ruoyi/controller/mcp/UserMcpConfigController.java`
- `src/main/java/org/ruoyi/mcp/service/core/LangChain4jMcpToolProviderService.java` (修改)
- `src/main/java/org/ruoyi/service/chat/impl/ChatServiceFacade.java` (修改)

### SQL 文件
- `docs/script/sql/update/update-user-mcp-config.sql`

### 文档文件
- `ruoyi-modules/ruoyi-chat/docs/用户 MCP 配置管理接口文档.md`
- `ruoyi-modules/ruoyi-chat/docs/MCP 用户配置支持实现总结.md` (本文件)
