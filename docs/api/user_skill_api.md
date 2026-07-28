# 用户技能管理 API 接口文档

## 概述

用户技能管理模块支持动态生成、管理和使用用户自定义技能。技能可以在聊天时动态生成，并自动加载到聊天上下文中使用。

## 基础信息

- **Base URL**: `/ruoyi-chat/skill/my`
- **认证方式**: SaToken 认证
- **权限前缀**: `skill:my`

## 接口列表

### 1. 查询用户技能列表（分页）

**请求**

```
GET /skill/my/list
```

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| skillType | String | 否 | 技能类型（LOCAL/MCP/CUSTOM） |
| isEnabled | String | 否 | 是否启用（Y/N） |
| keyword | String | 否 | 搜索关键词 |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "rows": [
    {
      "id": 1,
      "userId": 100,
      "skillName": "文本总结",
      "skillCode": "text_summary_1234567890",
      "skillType": "LOCAL",
      "description": "对长文本进行自动总结",
      "filePath": "E:\\working\\ruoyi-ai\\skills\\100\\text_summary_1234567890.py",
      "isEnabled": "Y",
      "isPublic": "N",
      "testResult": "{\"status\":\"success\"}",
      "testTime": "2026-06-02 10:00:00",
      "createTime": "2026-06-01 09:00:00",
      "createBy": "admin"
    }
  ],
  "total": 1
}
```

---

### 2. 查询用户技能列表（不分页）

**请求**

```
GET /skill/my/all
```

**请求参数**

同分页查询参数

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "skillName": "文本总结",
      "skillType": "LOCAL",
      ...
    }
  ]
}
```

---

### 3. 根据技能 ID 获取详细信息

**请求**

```
GET /skill/my/{id}
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 技能 ID |

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "skillName": "文本总结",
    "skillCode": "text_summary_1234567890",
    "skillType": "LOCAL",
    "description": "对长文本进行自动总结",
    "skillConfig": "{}",
    "filePath": "E:\\working\\ruoyi-ai\\skills\\100\\text_summary_1234567890.py",
    "isEnabled": "Y",
    "isPublic": "N",
    "testResult": null,
    "testTime": null,
    "createTime": "2026-06-01 09:00:00",
    "createBy": "admin"
  }
}
```

---

### 4. 根据技能编码获取详细信息

**请求**

```
GET /skill/my/code/{skillCode}
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| skillCode | String | 是 | 技能编码 |

---

### 5. 新增技能

**请求**

```
POST /skill/my
```

**请求体**

```json
{
  "skillName": "文本总结",
  "skillCode": "text_summary",
  "skillType": "LOCAL",
  "description": "对长文本进行自动总结",
  "skillConfig": "{}",
  "skillCodeContent": "# Python 代码内容...",
  "isEnabled": "Y",
  "isPublic": "N"
}
```

**字段说明**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| skillName | String | 是 | 技能名称 |
| skillCode | String | 否 | 技能编码，不传自动生成 |
| skillType | String | 是 | 技能类型（LOCAL/MCP/CUSTOM） |
| description | String | 否 | 技能描述 |
| skillConfig | String | 否 | 技能配置（JSON 格式） |
| skillCodeContent | String | 否 | 技能代码内容 |
| isEnabled | String | 否 | 是否启用，默认 Y |
| isPublic | String | 否 | 是否公开，默认 N |

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功"
}
```

---

### 6. 修改技能

**请求**

```
PUT /skill/my
```

**请求体**

同新增技能，需包含 id 字段

```json
{
  "id": 1,
  "skillName": "文本总结（已更新）",
  "description": "更新后的描述",
  "skillCodeContent": "# 更新后的代码..."
}
```

---

### 7. 删除技能

**请求**

```
DELETE /skill/my/{ids}
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | Long | 是 | 技能 ID 列表，多个用逗号分隔 |

**示例**

```
DELETE /skill/my/1,2,3
```

---

### 8. 更新技能状态

**请求**

```
PUT /skill/my/{id}/status
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 技能 ID |

**查询参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| isEnabled | String | 是 | Y-启用，N-禁用 |

**示例**

```
PUT /skill/my/1/status?isEnabled=N
```

---

### 9. 测试技能

**请求**

```
POST /skill/my/{id}/test
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 技能 ID |

**请求体**

```json
{
  "testInput": "这是一段需要测试的文本内容"
}
```

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "success": true,
    "message": "测试成功",
    "output": "{\"status\":\"success\",\"result\":\"处理结果\"}",
    "error": null,
    "executionTime": 150,
    "rawOutput": "{\"status\":\"success\",\"result\":\"处理结果\"}"
  }
}
```

