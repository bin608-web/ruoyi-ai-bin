# 用户技能模块目录结构说明

## 项目结构

```
ruoyi-ai/
├── ruoyi-modules/
│   └── ruoyi-chat/
│       └── src/main/
│           ├── java/org/ruoyi/
│           │   ├── controller/skill/
│           │   │   └── UserSkillController.java      # 技能管理控制器
│           │   ├── service/skill/
│           │   │   ├── IUserSkillService.java        # 技能服务接口
│           │   │   └── impl/
│           │   │       └── UserSkillServiceImpl.java # 技能服务实现
│           │   ├── mapper/skill/
│           │   │   └── UserSkillMapper.java          # 技能 Mapper 接口
│           │   ├── domain/
│           │   │   ├── entity/skill/
│           │   │   │   └── UserSkill.java            # 技能实体类
│           │   │   ├── bo/skill/
│           │   │   │   └── UserSkillBo.java          # 技能业务对象
│           │   │   ├── dto/skill/
│           │   │   │   ├── UserSkillDto.java         # 技能 DTO（生成返回）
│           │   │   │   └── UserSkillTestResult.java  # 技能测试结果
│           │   │   └── vo/skill/
│           │   │       └── UserSkillVo.java          # 技能视图对象
│           │   └── manager/skill/
│           │       ├── SkillGeneratorManager.java    # 技能生成管理器
│           │       └── SkillLoaderManager.java       # 技能加载管理器
│           └── resources/
│               └── mapper/skill/
│                   └── UserSkillMapper.xml           # MyBatis 映射文件
├── script/
│   └── sql/update/
│       └── user_skill.sql                            # 数据库建表脚本
├── skills/                                           # 技能文件存储目录
│   └── {userId}/                                     # 用户技能目录
│       ├── skill_code_xxx.py                         # Python 脚本
│       ├── skill_code_xxx.json                       # MCP 配置
│       └── skill_code_xxx.js                         # JavaScript 脚本
└── docs/
    ├── api/
    │   └── user_skill_api.md                         # API 接口文档
    └── skills/
        └── README.md                                 # 本文件
```

## 核心文件说明

### 1. 实体类（Entity）

**UserSkill.java**
- 位置：`domain/entity/skill/UserSkill.java`
- 说明：数据库表映射实体
- 字段：
  - `id`: 主键 ID
  - `userId`: 用户 ID
  - `skillName`: 技能名称
  - `skillCode`: 技能编码（唯一）
  - `skillType`: 技能类型（LOCAL/MCP/CUSTOM）
  - `description`: 技能描述
  - `skillConfig`: 技能配置（JSON）
  - `skillCodeContent`: 技能代码内容
  - `filePath`: 技能文件路径
  - `isEnabled`: 是否启用
  - `isPublic`: 是否公开
  - `testResult`: 最近测试结果
  - `testTime`: 最近测试时间

### 2. 业务对象（BO）

**UserSkillBo.java**
- 位置：`domain/bo/skill/UserSkillBo.java`
- 说明：业务操作对象，用于接收前端参数
- 包含校验注解

### 3. 数据传输对象（DTO）

**UserSkillDto.java**
- 位置：`domain/dto/skill/UserSkillDto.java`
- 说明：技能生成时返回的数据结构

**UserSkillTestResult.java**
- 位置：`domain/dto/skill/UserSkillTestResult.java`
- 说明：技能测试结果的返回结构

### 4. 视图对象（VO）

**UserSkillVo.java**
- 位置：`domain/vo/skill/UserSkillVo.java`
- 说明：返回给前端的数据结构
- 包含 `convert()` 方法用于实体转换

### 5. 控制器（Controller）

**UserSkillController.java**
- 位置：`controller/skill/UserSkillController.java`
- 说明：REST API 接口
- 路径前缀：`/skill/my`
- 主要接口：
  - `GET /list`: 分页查询
  - `GET /all`: 全部查询
  - `GET /{id}`: 根据 ID 查询
  - `POST /`: 新增
  - `PUT /`: 修改
  - `DELETE /{ids}`: 删除
  - `PUT /{id}/status`: 更新状态
  - `POST /{id}/test`: 测试技能
  - `POST /generate`: 动态生成技能
  - `POST /{id}/share`: 分享技能
  - `GET /available`: 获取可用技能

### 6. 服务层（Service）

**IUserSkillService.java**
- 位置：`service/skill/IUserSkillService.java`
- 说明：服务接口定义

