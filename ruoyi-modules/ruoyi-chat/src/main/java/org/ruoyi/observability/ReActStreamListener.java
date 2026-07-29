package org.ruoyi.observability;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.utils.SseMessageUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReActAgent 流式监听器
 * 将 ReAct 的 Thought → Action → Observation 循环实时推送到前端 SSE
 * 同时收集思考步骤，可序列化为 JSON 保存到数据库
 *
 * @author ruoyi
 */
@Slf4j
public class ReActStreamListener implements dev.langchain4j.agentic.observability.AgentListener {

    private final Long userId;
    private final AtomicReference<String> finalOutputRef = new AtomicReference<>();
    private final List<Map<String, Object>> thinkingSteps = new ArrayList<>();
    private long startTime = System.currentTimeMillis();

    public ReActStreamListener(Long userId) {
        this.userId = userId;
    }

    public String getFinalOutput() {
        return finalOutputRef.get();
    }

    /**
     * 获取思考步骤的 JSON 字符串
     */
    public String getThinkingStepsJson() {
        if (thinkingSteps.isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(thinkingSteps);
        } catch (Exception e) {
            log.warn("序列化思考步骤失败", e);
            return null;
        }
    }

    /**
     * 记录一个思考步骤
     */
    private void addStep(String type, String content, String toolName, String toolInput, String status) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("type", type);       // thought / tool_call / observation
        step.put("content", content);
        step.put("toolName", toolName != null ? toolName : "");
        step.put("toolInput", toolInput != null ? toolInput : "");
        step.put("status", status);   // pending / running / success / error
        step.put("timestamp", System.currentTimeMillis());
        thinkingSteps.add(step);
    }

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        AgentInstance agent = agentRequest.agent();
        log.info("【ReAct】Agent 开始调用: {} ({})", agent.name(), agent.description());

        if (!"invoke".equals(agent.agentId())) {
            String desc = agent.description();
            String msg = String.format("\n\n🔧 **调用工具: %s**\n> %s\n\n", agent.name(), desc != null ? desc : "");
            SseMessageUtils.sendContent(userId, msg);

            addStep("tool_call", desc, agent.name(), "", "running");
        }
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        AgentInstance agent = agentResponse.agent();
        Object output = agentResponse.output();
        String outputStr = output != null ? output.toString() : "";

        log.info("【ReAct】Agent 调用完成: {}, 输出长度: {}", agent.name(), outputStr.length());

        if (!"invoke".equals(agent.agentId()) && !outputStr.isEmpty()) {
            // 截断过长的输出
            String display = outputStr.length() > 2000 ? outputStr.substring(0, 2000) + "..." : outputStr;
            String msg = String.format("\n📋 **工具结果** (%s):\n```\n%s\n```\n", agent.name(), display);
            SseMessageUtils.sendContent(userId, msg);

            // 更新工具调用步骤状态
            if (!thinkingSteps.isEmpty()) {
                Map<String, Object> lastStep = thinkingSteps.get(thinkingSteps.size() - 1);
                if ("running".equals(lastStep.get("status"))) {
                    lastStep.put("status", "success");
                    lastStep.put("content", outputStr);
                }
            }
        }

        // 捕获主 Agent 的最终输出
        if ("invoke".equals(agent.agentId()) && !outputStr.isEmpty()) {
            finalOutputRef.set(outputStr);
        }
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        AgentInstance agent = error.agent();
        Throwable throwable = error.error();

        log.error("【ReAct】Agent 执行错误: {} - {}", agent.name(), throwable.getMessage());
        SseMessageUtils.sendContent(userId,
            String.format("\n\n❌ 执行错误 (%s): %s", agent.name(), throwable.getMessage()));

        addStep("tool_call", throwable.getMessage(), agent.name(), "", "error");
    }

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        log.info("【ReAct】AgenticScope 已创建: {}", agenticScope.memoryId());
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        log.info("【ReAct】AgenticScope 即将销毁, 总调用次数: {}, 耗时: {}ms",
            agenticScope.agentInvocations().size(),
            System.currentTimeMillis() - startTime);
    }
}