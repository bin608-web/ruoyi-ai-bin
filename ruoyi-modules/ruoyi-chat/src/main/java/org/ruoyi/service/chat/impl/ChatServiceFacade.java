package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.ReActAgent;
import org.ruoyi.agent.SkillsAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.common.chat.base.ThreadContext;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.dto.request.ReSumeRunner;
import org.ruoyi.common.chat.domain.dto.request.WorkFlowRunner;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.common.core.utils.ObjectUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.domain.vo.skill.UserSkillVo;
import org.ruoyi.service.skill.IUserSkillService;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mcp.service.core.LangChain4jMcpToolProviderService;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.observability.*;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.skills.service.core.SkillsToolProviderService;
import dev.langchain4j.mcp.McpToolProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 聊天服务门面层
 * <p>
 * 作为统一入口，负责：
 * 1. 构建对话上下文
 * 2. 路由到对应的处理器
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceFacade implements IChatService {

    private static final Integer DEFAULT_MAX_MESSAGES = 20;

    private final IChatModelService chatModelService;

    private final ChatServiceFactory chatServiceFactory;

    private final IKnowledgeInfoService knowledgeInfoService;

    private final VectorStoreService vectorStoreService;

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final SseEmitterManager sseEmitterManager;

    private final IChatMessageService chatMessageService;

    private final IWorkFlowStarterService workFlowStarterService;

    private final ToolProviderFactory toolProviderFactory;
    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;
    private final SkillsToolProviderService skillsToolProviderService;
    private final IUserSkillService userSkillService;

    /**
     * 内存实例缓存，避免同一会话重复创建
     * Key: sessionId, Value: MessageWindowChatMemory实例
     */
    private static final Map<Object, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();



    /**
     * 统一聊天入口 - SSE流式响应
     *
     * @param chatRequest 聊天请求
     * @return SseEmitter
     */
    public SseEmitter sseChat(ChatRequest chatRequest) {

        // 4. 具体的服务实现
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();
        SseEmitter emitter = sseEmitterManager.connect(userId, tokenValue);

        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 2. 构建上下文消息列表
        List<ChatMessage> contextMessages = buildContextMessages(chatRequest);

        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(contextMessages);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 3. 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
        SseEmitter sseEmitter = handleSpecialChatModes(chatRequest);
        if (sseEmitter != null) {
            return sseEmitter;
        }

        // 4. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        // 5. 检查是否需要使用工具（MCP + Skills）
        boolean hasMcpConfigs = chatRequest.getMcpConfigIds() != null && !chatRequest.getMcpConfigIds().isEmpty();
        boolean hasSkillIds = chatRequest.getSkillIds() != null && !chatRequest.getSkillIds().isEmpty();

        // 6. 使用流式对话处理所有聊天
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        StreamingChatResponseHandler handler = createResponseHandler(userId, tokenValue, chatRequest);

        if (hasMcpConfigs || hasSkillIds) {
            // 有工具：ReAct 循环 — 提示词 + 执行脚本 + 循环调用模型
            log.info("ReAct 循环模式（MCP: {}, Skills: {}）",
                chatRequest.getMcpConfigIds(), chatRequest.getSkillIds());
            handleReActLoop(chatRequest, chatModelVo, chatService, streamingChatModel, handler);
        } else {
            streamingChatModel.chat(chatRequest.getContent(), handler);
        }
        return emitter;
    }

    /**
     * 根据用户构建带用户 MCP 配置和 Skills 的 ToolProvider
     *
     * @param userId       用户 ID
     * @param mcpConfigIds MCP 配置 ID 列表（可选）
     * @param skillIds     Skills ID 列表（可选，String 类型来自前端）
     * @return ToolProvider
     */
    private ToolProvider buildUserToolProvider(Long userId, List<Long> mcpConfigIds,
                                                List<String> skillIds,
                                                boolean hasMcpConfigs, boolean hasSkillIds) {
        if (userId == null) {
            return null;
        }

        // 获取 MCP 工具提供者（仅在用户选择了 MCP 配置时才加载）
        ToolProvider mcpProvider = null;
        if (hasMcpConfigs && mcpConfigIds != null && !mcpConfigIds.isEmpty()) {
            mcpProvider = langChain4jMcpToolProviderService.getUserEnabledToolsProvider(userId, mcpConfigIds);
            if (mcpProvider == null) {
                log.warn("未找到用户 {} 的 MCP 配置: {}", userId, mcpConfigIds);
            }
        }

        // 获取 Skills 工具提供者（仅在用户选择了 Skills 时才加载）
        ToolProvider skillsProvider = null;
        if (hasSkillIds && skillIds != null && !skillIds.isEmpty()) {
            List<Long> skillIdLongs = skillIds.stream()
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
            skillsProvider = skillsToolProviderService.getToolProvider(userId, skillIdLongs);
            if (skillsProvider == null) {
                log.warn("未找到用户 {} 的 Skills: {}", userId, skillIds);
            }
        }

        // 优先返回 MCP，其次 Skills
        if (mcpProvider != null) {
            return mcpProvider;
        }
        if (skillsProvider != null) {
            return skillsProvider;
        }

        log.warn("用户 {} 既无 MCP 配置也无 Skills，返回 null", userId);
        return null;
    }

    /**
     * 使用 ReActAgent 处理聊天（支持 MCP + Skills 工具）
     * ReAct 模式：Thought → Action → Action Input → Observation 循环
     * 即使没有工具，ReActAgent 也会按规范格式输出最终答案
     */
    private void handleReActChat(ChatRequest chatRequest, ChatModelVo chatModelVo, ToolProvider toolProvider) {
//        reActChatHandler.handle(chatRequest, chatModelVo, toolProvider);
    }

    /**
     * @deprecated 使用 ReActChatHandler 替代
     */
    @Deprecated
    private void handleAgentChat(ChatRequest chatRequest, ChatModelVo chatModelVo) {
        Long userId = chatRequest.getUserId();
        String tokenValue = chatRequest.getTokenValue();

        // 构建同步聊天模型
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(Duration.ofSeconds(120))
            .build();

        // 构建用户工具提供者
        boolean hasMcp = chatRequest.getMcpConfigIds() != null && !chatRequest.getMcpConfigIds().isEmpty();
        boolean hasSkills = chatRequest.getSkillIds() != null && !chatRequest.getSkillIds().isEmpty();
        ToolProvider toolProvider = buildUserToolProvider(userId,
            chatRequest.getMcpConfigIds(), chatRequest.getSkillIds(), hasMcp, hasSkills);

        // 通过 SSE 发送工具调用通知
        SseMessageUtils.sendContent(userId, "🔧 正在加载工具（MCP + Skills）...\n\n");

        // 异步执行 Agent
        CompletableFuture.runAsync(() -> {
            try {
                // 使用简单的 Agent 模式处理
                var agent = AgenticServices.agentBuilder(WebSearchAgent.class)
                    .chatModel(chatModel)
                    .toolProvider(toolProvider)
                    .listener(new MyAgentListener())
                    .build();

                String result = agent.search(chatRequest.getContent());
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
            } catch (Exception e) {
                log.error("Agent 执行失败", e);
                SseMessageUtils.sendError(userId, e.getMessage());
            } finally {
                SseMessageUtils.completeConnection(userId, tokenValue);
            }
        });
    }

    /**
     * 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
     *
     * @param chatRequest      聊天请求
     * @return 如果需要提前返回则返回SseEmitter，否则返回null
     */
    private SseEmitter handleSpecialChatModes(ChatRequest chatRequest) {
        // 处理工作流对话
        if (chatRequest.getEnableWorkFlow()) {
            log.info("处理工作流对话,会话: {}", chatRequest.getSessionId());

            WorkFlowRunner runner = chatRequest.getWorkFlowRunner();
            if (ObjectUtils.isEmpty(runner)) {
                log.warn("工作流参数为空");
            }
            return workFlowStarterService.streaming(
                ThreadContext.getCurrentUser(),
                runner.getUuid(),
                runner.getInputs(),
                chatRequest.getSessionId()
            );
        }

        // 处理人机交互恢复
        if (chatRequest.getIsResume()) {
            log.info("处理人机交互恢复");
            ReSumeRunner reSumeRunner = chatRequest.getReSumeRunner();
            if (ObjectUtils.isEmpty(reSumeRunner)) {
                log.warn("人机交互恢复参数为空");
            }
            workFlowStarterService.resumeFlow(
                reSumeRunner.getRuntimeUuid(),
                reSumeRunner.getFeedbackContent(),
                chatRequest.getEmitter()
            );

            return chatRequest.getEmitter();

        }
        // 处理思考模式
        if (chatRequest.getEnableThinking()) {
           return handleThinkingMode(chatRequest);
        }

        return null;
    }

    /**
     * 处理思考模式
     *
     * @param chatRequest     聊天请求

     */
    private SseEmitter handleThinkingMode(ChatRequest chatRequest) {
        // 配置监督者模型
        OpenAiChatModel plannerModel = OpenAiChatModel.builder()
            .baseUrl(chatRequest.getChatModelVo().getApiHost())
            .apiKey(chatRequest.getChatModelVo().getApiKey())
            .modelName(chatRequest.getChatModelVo().getModelName())
            .build();

        // Bing 搜索 MCP 客户端
//        McpTransport bingTransport = new StdioMcpTransport.Builder()
//            .command(List.of("npx.cmd", "-y", "bing-cn-mcp"))
//            .logEvents(true)
//            .build();

        Long userId = chatRequest.getUserId();
//        McpClient bingMcpClient = new DefaultMcpClient.Builder()
//            .transport(bingTransport)
//            .listener(new MyMcpClientListener(userId))
//            .build();

        // Playwright MCP 客户端 - 浏览器自动化工具
        McpTransport playwrightTransport = new StdioMcpTransport.Builder()
            .command(List.of("npx.cmd", "-y", "@playwright/mcp@latest"))
            .logEvents(true)
            .build();

        McpClient playwrightMcpClient = new DefaultMcpClient.Builder()
            .transport(playwrightTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // 使用用户 MCP 配置和 Skills 获取工具提供者
        boolean hasMcpConfigs = chatRequest.getMcpConfigIds() != null && !chatRequest.getMcpConfigIds().isEmpty();
        boolean hasSkillIds = chatRequest.getSkillIds() != null && !chatRequest.getSkillIds().isEmpty();

        ToolProvider toolProvider = null;
        if (hasMcpConfigs || hasSkillIds) {
            toolProvider = buildUserToolProvider(userId,
                chatRequest.getMcpConfigIds(), chatRequest.getSkillIds(), hasMcpConfigs, hasSkillIds);
        }

        // Filesystem MCP 客户端 - 文件管理工具
        // 允许 AI 读取、写入、搜索文件（基于当前项目根目录）
        String userDir = System.getProperty("user.dir");
        McpTransport filesystemTransport = new StdioMcpTransport.Builder()
            .command(List.of("npx.cmd", "-y",
                "@modelcontextprotocol/server-filesystem", userDir))
            .logEvents(true)
            .build();

        McpClient filesystemMcpClient = new DefaultMcpClient.Builder()
            .transport(filesystemTransport)
            .listener(new MyMcpClientListener(userId))
            .initializationTimeout(Duration.ofSeconds(120))
            .build();

        // ========== LangChain4j Skills 基本用法 ==========
        // 通过 SKILL.md 文件定义，LLM 按需通过 activate_skill 工具加载
        // 加载 Skills - 使用相对路径，基于项目根目录
        java.nio.file.Path skillsPath = java.nio.file.Path.of(userDir, "ruoyi-admin/src/main/resources/skills");
        List<dev.langchain4j.skills.FileSystemSkill> skillsList = dev.langchain4j.skills.FileSystemSkillLoader
            .loadSkills(skillsPath)
            ;

        ShellSkills skills = ShellSkills.from(skillsList);

        // 构建子 Agent
        WebSearchAgent searchAgent  = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(plannerModel)
            .toolProvider(toolProvider)
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 2: SkillsAgent - 负责文档处理技能（docx、pdf、xlsx）
        // 独立管理 Skills 工具
        SkillsAgent skillsAgent = AgenticServices.agentBuilder(SkillsAgent.class)
            .chatModel(plannerModel)
            .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                + "\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.")
            .toolProvider(skills.toolProvider())
            .build();

        // 构建子 Agent 3: SqlAgent - 负责数据库查询
        SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 4: ChartGenerationAgent - 负责图表生成
        ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
            .chatModel(plannerModel)
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 5: EchartsAgent - 负责数据可视化（结合 SQL 查询生成 Echarts 图表）
        EchartsAgent echartsAgent = AgenticServices.agentBuilder(EchartsAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .listener(new MyAgentListener())
            .build();
//        MessageWindowChatMemory chatMemory = createChatMemory(chatRequest.getSessionId());
//        chatMemory.set(chatRequest.getContextMessages());
        // 构建监督者 Agent - 管理多个子 Agent
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            .listener(new SupervisorStreamListener(null))
            .subAgents(skillsAgent,searchAgent, sqlAgent, chartGenerationAgent, echartsAgent)
            // 加入历史上下文 - 使用 ChatMemoryProvider 提供持久化的聊天内存
//            .chatMemoryProvider(memoryId -> chatMemory)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        String tokenValue = chatRequest.getTokenValue();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            try {
                String result = supervisor.invoke(chatRequest.getContent());
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
            } catch (Exception e) {
                log.error("Supervisor 执行失败", e);
                SseMessageUtils.sendError(userId, e.getMessage());
            } finally {
                SseMessageUtils.completeConnection(userId, tokenValue);
            }
        });
        return chatRequest.getEmitter();
    }

    /**
     * 支持外部 handler 的对话接口（跨模块调用）
     * 同时发送到 SSE 和外部 handler
     *
     * @param chatRequest     聊天请求
     * @param externalHandler 外部响应处理器（可为 null）
     */
    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler externalHandler) {
        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 3. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("跨模块调用 - 路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        // 4. 获取用户信息
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();

        // 5. 建立 SSE 连接（用于前端监听）
        sseEmitterManager.connect(userId, tokenValue);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 6. 创建组合 handler：同时发送到 SSE 和外部 handler
        StreamingChatResponseHandler combinedHandler = createCombinedHandler(userId, tokenValue, externalHandler);

        // 7. 发起对话
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(chatRequest.getContent(), combinedHandler);
    }

    /**
     * 实现接口默认方法 - 不带 handler 的调用
     */
    @Override
    public SseEmitter chat(ChatRequest chatRequest) {
        return sseChat(chatRequest);
    }


    /**
     * 创建或获取聊天内存实例（缓存机制）
     * 同一个会话ID会返回同一个内存实例，避免重复创建和消息丢失
     *
     * @param memoryId 内存ID（会话ID）
     * @return MessageWindowChatMemory实例
     */
    private MessageWindowChatMemory createChatMemory(Object memoryId) {
        // 先从缓存中获取
        return memoryCache.computeIfAbsent(memoryId, key -> {
            try {
                PersistentChatMemoryStore store = new PersistentChatMemoryStore(chatMessageService);
                return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(DEFAULT_MAX_MESSAGES)
                    .chatMemoryStore(store)
                    .build();
            } catch (Exception e) {
                log.warn("创建聊天内存失败: {}", e.getMessage());
                return null;
            }
        });
    }


    /**
     * 构建上下文消息列表
     * 消息顺序：历史消息 → 当前用户消息（确保 AI 正确理解对话上下文）
     *
     * @param chatRequest 聊天请求
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContextMessages(ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 初始化当前用户消息
        dev.langchain4j.data.message.UserMessage userMessage = dev.langchain4j.data.message.UserMessage.userMessage(chatRequest.getContent());

        // 2. 知识库检索增强 (RAG)
        if (chatRequest.getKnowledgeId() != null) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKnowledgeId()));
            if (knowledgeInfoVo != null) {
                ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
                if (chatModel != null) {
                    log.info("执行高级 RAG 流程: kid={}", chatRequest.getKnowledgeId());

                    // 构建自定义检索器
                    CustomVectorRetriever retriever = new CustomVectorRetriever(
                            knowledgeRetrievalService, knowledgeInfoVo, chatModel);

                    // 构建增强流水线
                    RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                            .contentRetriever(retriever)
                            .build();

                    // 执行增强：编织上下文到 UserMessage
                    Metadata metadata = Metadata.from(userMessage, chatRequest.getSessionId(), new ArrayList<>());
                    AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, metadata);
                    AugmentationResult result = augmentor.augment(augmentationRequest);

                    ChatMessage augmented = result.chatMessage();
                    if (augmented instanceof dev.langchain4j.data.message.UserMessage) {
                        userMessage = (dev.langchain4j.data.message.UserMessage) augmented;
                        log.info("RAG 增强完成，UserMessage 已注入背景知识");
                    }
                }
            }
        }

        // 3. 从数据库查询历史对话消息（放在前面）
        if (chatRequest.getSessionId() != null) {
            MessageWindowChatMemory memory = createChatMemory(chatRequest.getSessionId());
            if (memory != null) {
                List<ChatMessage> historicalMessages = memory.messages();
                if (historicalMessages != null && !historicalMessages.isEmpty()) {
                    messages.addAll(historicalMessages);
                    log.info("已加载 {} 条历史消息用于会话 {}", historicalMessages.size(), chatRequest.getSessionId());
                }
            }
        }

        // 4. 添加经过增强的用户消息（放在最后）
        messages.add(userMessage);

        return messages;
    }

    /**
     * 构建向量查询参数
     */
    private QueryVectorBo buildQueryVectorBo(ChatRequest chatRequest, KnowledgeInfoVo knowledgeInfoVo,
                                             ChatModelVo chatModel) {
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(chatRequest.getContent());
        queryVectorBo.setKid(chatRequest.getKnowledgeId());
        queryVectorBo.setApiKey(chatModel.getApiKey());
        queryVectorBo.setBaseUrl(chatModel.getApiHost());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());

        // 设置重排序参数
        queryVectorBo.setEnableRerank(knowledgeInfoVo.getEnableRerank() != null && knowledgeInfoVo.getEnableRerank() == 1);
        queryVectorBo.setRerankModelName(knowledgeInfoVo.getRerankModel());
        queryVectorBo.setRerankTopN(knowledgeInfoVo.getRerankTopN());
        queryVectorBo.setRerankScoreThreshold(knowledgeInfoVo.getRerankScoreThreshold());

        return queryVectorBo;
    }

    /**
     * 创建标准的响应处理器
     *
     * @param userId      用户ID
     * @param tokenValue  会话令牌
     * @return 标准的流式响应处理器
     */
    protected StreamingChatResponseHandler createResponseHandler(Long userId, String tokenValue,ChatRequest chatRequest) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 将消息片段追加到缓冲区
                messageBuffer.append(partialResponse);

                // 实时发送内容事件到客户端
                SseMessageUtils.sendContent(userId, partialResponse);
                log.info("收到消息片段: {}",  partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 发送完成事件
                    SseMessageUtils.sendDone(userId);

                    // 消息流完成，保存消息到数据库和内存
                    String fullMessage = messageBuffer.toString();

                    if (fullMessage.isEmpty()) {
                          log.warn("接收到空消息");
                    } else {
                        // 保存助手回复消息
                        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), fullMessage, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                    }

                    // 关闭SSE连接
                    SseMessageUtils.completeConnection(userId, tokenValue);
                     log.info("消息结束，已保存到数据库");
                } catch (Exception e) {
                      log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件
                SseMessageUtils.sendError(userId, error.getMessage());
                log.error("流式响应错误: {}", error.getMessage());
            }
        };
    }

    /**
     * 创建组合响应处理器 - 同时发送到 SSE 和外部 handler
     *
     * @param userId          用户ID
     * @param tokenValue      会话令牌
     * @param externalHandler 外部响应处理器（可为 null）
     * @return 组合的流式响应处理器
     */
    protected StreamingChatResponseHandler createCombinedHandler(Long userId, String tokenValue,
                                                                  StreamingChatResponseHandler externalHandler) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 1. 追加到缓冲区
                messageBuffer.append(partialResponse);

                // 2. 发送内容事件到 SSE（前端可通过 SSE 监听）
                SseMessageUtils.sendContent(userId, partialResponse);

                // 3. 转发给外部 handler（Workflow 等模块可处理）
                if (externalHandler != null) {
                    externalHandler.onPartialResponse(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 1. 发送完成事件
                    SseMessageUtils.sendDone(userId);

                    // 2. 关闭 SSE 连接
                    SseMessageUtils.completeConnection(userId, tokenValue);

                    // 3. 转发给外部 handler
                    if (externalHandler != null) {
                        externalHandler.onCompleteResponse(completeResponse);
                    }
                } catch (Exception e) {
                    log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件
                SseMessageUtils.sendError(userId, error.getMessage());
                log.error("流式响应错误: {}", error.getMessage(), error);

                // 转发给外部 handler
                if (externalHandler != null) {
                    externalHandler.onError(error);
                }
            }
        };
    }

    /**
     * ReAct 循环处理：提示词 + 执行本地脚本 + 循环调用模型
     * 模仿 DB-GPT ReAct Agent 的 Thought→Action→Observation 循环
     * <p>
     * 每次对话都会：
     * 1. 从数据库加载历史对话记录，实现上下文延续
     * 2. 收集思考步骤（Thought → Action → Observation）
     * 3. 保存技能加载信息，便于下次打开聊天记录时回调显示
     */
    private void handleReActLoop(ChatRequest chatRequest, ChatModelVo chatModelVo,
                                  AbstractChatService chatService,
                                  StreamingChatModel streamingChatModel,
                                  StreamingChatResponseHandler handler) {
        Long userId = chatRequest.getUserId();
        String tokenValue = chatRequest.getTokenValue();

        List<UserSkillVo> skills = userSkillService.getAvailableSkills(userId);
        String skillsDir = "E:\\working\\ruoyi-ai\\skills\\" + userId;
        String systemPrompt = buildReActSystemPrompt(skills, skillsDir);

        // 收集思考步骤，用于保存到数据库便于回调显示
        List<Map<String, Object>> thinkingSteps = Collections.synchronizedList(new ArrayList<>());

        // 发送技能加载信息（保存到思考步骤中，便于回调显示）
        if (skills != null && !skills.isEmpty()) {
            List<String> skillNames = skills.stream().map(UserSkillVo::getSkillName).collect(java.util.stream.Collectors.toList());
            String skillInfo = skills.stream()
                .map(s -> "名称: " + s.getSkillName() + " (编码: " + s.getSkillCode() + ")"
                    + (s.getDescription() != null ? " - " + s.getDescription() : ""))
                .collect(java.util.stream.Collectors.joining("; "));
            // 发送技能加载事件
            sendReActEvent(userId, "skill", "已加载技能",
                skillNames.size() + " 个技能已就绪: " + skillInfo);
            // 记录到思考步骤
            Map<String, Object> skillStep = new LinkedHashMap<>();
            skillStep.put("type", "skill_loaded");
            skillStep.put("content", skillInfo);
            skillStep.put("toolName", "");
            skillStep.put("toolInput", "");
            skillStep.put("status", "success");
            skillStep.put("timestamp", System.currentTimeMillis());
            thinkingSteps.add(skillStep);
        }

        // 发送初始状态：已选择 MCP 和 Skills
        if (chatRequest.getMcpConfigIds() != null && !chatRequest.getMcpConfigIds().isEmpty()) {
            sendReActEvent(userId, "mcp", "已选择 MCP 配置", chatRequest.getMcpConfigIds().toString());
            Map<String, Object> mcpStep = new LinkedHashMap<>();
            mcpStep.put("type", "mcp_loaded");
            mcpStep.put("content", "已选择 MCP 配置: " + chatRequest.getMcpConfigIds());
            mcpStep.put("toolName", "");
            mcpStep.put("toolInput", "");
            mcpStep.put("status", "success");
            mcpStep.put("timestamp", System.currentTimeMillis());
            thinkingSteps.add(mcpStep);
        }

        CompletableFuture.runAsync(() -> {
            StringBuilder fullContent = new StringBuilder();
            List<ChatMessage> messages = new ArrayList<>();

            // 1. 从数据库加载历史对话记录，实现上下文延续
            List<ChatMessage> historicalMessages = chatRequest.getContextMessages();

            // 2. 根据当前用户输入压缩历史消息，过滤无关内容
            String userContent = chatRequest.getContent();
            List<ChatMessage> compressedMessages = compressHistoryMessages(
                historicalMessages, userContent, chatModelVo);
            if (compressedMessages != null && !compressedMessages.isEmpty()) {
                int originalSize = historicalMessages != null ? historicalMessages.size() : 0;
                int compressedSize = compressedMessages.size();
                if (originalSize != compressedSize) {
                    sendReActEvent(userId, "history", "历史消息压缩",
                        "已从 " + originalSize + " 条压缩到 " + compressedSize + " 条相关消息");
                    Map<String, Object> compressStep = new LinkedHashMap<>();
                    compressStep.put("type", "history_compressed");
                    compressStep.put("content", "已从 " + originalSize + " 条压缩到 " + compressedSize + " 条");
                    compressStep.put("toolName", "");
                    compressStep.put("toolInput", "");
                    compressStep.put("status", "success");
                    compressStep.put("timestamp", System.currentTimeMillis());
                    thinkingSteps.add(compressStep);
                }
                historicalMessages = compressedMessages;
            }

            if (historicalMessages != null && !historicalMessages.isEmpty()) {
                // 发送历史加载事件
                sendReActEvent(userId, "history", "加载历史记录",
                    "已加载 " + historicalMessages.size() + " 条历史消息");
                Map<String, Object> historyStep = new LinkedHashMap<>();
                historyStep.put("type", "history_loaded");
                historyStep.put("content", "已加载 " + historicalMessages.size() + " 条历史消息");
                historyStep.put("toolName", "");
                historyStep.put("toolInput", "");
                historyStep.put("status", "success");
                historyStep.put("timestamp", System.currentTimeMillis());
                thinkingSteps.add(historyStep);
            }

            // 3. 构建消息列表：系统提示词 + (压缩后)历史消息 + 当前用户消息
            messages.add(dev.langchain4j.data.message.SystemMessage.from(systemPrompt));
            if (historicalMessages != null) {
                messages.addAll(historicalMessages);
            }
//            messages.add(dev.langchain4j.data.message.UserMessage.from(chatRequest.getContent()));

            int maxRounds = 20;
            StreamingChatModel streamingModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);

            try {
                for (int round = 0; round < maxRounds; round++) {
                    log.info("ReAct round {}", round + 1);

                    // 流式调用：边推边缓冲，完整响应到达后解析 ReAct 标记
                    StringBuilder roundBuffer = new StringBuilder();
                    CompletableFuture<Boolean> roundDone = new CompletableFuture<>();

                    streamingModel.chat(messages, new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String partialResponse) {
                            roundBuffer.append(partialResponse);
                            // 实时推送内容到前端
                            SseMessageUtils.sendContent(userId, partialResponse);
                        }

                        @Override
                        public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                            roundDone.complete(true);
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("流式响应出错: {}", error.getMessage());
                            roundDone.completeExceptionally(error);
                        }
                    });

                    // 等待本轮流式响应完成
                    String aiText;
                    try {
                        roundDone.get(180, TimeUnit.SECONDS);
                        aiText = roundBuffer.toString();
                    } catch (Exception e) {
                        log.error("ReAct 等待响应超时或出错: {}", e.getMessage());
                        break;
                    }

                    messages.add(dev.langchain4j.data.message.AiMessage.from(aiText));
                    fullContent.append(aiText).append("\n");

                    // 解析 Thought/Action/Action Input
                    String thought = extractTag(aiText, "Thought");
                    String action = extractTag(aiText, "Action");
                    String actionInput = extractTag(aiText, "Action Input");

                    // 记录思考步骤
                    if (thought != null && !thought.isBlank()) {
                        sendReActEvent(userId, "thought", "思考", thought);
                        Map<String, Object> thoughtStep = new LinkedHashMap<>();
                        thoughtStep.put("type", "thought");
                        thoughtStep.put("content", thought);
                        thoughtStep.put("toolName", "");
                        thoughtStep.put("toolInput", "");
                        thoughtStep.put("status", "success");
                        thoughtStep.put("timestamp", System.currentTimeMillis());
                        thinkingSteps.add(thoughtStep);
                    }

                    if (round + 1 == maxRounds){
                        String end_text = "调用工具达到最大回合。" + "\n\n";
                        SseMessageUtils.sendContent(userId, end_text);
                        saveChatMessageWithThinkingSteps(userId, chatRequest.getSessionId(),
                            end_text, RoleType.ASSISTANT.getName(), chatRequest.getModel(), thinkingSteps);
                        SseMessageUtils.sendDone(userId);
                    }
                    if (action == null || action.isBlank() || "None".equalsIgnoreCase(action.trim())) {
                        log.info("ReAct 完成，Action=None");
                        // 记录完成步骤
                        Map<String, Object> finalStep = new LinkedHashMap<>();
                        finalStep.put("type", "final_answer");
                        finalStep.put("content", "任务完成");
                        finalStep.put("toolName", "");
                        finalStep.put("toolInput", "");
                        finalStep.put("status", "success");
                        finalStep.put("timestamp", System.currentTimeMillis());
                        thinkingSteps.add(finalStep);
                        // 流式推送 AI 完整回复
                        SseMessageUtils.sendContent(userId, aiText + "\n\n");
                        // 只保存最后的结果。
                        saveChatMessageWithThinkingSteps(userId, chatRequest.getSessionId(),
                            aiText, RoleType.ASSISTANT.getName(), chatRequest.getModel(), thinkingSteps);
                        SseMessageUtils.sendDone(userId);
                        break;
                    }

                    // 发送工具调用事件
                    sendReActEvent(userId, "tool_call", action, actionInput != null ? actionInput : "{}");
                    // 记录工具调用步骤
                    Map<String, Object> toolStep = new LinkedHashMap<>();
                    toolStep.put("type", "tool_call");
                    toolStep.put("toolName", action);
                    toolStep.put("toolInput", actionInput != null ? actionInput : "{}");
                    toolStep.put("content", "");
                    toolStep.put("status", "running");
                    toolStep.put("timestamp", System.currentTimeMillis());
                    thinkingSteps.add(toolStep);

                    // 执行工具
                    String observation = executeReActAction(action, actionInput, skills, skillsDir);

                    // 更新工具调用状态为完成
                    toolStep.put("status", "success");

                    // 发送观察结果
                    String obsPreview = observation.length() > 500 ? observation.substring(0, 500) + "..." : observation;
                    sendReActEvent(userId, "observation", "工具执行结果", obsPreview);
                    // 记录观察步骤
                    Map<String, Object> obsStep = new LinkedHashMap<>();
                    obsStep.put("type", "observation");
                    obsStep.put("content", obsPreview);
                    obsStep.put("toolName", action);
                    obsStep.put("toolInput", actionInput != null ? actionInput : "{}");
                    obsStep.put("status", "success");
                    obsStep.put("timestamp", System.currentTimeMillis());
                    thinkingSteps.add(obsStep);
                    messages.add(dev.langchain4j.data.message.UserMessage.from("Observation: " + observation));
                }

            } catch (Exception e) {
                log.error("ReAct 执行失败", e);
                // 记录错误步骤
                Map<String, Object> errorStep = new LinkedHashMap<>();
                errorStep.put("type", "error");
                errorStep.put("content", e.getMessage());
                errorStep.put("toolName", "");
                errorStep.put("toolInput", "");
                errorStep.put("status", "error");
                errorStep.put("timestamp", System.currentTimeMillis());
                thinkingSteps.add(errorStep);
                SseMessageUtils.sendContent(userId, "\n\n❌ 执行失败: " + e.getMessage());
                SseMessageUtils.sendDone(userId);
                // 即使出错也保存思考步骤
                saveChatMessageWithThinkingSteps(userId, chatRequest.getSessionId(),
                    fullContent.length() > 0 ? fullContent.toString() : "执行失败: " + e.getMessage(),
                    RoleType.ASSISTANT.getName(), chatRequest.getModel(), thinkingSteps);
            } finally {
                SseMessageUtils.completeConnection(userId, tokenValue);
            }
        });
    }

    /**
     * 最近保留的消息对数（不参与压缩，直接保留完整内容）
     */
    private static final int RECENT_PAIR_COUNT = 2;
    /**
     * 历史消息压缩的触发阈值：总对数超过此值时触发压缩
     */
    private static final int HISTORY_COMPRESS_THRESHOLD = 4;

    /**
     * 根据当前用户输入，对历史消息进行语义相关性压缩。
     * <p>
     * 策略：保留最近 {@value RECENT_PAIR_COUNT} 对完整对话，将其之前的更早历史用 LLM 压缩为摘要。
     * <ul>
     *   <li>总对数 ≤ 阈值 → 全量返回，不做压缩</li>
     *   <li>总对数 > 阈值 → 前 N-2 对压缩为摘要 + 保留最近 2 对原文</li>
     *   <li>压缩失败 → 降级保留最近 2 对</li>
     * </ul>
     *
     * @param historyMessages 原始历史消息列表
     * @param userContent     当前用户输入
     * @param chatModelVo     模型配置（用于构建压缩用的 LLM）
     * @return 压缩后的消息列表：[压缩摘要] + [最近2对原文]
     */
    private List<ChatMessage> compressHistoryMessages(List<ChatMessage> historyMessages,
                                                       String userContent,
                                                       ChatModelVo chatModelVo) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return historyMessages;
        }
        int pairCount = countMessagePairs(historyMessages);
        if (pairCount <= HISTORY_COMPRESS_THRESHOLD) {
            log.debug("历史消息仅 {} 对，无需压缩", pairCount);
            return historyMessages;
        }

        // 分离：最近 N 对 + 老历史
        SplitResult split = splitRecentAndOld(historyMessages, RECENT_PAIR_COUNT);
        List<ChatMessage> oldHistory = split.oldMessages;
        List<ChatMessage> recentPairs = split.recentMessages;

        if (oldHistory.isEmpty()) {
            return historyMessages;
        }

        try {
            String compressed = llmCompressHistory(oldHistory, userContent, chatModelVo);
            List<ChatMessage> result = new ArrayList<>();
            if (compressed != null && !compressed.isBlank()) {
                result.add(dev.langchain4j.data.message.SystemMessage.from(
                    "【历史对话摘要 - 以下为之前对话的关键信息，供你参考】\n" + compressed));
                log.info("历史消息压缩完成: {} 对 → 摘要（{} 字符）+ 最近 {} 对原文",
                    pairCount, compressed.length(), recentPairs.size() / 2);
            } else {
                log.debug("压缩摘要为空，仅保留最近 {} 对", RECENT_PAIR_COUNT);
            }
            // 拼接：压缩摘要 + 最近原文
            result.addAll(recentPairs);
            return result;
        } catch (Exception e) {
            log.error("历史消息压缩失败: {}", e.getMessage());
            return recentPairs.isEmpty() ? new ArrayList<>() : new ArrayList<>(recentPairs);
        }
    }

    /**
     * 消息分割结果
     */
    private static class SplitResult {
        final List<ChatMessage> oldMessages;
        final List<ChatMessage> recentMessages;

        SplitResult(List<ChatMessage> oldMessages, List<ChatMessage> recentMessages) {
            this.oldMessages = oldMessages;
            this.recentMessages = recentMessages;
        }
    }

    /**
     * 按消息对分离：前 N-{@code recentPairs} 对为老历史，最后 {@code recentPairs} 对为最近对话。
     * 每对 = 1 条 UserMessage + 1 条 AiMessage。
     */
    private SplitResult splitRecentAndOld(List<ChatMessage> messages, int recentPairs) {
        int totalPairs = countMessagePairs(messages);
        if (totalPairs <= recentPairs) {
            return new SplitResult(new ArrayList<>(), new ArrayList<>(messages));
        }
        int pairsToKeepOld = totalPairs - recentPairs;
        List<ChatMessage> oldList = new ArrayList<>();
        List<ChatMessage> recentList = new ArrayList<>();
        int pairCnt = 0;
        for (ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                pairCnt++;
            }
            if (pairCnt <= pairsToKeepOld) {
                oldList.add(msg);
            } else {
                recentList.add(msg);
            }
        }
        return new SplitResult(oldList, recentList);
    }

    /**
     * 使用 LLM 调用压缩历史消息
     * 从历史对话中提取与当前用户输入相关的内容
     */
    private String llmCompressHistory(List<ChatMessage> historyMessages, String userContent,
                                       ChatModelVo chatModelVo) {
        // 构建压缩用的轻量级模型（使用相同配置但降低超时）
        OpenAiChatModel compressModel = OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(Duration.ofSeconds(60))
            .maxTokens(1024)
            .temperature(0.0)
            .build();

        // 构建压缩提示词
        StringBuilder historyText = new StringBuilder();
        int index = 1;
        for (ChatMessage msg : historyMessages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                historyText.append("Q").append(index / 2 + 1).append(": ")
                    .append(((dev.langchain4j.data.message.UserMessage) msg).singleText()).append("\n");
            } else if (msg instanceof dev.langchain4j.data.message.AiMessage) {
                String aiText = ((dev.langchain4j.data.message.AiMessage) msg).text();
                // 截断过长的 AI 回复，避免压缩提示词过长
                if (aiText.length() > 500) {
                    aiText = aiText.substring(0, 500) + "...";
                }
                historyText.append("A").append(index / 2 + 1).append(": ").append(aiText).append("\n");
            }
            index++;
        }

        List<ChatMessage> compressMessages = new ArrayList<>();
        compressMessages.add(dev.langchain4j.data.message.SystemMessage.from(
            "你是一名对话历史压缩助手。请根据用户的当前问题，从历史对话中提取**所有相关**的内容，输出简洁摘要。\n\n"
            + "规则：\n"
            + "1. 只提取与当前问题语义相关的对话内容\n"
            + "2. 完全不相关的对话直接丢弃，不要提及\n"
            + "3. 用简洁的中文摘要形式输出，保留关键事实、数字、决策、偏好\n"
            + "4. 输出格式：<summary>相关历史摘要</summary>\n"
            + "5. 如果没有任何相关的历史对话，输出：<summary>无相关内容</summary>"));
        compressMessages.add(dev.langchain4j.data.message.UserMessage.from(
            "当前用户问题: " + userContent + "\n\n"
            + "历史对话记录:\n" + historyText.toString()));

        try {
            dev.langchain4j.model.chat.response.ChatResponse response = compressModel.chat(compressMessages);
            String result = response.aiMessage().text();
            // 提取 <summary> 标签内容
            int start = result.indexOf("<summary>");
            int end = result.indexOf("</summary>");
            if (start >= 0 && end > start) {
                String summary = result.substring(start + 9, end).trim();
                if ("无相关内容".equals(summary)) {
                    // 无相关内容，保留最近 N 对
                    log.debug("压缩结果: 无相关内容，保留最近 {} 对", HISTORY_COMPRESS_THRESHOLD);
                    return null; // 返回 null 让上层降级处理
                }
                return summary;
            }
            // 没有标签，直接返回全部内容
            return result.trim();
        } catch (Exception e) {
            log.error("LLM 压缩调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 统计消息对数（一个 UserMessage + 一个 AiMessage 为一对）
     */
    private int countMessagePairs(List<ChatMessage> messages) {
        int pairs = 0;
        for (ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                pairs++;
            }
        }
        return pairs;
    }

    /**
     * 从数据库加载会话历史消息，转换为 LangChain4j ChatMessage 列表
     * 用于 ReAct 循环中实现上下文延续
     *
     * @param sessionId 会话ID
     * @return 历史消息列表
     */
    private List<ChatMessage> loadHistoryMessages(Long sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }
        try {
            return chatMessageService.getMessagesBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("加载历史消息失败: sessionId={}, error={}", sessionId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 保存聊天消息，附带思考步骤 JSON
     *
     * @param userId         用户ID
     * @param sessionId      会话ID
     * @param content        消息内容
     * @param role           角色
     * @param modelName      模型名称
     * @param thinkingSteps  思考步骤列表
     */
    private void saveChatMessageWithThinkingSteps(Long userId, Long sessionId, String content,
                                                   String role, String modelName,
                                                   List<Map<String, Object>> thinkingSteps) {
        try {
            String thinkingStepsJson = null;
            if (thinkingSteps != null && !thinkingSteps.isEmpty()) {
                thinkingStepsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(thinkingSteps);
            }
            chatMessageService.saveChatMessage(userId, sessionId, content, role, modelName, thinkingStepsJson);
            log.info("保存聊天消息成功，sessionId: {}, 思考步骤: {} 条", sessionId, thinkingSteps.size());
        } catch (Exception e) {
            log.error("保存聊天消息失败: {}", e.getMessage());
            // 降级：不带思考步骤保存
            chatMessageService.saveChatMessage(userId, sessionId, content, role, modelName);
        }
    }

    /** 发送结构化 ReAct 事件 */
    private void sendReActEvent(Long userId, String eventType, String title, String content) {
        SseEventDto dto = SseEventDto.builder()
            .event(eventType)
            .content("{\"title\":\"" + escapeJson(title) + "\",\"content\":\"" + escapeJson(content) + "\"}")
            .build();
        SseMessageUtils.sendEvent(userId, dto);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /**
     * 执行 ReAct Action
     */
    private String executeReActAction(String action, String actionInput,
                                       List<UserSkillVo> skills, String skillsDir) {
        try {
            String act = action.trim();

            // 匹配技能执行
            if ("execute_skill_script".equals(act) || "execute_script".equals(act)) {
                return executeSkillScript(actionInput, skills, skillsDir);
            }

            // 匹配 shell 命令
            if ("shell".equals(act) || "bash".equals(act) || "execute".equals(act)) {
                return executeShell(actionInput);
            }

            // 匹配读取文件
            if ("read_file".equals(act) || "cat".equals(act)) {
                return readFile(actionInput);
            }

            // 默认：尝试作为技能名称执行
            return executeSkillByName(act, actionInput, skills, skillsDir);

        } catch (Exception e) {
            log.error("执行 Action 失败: {} - {}", action, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private String executeSkillScript(String actionInput, List<UserSkillVo> skills, String skillsDir) {
        try {
            var params = new com.fasterxml.jackson.databind.ObjectMapper().readValue(actionInput, java.util.Map.class);
            String skillName = (String) params.get("skill_name");
            String scriptName = (String) params.getOrDefault("script_name", "main.py");
            var args = (java.util.Map<String, Object>) params.getOrDefault("args", java.util.Collections.emptyMap());

            // 查找技能目录
            Path skillPath = findSkillPath(skillName, skills, skillsDir);
            if (skillPath == null) return "Skill not found: " + skillName;

            Path scriptFile = skillPath.resolve("scripts").resolve(scriptName);
            if (!Files.exists(scriptFile)) {
                scriptFile = skillPath.resolve(scriptName);
            }
            if (!Files.exists(scriptFile)) {
                return "Script not found: " + scriptName + " in " + skillPath;
            }

            // 构建命令
            List<String> cmd = new ArrayList<>();
            String ext = scriptName.substring(scriptName.lastIndexOf('.'));
            if (".py".equals(ext)) {
                cmd.add("D:\\python\\python.exe");
            } else if (".js".equals(ext)) {
                cmd.add("node");
            } else {
                cmd.add("bash");
            }
            cmd.add(scriptFile.toString());
            if (args != null && !args.isEmpty()) {
                cmd.add(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(args));
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);;
            p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "Script execution error: " + e.getMessage();
        }
    }

    private String executeShell(String actionInput) {
        try {
            String cmd;
            try {
                var params = new com.fasterxml.jackson.databind.ObjectMapper().readValue(actionInput, java.util.Map.class);
                cmd = (String) params.getOrDefault("command", params.getOrDefault("code", actionInput));
            } catch (Exception e) {
                cmd = actionInput;
            }

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmd);
            pb.redirectErrorStream(true);
            // 设置控制台代码页为 UTF-8 (chcp 65001)，避免中文乱码
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUTF8", "1");
            // 在命令前加 chcp 65001 切换控制台编码，确保 cmd.exe 输出中文不乱码
            ProcessBuilder wrappedPb = new ProcessBuilder("cmd", "/c", "chcp 65001 >nul && " + cmd);
            wrappedPb.redirectErrorStream(true);
            wrappedPb.environment().putAll(env);
            Process p = wrappedPb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "Shell error: " + e.getMessage();
        }
    }

    private String readFile(String actionInput) {
        try {
            String path;
            try {
                var params = new com.fasterxml.jackson.databind.ObjectMapper().readValue(actionInput, java.util.Map.class);
                path = (String) params.getOrDefault("path", params.getOrDefault("file", actionInput));
            } catch (Exception e) {
                path = actionInput;
            }
            return Files.readString(Path.of(path));
        } catch (Exception e) {
            return "Read file error: " + e.getMessage();
        }
    }

    private String executeSkillByName(String skillName, String actionInput,
                                       List<UserSkillVo> skills, String skillsDir) {
        // 尝试按技能名匹配
        Path skillPath = findSkillPath(skillName, skills, skillsDir);
        if (skillPath == null) {
            return "No matching skill or command for: " + skillName;
        }

        Path scriptFile = skillPath.resolve("scripts").resolve("main.py");
        if (!Files.exists(scriptFile)) {
            return "No main script found in skill: " + skillName;
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("D:\\python\\python.exe");
            cmd.add(scriptFile.toString());
            if (actionInput != null && !actionInput.isBlank()) {
                cmd.add(actionInput);
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "Skill execution error: " + e.getMessage();
        }
    }

    private Path findSkillPath(String skillName, List<UserSkillVo> skills, String skillsDir) {
        for (UserSkillVo s : skills) {
            if (s.getSkillName().equals(skillName) || s.getSkillCode().equals(skillName)) {
                return Path.of(skillsDir, s.getSkillCode());
            }
        }
        // 尝试直接路径
        Path direct = Path.of(skillsDir, skillName);
        return Files.exists(direct) ? direct : null;
    }

    /**
     * 从文本中提取标签值
     */
    private String extractTag(String text, String tag) {
        for (String prefix : new String[]{tag + ": ", tag + ":"}) {
            int idx = text.indexOf(prefix);
            if (idx >= 0) {
                int start = idx + prefix.length();
                int end = text.indexOf("\n", start);
                if (end < 0) end = text.length();
                return text.substring(start, end).trim();
            }
        }
        return null;
    }

    private String buildReActSystemPrompt(List<UserSkillVo> skills, String skillsDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 CaoBin-AI 智能助手，能够通过执行命令来帮助用户完成任务。\n");
        sb.append("请使用与用户输入相同的语言进行回复。\n\n");

        sb.append("## 自主决策原则\n");
        sb.append("1. 仔细分析用户的任务要求。\n");
        sb.append("2. 对于每一步，输出 Thought → Action → Action Input。\n");
        sb.append("3. 等待系统返回 Observation 后再决定下一步。\n");
        sb.append("4. 任务完成后输出 Action: None 并给出最终答案。\n\n");


        if (skills != null && !skills.isEmpty()) {
            sb.append("## 可用技能\n");
            sb.append("技能目录: ").append(skillsDir).append("\n");
            for (UserSkillVo s : skills) {
                String scriptPath = skillsDir + "/" + s.getSkillCode() + "/scripts/main.py";
                sb.append("- **").append(s.getSkillName()).append("** (`").append(s.getSkillCode()).append("`)");
                sb.append(" 脚本: ").append(scriptPath);
                if (s.getDescription() != null) sb.append(" - ").append(s.getDescription());
                sb.append("\n");
            }
            sb.append("\n执行技能: Action: execute_skill_script\n");
            sb.append("Action Input: {\"skill_name\": \"技能名称\", \"script_name\": \"main.py\", \"args\": {\"param\": \"value\"}}\n\n");

            // 读取每个技能的 SKILL.md 内容并添加到提示词
            for (UserSkillVo s : skills) {
                String skillMdContent = readSkillMd(skillsDir, s.getSkillCode());
                if (skillMdContent != null && !skillMdContent.isBlank()) {
                    sb.append("### ").append(s.getSkillName()).append(" 技能详情\n");
                    sb.append(skillMdContent).append("\n\n");
                }
            }
        }

        sb.append("## 可用工具\n");
        sb.append("- shell: 执行系统命令\n");
        sb.append("- read_file: 读取文件内容\n");
        sb.append("- execute_skill_script: 执行技能脚本\n\n");

        sb.append("## 执行环境\n");
        sb.append("- 执行python环境: D:\\python\\python.exe\n");


        sb.append("## ReAct 输出格式\n");
        sb.append("Thought: 分析当前状态\n");
        sb.append("Action: 工具名称\n");
        sb.append("Action Input: JSON 参数\n");
        sb.append("收到 Observation 后继续。完成时输出 Action: None。\n");

        return sb.toString();
    }

    /**
     * 读取技能目录下的 SKILL.md 文件内容
     *
     * @param skillsDir 技能根目录
     * @param skillCode 技能编码
     * @return SKILL.md 内容，如果文件不存在或读取失败则返回 null
     */
    private String readSkillMd(String skillsDir, String skillCode) {
        try {
            Path skillMdPath = Path.of(skillsDir, skillCode, "SKILL.md");
            if (Files.exists(skillMdPath)) {
                String content = Files.readString(skillMdPath);
                // 去除 YAML front matter（--- 之间的元数据），保留正文内容
                if (content.startsWith("---")) {
                    int endIndex = content.indexOf("---", 3);
                    if (endIndex > 0) {
                        content = content.substring(endIndex + 3).trim();
                    }
                }
                return content;
            }
        } catch (Exception e) {
            log.warn("读取 SKILL.md 失败: {}/{} - {}", skillsDir, skillCode, e.getMessage());
        }
        return null;
    }
}

