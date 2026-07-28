# 用户技能动态生成与管理 - 实施说明

## 概述

本文档详细说明如何在若依 AI 平台中实现用户技能的动态生成与管理功能。

## 已完成的工作

### 1. 数据库设计

**文件**: `script/sql/update/user_skill.sql`

创建了以下三张表：
- `user_skill`: 用户技能表（主表）
- `skill_market`: 技能市场表（公开分享）
- `skill_share`: 技能分享记录表

**执行 SQL**:
```sql
source E:\working\ruoyi-ai\script\sql\update\user_skill.sql
```

### 2. 实体类（已存在）

**文件位置**: `ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/domain/entity/skill/UserSkill.java`

包含字段：
- 基本信息：id, userId, skillName, skillCode, skillType
- 内容信息：description, skillConfig, skillCodeContent, filePath
- 状态信息：isEnabled, isPublic, testResult, testTime
- 审计信息：tenantId, createBy, createTime, updateBy, updateTime

### 3. BO/DTO/VO（已存在）

**BO**: `UserSkillBo.java` - 业务操作对象
**DTO**: 
- `UserSkillDto.java` - 技能生成返回
- `UserSkillTestResult.java` - 测试结果

**VO**: `UserSkillVo.java` - 视图对象（已添加转换方法）

### 4. Mapper 层

**接口**: `UserSkillMapper.java`（已存在）
**XML**: `UserSkillMapper.xml`（已创建）

主要方法：
- `selectUserSkillList`: 查询技能列表
- `selectBySkillCode`: 根据编码查询
- `updateTestResult`: 更新测试结果
- `batchUpdateEnabled`: 批量更新状态

### 5. Service 层

**接口**: `IUserSkillService.java`（已存在）
**实现**: `UserSkillServiceImpl.java`（已创建）

主要功能：
- 技能 CRUD 操作
- 技能文件保存/删除
- 技能测试执行
- 技能生成逻辑
- 技能分享

### 6. Controller 层（已存在）

**文件**: `UserSkillController.java`

API 端点：
- `GET /skill/my/list` - 分页查询
- `GET /skill/my/all` - 全部查询
- `GET /skill/my/{id}` - 根据 ID 查询
- `POST /skill/my` - 新增技能
- `PUT /skill/my` - 修改技能
- `DELETE /skill/my/{ids}` - 删除技能
- `PUT /skill/my/{id}/status` - 更新状态
- `POST /skill/my/{id}/test` - 测试技能
- `POST /skill/my/generate` - 动态生成技能
- `POST /skill/my/{id}/share` - 分享技能
- `GET /skill/my/available` - 获取可用技能

### 7. 管理器（已创建）

**SkillGeneratorManager.java**
- 根据用户描述调用 LLM 生成技能代码
- 保存技能到数据库和文件系统
- 支持 LOCAL/MCP/CUSTOM 三种类型

**SkillLoaderManager.java**
- 加载用户启用的技能
- 技能缓存管理
- 聊天时自动注入技能上下文

### 8. 目录结构

**技能存储目录**: `E:\working\ruoyi-ai\skills\{userId}\`

文件命名：
- LOCAL: `{skillCode}.py`
- MCP: `{skillCode}.json`
- CUSTOM: `{skillCode}.js`

## 待完成的工作

### 1. 数据库初始化

执行建表 SQL：
```bash
# 进入 MySQL
mysql -u root -p

# 选择数据库
use ruoyi_ai;

# 执行建表脚本
source E:\working\ruoyi-ai\script\sql\update\user_skill.sql;
```

### 2. LLM 集成

**位置**: `SkillGeneratorManager.generateCodeWithLLM()`

当前使用模板代码，需要集成实际的 LLM 服务：

```java
private String generateCodeWithLLM(String description, String skillType) {
    // TODO: 调用实际的 LLM 服务
    
    // 示例（根据项目中的 LLM 调用方式修改）：
    ChatClient chatClient = ChatClient.create();
    String response = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();
    
    return response;
}
```

### 3. 技能执行引擎

**位置**: `UserSkillServiceImpl.executeLocalScript()`

当前支持 Python 和 Node.js 脚本，需要：
1. 确保环境已安装（python, node）
2. 配置执行路径
3. 添加超时和错误处理

### 4. MCP 工具集成

**位置**: `UserSkillServiceImpl.executeMcpTool()`

需要集成 MCP 协议调用逻辑。

### 5. 聊天上下文注入

在聊天模块中集成技能加载：

```java
// 在聊天服务中
@Autowired
private SkillLoaderManager skillLoaderManager;