**UserSkillServiceImpl.java**
- 位置：`service/skill/impl/UserSkillServiceImpl.java`
- 说明：服务实现类
- 主要功能：
  - 技能 CRUD 操作
  - 技能文件保存/删除
  - 技能测试执行
  - 技能生成逻辑
  - 技能分享

### 7. 数据访问层（Mapper）

**UserSkillMapper.java**
- 位置：`mapper/skill/UserSkillMapper.java`
- 说明：MyBatis Mapper 接口
- 主要方法：
  - `selectUserSkillList`: 查询列表
  - `selectBySkillCode`: 根据编码查询
  - `updateTestResult`: 更新测试结果
  - `batchUpdateEnabled`: 批量更新状态

**UserSkillMapper.xml**
- 位置：`resources/mapper/skill/UserSkillMapper.xml`
- 说明：MyBatis SQL 映射文件

### 8. 管理器（Manager）

**SkillGeneratorManager.java**
- 位置：`manager/skill/SkillGeneratorManager.java`
- 说明：技能生成管理器
- 主要功能：
  - 调用 LLM 生成技能代码
  - 解析技能信息
  - 保存技能到数据库和文件系统

**SkillLoaderManager.java**
- 位置：`manager/skill/SkillLoaderManager.java`
- 说明：技能加载管理器
- 主要功能：
  - 加载用户启用的技能
  - 技能缓存管理
  - 文件系统扫描
  - 技能代码读取

## 数据库表

### user_skill（用户技能表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 ID |
| user_id | bigint | 用户 ID |
| skill_name | varchar(100) | 技能名称 |
| skill_code | varchar(100) | 技能编码（唯一） |
| skill_type | varchar(20) | 技能类型 |
| description | varchar(500) | 技能描述 |
| skill_config | text | 技能配置（JSON） |
| skill_code_content | longtext | 技能代码内容 |
| file_path | varchar(500) | 技能文件路径 |
| is_enabled | char(1) | 是否启用 |
| is_public | char(1) | 是否公开 |
| test_result | text | 测试结果 |
| test_time | datetime | 测试时间 |
| tenant_id | bigint | 租户 ID |
| create_by | varchar(64) | 创建者 |
| create_time | datetime | 创建时间 |
| update_by | varchar(64) | 更新者 |
| update_time | datetime | 更新时间 |

### skill_market（技能市场表）

用于存储公开分享的技能。

### skill_share（技能分享记录表）

用于记录技能分享行为。

## 技能文件存储

### 目录结构

```
E:\working\ruoyi-ai\skills\
├── 100/                              # 用户 100 的技能目录
│   ├── text_summary_1234567890.py    # Python 脚本
│   ├── data_extract_2234567890.js    # JavaScript 脚本
│   └── mcp_tool_3234567890.json      # MCP 配置
└── 200/                              # 用户 200 的技能目录
    └── ...
```

### 文件命名规则

- **LOCAL 类型**: `{skillCode}.py`
- **MCP 类型**: `{skillCode}.json`
- **CUSTOM 类型**: `{skillCode}.js`

### 文件权限

- 创建目录：`skills/{userId}/`
- 文件读写：当前运行用户
- 建议权限：644（文件），755（目录）

## 使用流程

### 1. 动态生成技能

```
用户描述 → SkillGeneratorManager → LLM → 代码生成 → 保存到数据库/文件系统
```

### 2. 聊天时使用技能

```
聊天开始 → SkillLoaderManager.loadUserSkills() → 加载技能 → 注入 LLM 上下文 → 用户使用
```

### 3. 测试技能

```
选择技能 → 输入测试数据 → UserSkillServiceImpl.testSkill() → 执行脚本 → 返回结果
```

## 扩展说明

### 添加新的技能类型

1. 在 `UserSkill` 实体类中添加新的 `skillType` 值
2. 在 `SkillGeneratorManager.getFileExtension()` 中添加文件扩展名映射
3. 在 `UserSkillServiceImpl.executeLocalScript()` 中添加执行逻辑
4. 更新文档

### 自定义技能生成模板

修改 `SkillGeneratorManager.generateTemplateCode()` 方法中的模板代码。

## 注意事项

1. **文件路径**: Windows 路径使用反斜杠，注意转义
2. **编码格式**: 所有文件使用 UTF-8 编码
3. **权限控制**: 用户只能操作自己的技能
4. **唯一性**: 技能编码必须唯一
5. **缓存管理**: 技能变更后需要刷新缓存

## 相关文档

- [API 接口文档](../../docs/api/user_skill_api.md)
- [数据库建表脚本](../../script/sql/update/user_skill.sql)
