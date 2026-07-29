package org.ruoyi.service.chat.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ReAct 聊天处理器
 * 使用 StreamingChatModel + AiServices 实现流式 SSE 输出
 * 同时解析 Thought → Action → Observation 思考过程
 *
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReActChatHandler {

    private final IChatMessageService chatMessageService;
    private final ChatServiceFactory chatServiceFactory;

    /**
     * 处理 ReAct 聊天
     */
    public void handle(ChatRequest chatRequest, ChatModelVo chatModelVo, ToolProvider toolProvider) {
        Long userId = chatRequest.getUserId();
        String tokenValue = chatRequest.getTokenValue();
        String providerCode = chatModelVo.getProviderCode();
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        List<Map<String, Object>> thinkingSteps = Collections.synchronizedList(new ArrayList<>());
        StringBuilder fullContent = new StringBuilder();

        SseMessageUtils.sendContent(userId, "🤔 **ReAct 智能体正在思考...**\n\n");

        try {
            StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
            List<ChatMessage> contextMessages = chatRequest.getContextMessages();

            // 注入 ReAct 系统提示词
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(dev.langchain4j.data.message.SystemMessage.from(
                "你是一个具备自主决策能力的 ReAct AI 助手。\n\n" +
                "## 核心原则\n" +
                "1. 当需要调用工具时，严格按以下格式输出：\n" +
                "   Thought: <思考过程>\n" +
                "   Action: <工具名称>\n" +
                "   Action Input: <JSON参数>\n" +
                "2. 工具调用结果会以 Observation 形式返回，收到后评估是否继续。\n" +
                "3. 任务完成时输出：\n" +
                "   Thought: 任务完成\n" +
                "   Final Answer: <最终自然语言答案>\n" +
                "4. 如果不需要工具，直接给出答案。"
            ));
            messages.addAll(contextMessages);

            if (toolProvider != null) {
                // AiServices 需要 chatModel（工具调用）+ streamingChatModel（流式输出）
                var assistant = dev.langchain4j.service.AiServices.builder(ReActAssistant.class)
                    .streamingChatModel(streamingChatModel)
                    .chatModel(chatService.buildChatModel(chatModelVo))
                    .systemMessage(messages.get(0).toString())
                    .toolProvider(toolProvider)
                    .build();

                String userMessage = chatRequest.getContent();
                assistant.chat(userMessage, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        fullContent.append(partialResponse);
                        SseMessageUtils.sendContent(userId, partialResponse);
                    }
                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        finish(userId, tokenValue, chatRequest, fullContent.toString(), thinkingSteps);
                    }
                    @Override
                    public void onError(Throwable error) {
                        handleError(userId, tokenValue, chatRequest, error.getMessage(), thinkingSteps);
                    }
                });
            } else {
                // 无工具：直接流式对话
                String userMessage = chatRequest.getContent();
                streamingChatModel.chat(userMessage, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        fullContent.append(partialResponse);
                        SseMessageUtils.sendContent(userId, partialResponse);
                    }
                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        finish(userId, tokenValue, chatRequest, fullContent.toString(), thinkingSteps);
                    }
                    @Override
                    public void onError(Throwable error) {
                        handleError(userId, tokenValue, chatRequest, error.getMessage(), thinkingSteps);
                    }
                });
            }
        } catch (Exception e) {
            log.error("ReAct 初始化失败", e);
            handleError(userId, tokenValue, chatRequest, e.getMessage(), thinkingSteps);
        }
    }

    private void finish(Long userId, String tokenValue, ChatRequest chatRequest,
                         String rawContent, List<Map<String, Object>> steps) {
        parseThinkingSteps(rawContent, steps);
        String finalOutput = extractFinalAnswer(rawContent);
        SseMessageUtils.sendContent(userId, "\n\n---\n\n");
        SseMessageUtils.sendDone(userId);
        saveMessage(userId, chatRequest, finalOutput, steps);
        SseMessageUtils.completeConnection(userId, tokenValue);
    }

    private void handleError(Long userId, String tokenValue, ChatRequest chatRequest,
                              String errorMsg, List<Map<String, Object>> steps) {
        SseMessageUtils.sendContent(userId, "\n\n❌ 错误: " + errorMsg);
        SseMessageUtils.sendDone(userId);
        saveMessage(userId, chatRequest, "错误: " + errorMsg, steps);
        SseMessageUtils.completeConnection(userId, tokenValue);
    }

    private void saveMessage(Long userId, ChatRequest req, String content,
                              List<Map<String, Object>> steps) {
        try {
            String json = steps.isEmpty() ? null :
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(steps);
            chatMessageService.saveChatMessage(userId, req.getSessionId(), content,
                RoleType.ASSISTANT.getName(), req.getModel(), json);
        } catch (Exception e) {
            log.error("保存消息失败", e);
        }
    }

    void parseThinkingSteps(String content, List<Map<String, Object>> steps) {
        if (content == null) return;
        for (String line : content.split("\n")) {
            String t = line.trim();
            if (t.startsWith("Thought:") || t.startsWith("Thought：")) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("type", "thought"); s.put("content", afterColon(t));
                s.put("toolName", ""); s.put("toolInput", "");
                s.put("status", "success"); s.put("timestamp", System.currentTimeMillis());
                steps.add(s);
            } else if (t.startsWith("Action:") || t.startsWith("Action：")) {
                String name = afterColon(t);
                if ("None".equalsIgnoreCase(name)) continue;
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("type", "tool_call"); s.put("toolName", name);
                s.put("content", ""); s.put("toolInput", "");
                s.put("status", "running"); s.put("timestamp", System.currentTimeMillis());
                steps.add(s);
            } else if (t.startsWith("Action Input:") || t.startsWith("Action Input：")) {
                if (!steps.isEmpty()) {
                    Map<String, Object> last = steps.get(steps.size() - 1);
                    last.put("toolInput", afterColon(t));
                    last.put("status", "success");
                }
            } else if (t.startsWith("Observation:") || t.startsWith("Observation：")) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("type", "observation"); s.put("content", afterColon(t));
                s.put("toolName", ""); s.put("toolInput", "");
                s.put("status", "success"); s.put("timestamp", System.currentTimeMillis());
                steps.add(s);
            }
        }
    }

    private String afterColon(String line) {
        int i = line.indexOf(':');
        if (i >= 0 && i + 1 < line.length()) return line.substring(i + 1).trim();
        return line;
    }

    private String extractFinalAnswer(String content) {
        if (content == null || content.isBlank()) return content;
        for (String kw : new String[]{"Final Answer:", "Final Answer：", "最终答案:", "最终答案："}) {
            int i = content.lastIndexOf(kw);
            if (i >= 0) {
                String ans = content.substring(i + kw.length()).trim();
                return ans.isEmpty() ? content : ans;
            }
        }
        return content.replaceAll("(?m)^(Thought|Action|Action Input|Observation)[：:].*\\n?", "").trim();
    }

    interface ReActAssistant {
        @UserMessage("{{userMessage}}")
        void chat(@V("userMessage") String userMessage, StreamingChatResponseHandler handler);
    }
}