---

### 10. 动态生成技能

**请求**

```
POST /skill/my/generate
```

**请求体**

```json
{
  "skillName": "文本总结",
  "skillType": "LOCAL"
}
```

**字段说明**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| skillName | String | 是 | 技能描述（LLM 根据此描述生成代码） |
| skillType | String | 是 | 技能类型（LOCAL/MCP/CUSTOM） |

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "skillName": "文本总结",
    "skillCode": "text_summary_1234567890",
    "skillType": "LOCAL",
    "description": "文本总结",
    "skillCodeContent": "#!/usr/bin/env python3...",
    "filePath": "E:\\working\\ruoyi-ai\\skills\\100\\text_summary_1234567890.py",
    "success": true,
    "errorMessage": null
  }
}
```

---

### 11. 分享技能

**请求**

```
POST /skill/my/{id}/share
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 技能 ID |

**查询参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| toUserId | Long | 否 | 接收者用户 ID（公开分享时不传） |
| shareType | String | 是 | 分享类型（PUBLIC-公开，PRIVATE-私聊） |
| message | String | 否 | 分享消息 |

**示例**

```
# 公开分享
POST /skill/my/1/share?shareType=PUBLIC

# 私聊分享
POST /skill/my/1/share?shareType=PRIVATE&toUserId=200
```

---

### 12. 获取用户可用技能列表

**请求**

```
GET /skill/my/available
```

**说明**

获取当前用户所有已启用的技能，用于聊天时选择使用。

**响应示例**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "skillName": "文本总结",
      "skillCode": "text_summary_1234567890",
      "skillType": "LOCAL",
      "description": "对长文本进行自动总结",
      "isEnabled": "Y",
      "isPublic": "N"
    },
    {
      "id": 2,
      "skillName": "数据提取",
      "skillCode": "data_extract_1234567890",
      "skillType": "LOCAL",
      "description": "从文本中提取指定数据",
      "isEnabled": "Y",
      "isPublic": "N"
    }
  ]
}
```

---

## 权限配置

需要在菜单管理中配置以下权限：

| 权限标识 | 说明 |
|----------|------|
| skill:my:list | 查询技能列表 |
| skill:my:query | 查询技能详情 |
| skill:my:add | 新增技能 |
| skill:my:edit | 修改技能 |
| skill:my:remove | 删除技能 |
| skill:my:test | 测试技能 |
| skill:my:share | 分享技能 |
| skill:my:export | 导出技能 |

---

## 目录结构

### 技能文件存储

```
E:\working\ruoyi-ai\skills\
├── {userId1}/
│   ├── skill_code_1234567890.py
│   ├── skill_code_2234567890.js
│   └── skill_code_3234567890.json
├── {userId2}/
│   └── ...
└── ...
```

### 文件命名规则

- **LOCAL 类型**: `{skillCode}.py`
- **MCP 类型**: `{skillCode}.json`
- **CUSTOM 类型**: `{skillCode}.js`

---

## 技能类型说明

### LOCAL（本地脚本）

- 执行本地 Python 脚本
- 支持命令行参数传入
- 返回标准输出结果

### MCP（MCP 工具）

- 符合 MCP 协议规范
- JSON 格式配置
- 可通过 MCP 服务调用

### CUSTOM（自定义）

- JavaScript 脚本
- 可扩展其他类型
- 自定义执行逻辑

---

## 使用示例

### 1. 动态生成技能

用户说："帮我生成一个技能，可以把长文本总结成 200 字以内的摘要"

调用接口：
```
POST /skill/my/generate
{
  "skillName": "把长文本总结成 200 字以内的摘要",
  "skillType": "LOCAL"
}
```

系统自动：
1. 调用 LLM 生成 Python 代码
2. 保存到 `E:\working\ruoyi-ai\skills\{userId}\summary_xxx.py`
3. 记录到数据库
4. 返回生成的技能信息

### 2. 聊天时使用技能

聊天时，系统自动加载用户启用的技能：

```java
List<LoadedSkill> skills = skillLoaderManager.loadUserSkills(userId);
```

将技能信息注入到 LLM 上下文，用户可以直接说：
- "使用文本总结技能处理这段话..."
- "用数据提取技能从这个文本中提取信息..."

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无权限 |
| 500 | 服务器错误 |

---

## 注意事项

1. 技能文件保存在本地文件系统，需确保目录存在且有写入权限
2. 技能编码必须唯一，系统会自动生成唯一编码
3. 测试技能时，LOCAL 类型需要安装 Python 环境
4. 公开分享的技能会进入技能市场，需审核后才能显示
5. 删除技能会同时删除对应的文件