public ChatResponse chat(Long userId, String message) {
    // 加载用户技能
    List<LoadedSkill> skills = skillLoaderManager.loadUserSkills(userId);
    
    // 构建技能上下文
    StringBuilder skillContext = new StringBuilder();
    for (LoadedSkill skill : skills) {
        skillContext.append("可用技能：").append(skill.getSkillName())
                   .append("（").append(skill.getDescription()).append("）\n");
    }
    
    // 注入到 LLM 提示词
    String fullPrompt = skillContext.toString() + message;
    
    // ... 继续聊天流程
}
```

### 6. 权限配置

在系统中配置菜单和权限：

1. 添加菜单：用户技能管理
2. 配置权限标识：
   - `skill:my:list`
   - `skill:my:query`
   - `skill:my:add`
   - `skill:my:edit`
   - `skill:my:remove`
   - `skill:my:test`
   - `skill:my:share`
   - `skill:my:export`

### 7. 前端页面（可选）

需要开发的前端页面：
1. 技能列表页
2. 技能详情页
3. 技能生成页
4. 技能测试页
5. 技能分享页

## 使用流程

### 场景 1: 用户动态生成技能

1. 用户在聊天中说："帮我生成一个文本总结技能"
2. 调用 `POST /skill/my/generate`
3. 系统调用 LLM 生成 Python 代码
4. 保存到 `skills/{userId}/text_summary_xxx.py`
5. 记录到数据库
6. 返回生成的技能信息

### 场景 2: 聊天时使用技能

1. 用户开始聊天
2. 系统自动调用 `SkillLoaderManager.loadUserSkills(userId)`
3. 加载所有启用的技能
4. 将技能信息注入到 LLM 上下文
5. 用户可以说："使用文本总结技能处理这段话"
6. 系统识别意图，调用对应技能
7. 执行技能脚本，返回结果

### 场景 3: 测试技能

1. 用户在技能详情页点击"测试"
2. 输入测试数据
3. 调用 `POST /skill/my/{id}/test`
4. 系统执行技能脚本
5. 返回执行结果

## 配置说明

### 1. 技能存储路径

修改 `UserSkillServiceImpl.SKILLS_BASE_PATH`：

```java
private static final String SKILLS_BASE_PATH = "E:\\working\\ruoyi-ai\\skills";
```

### 2. 执行环境

确保以下环境已安装：
- Python 3.x（用于 LOCAL 类型）
- Node.js（用于 CUSTOM 类型）

### 3. 权限配置

在 `application.yml` 中配置：

```yaml
sa-token:
  permission:
    # 启用权限检查
    enabled: true
```

## 测试建议

### 1. 单元测试

```java
@SpringBootTest
public class UserSkillServiceTest {
    
    @Autowired
    private IUserSkillService userSkillService;
    
    @Test
    public void testGenerateSkill() {
        UserSkillBo bo = new UserSkillBo();
        bo.setSkillName("测试技能");
        bo.setSkillType("LOCAL");
        
        UserSkillDto dto = userSkillService.generateSkill(1L, "测试", "LOCAL");
        Assert.assertTrue(dto.isSuccess());
    }
    
    @Test
    public void testExecuteSkill() {
        // 测试技能执行
    }
}
```

### 2. 集成测试

使用 Postman 测试 API：
1. 登录获取 token
2. 调用生成接口
3. 调用测试接口
4. 验证结果

## 注意事项

1. **安全性**: 技能代码执行存在安全风险，建议：
   - 在沙箱环境中执行
   - 限制执行时间
   - 限制资源使用
   - 禁止访问敏感文件

2. **性能**: 
   - 技能代码应缓存
   - 避免重复加载
   - 使用异步执行

3. **错误处理**:
   - 捕获执行异常
   - 返回友好错误信息
   - 记录执行日志

4. **扩展性**:
   - 支持新的技能类型
   - 支持技能版本管理
   - 支持技能依赖管理

## 相关文件清单

### Java 代码
- `UserSkill.java` - 实体类
- `UserSkillBo.java` - 业务对象
- `UserSkillDto.java` - DTO
- `UserSkillTestResult.java` - 测试结果
- `UserSkillVo.java` - 视图对象
- `UserSkillMapper.java` - Mapper 接口
- `UserSkillMapper.xml` - SQL 映射
- `IUserSkillService.java` - Service 接口
- `UserSkillServiceImpl.java` - Service 实现
- `UserSkillController.java` - Controller
- `SkillGeneratorManager.java` - 技能生成器
- `SkillLoaderManager.java` - 技能加载器

### SQL 脚本
- `user_skill.sql` - 建表脚本

### 文档
- `user_skill_api.md` - API 文档
- `README.md` - 目录结构说明
- `IMPLEMENTATION.md` - 实施说明（本文件）

### 示例
- `skills/demo/text_summary_example.py` - 示例技能

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-02 | 初始版本 |
