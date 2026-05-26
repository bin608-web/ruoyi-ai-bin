package org.ruoyi.service.embed.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ModalityType;
import org.ruoyi.service.embed.BaseEmbedModelService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Component("custom_api")
public class CustomAiEmbeddingProvider implements BaseEmbedModelService {
    protected ChatModelVo chatModelVo;
    // 初始化ObjectMapper用于JSON序列化/反序列化
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // 初始化OkHttpClient（可根据需要调整超时参数）
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
    // 定义JSON媒体类型
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    @Override
    public Set<ModalityType> getSupportedModalities() {
        return Set.of(ModalityType.TEXT);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        // 1. 构建请求参数
        EmbeddingRequest requestBody = buildEmbeddingRequest(textSegments);

        try {
            // 2. 序列化请求体为JSON字符串
            String requestJson = OBJECT_MAPPER.writeValueAsString(requestBody);

            // 3. 构建OkHttp请求
            Request request = new Request.Builder()
                .url(buildApiUrl())
                .header("Authorization", "Bearer " + chatModelVo.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .build();

            // 4. 执行请求并处理响应
            try (okhttp3.Response httpResponse = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!httpResponse.isSuccessful()) {
                    throw new IOException("请求嵌入接口失败: " + httpResponse.code() + " " + httpResponse.message());
                }

                ResponseBody responseBody = httpResponse.body();
                if (responseBody == null) {
                    throw new IOException("嵌入接口响应体为空");
                }

                // 5. 解析响应JSON
                JsonNode responseJson = OBJECT_MAPPER.readTree(responseBody.string());
                List<Embedding> embeddings = parseEmbeddings(responseJson);

                // 6. 构建langchain4j的Response对象返回
                return Response.from(embeddings);

            } catch (IOException e) {
                throw new RuntimeException("调用嵌入接口失败", e);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化嵌入请求参数失败", e);
        }
    }

    /**
     * 构建嵌入请求参数
     */
    private EmbeddingRequest buildEmbeddingRequest(List<TextSegment> textSegments) {
        // 提取所有文本段的内容
        List<String> texts = new ArrayList<>();
        for (TextSegment segment : textSegments) {
            texts.add(segment.text());
        }

        return new EmbeddingRequest()
            .setInput(texts)
            .setModel(chatModelVo.getModelName())
            .setDimensions(chatModelVo.getModelDimension());
    }

    /**
     * 构建API请求URL（兼容OpenAI格式）
     */
    private String buildApiUrl() {
        String baseUrl = chatModelVo.getApiHost();
        // 确保baseUrl不以/结尾，拼接embeddings接口路径
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/embeddings";
    }

    /**
     * 解析响应中的嵌入向量
     */
    private List<Embedding> parseEmbeddings(JsonNode responseJson) {
        List<Embedding> embeddings = new ArrayList<>();
        JsonNode dataArray = responseJson.get("data");

        if (dataArray == null || !dataArray.isArray()) {
            throw new RuntimeException("嵌入响应格式错误: 缺少data数组");
        }

        // 遍历每个嵌入结果
        for (JsonNode dataNode : dataArray) {
            JsonNode embeddingArray = dataNode.get("embedding");
            if (embeddingArray == null || !embeddingArray.isArray()) {
                throw new RuntimeException("嵌入响应格式错误: 缺少embedding数组");
            }

            // 将JSON数组转换为float数组
            float[] vector = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                vector[i] = (float) embeddingArray.get(i).asDouble();
            }

            // 创建langchain4j的Embedding对象
            embeddings.add(Embedding.from(vector));
        }

        return embeddings;
    }

    /**
     * 嵌入请求参数封装类（适配OpenAI Embeddings API格式）
     */
    public static class EmbeddingRequest {
        private List<String> input;
        private String model;
        private Integer dimensions;

        public List<String> getInput() {
            return input;
        }

        public EmbeddingRequest setInput(List<String> input) {
            this.input = input;
            return this;
        }

        public String getModel() {
            return model;
        }

        public EmbeddingRequest setModel(String model) {
            this.model = model;
            return this;
        }

        public Integer getDimensions() {
            return dimensions;
        }

        public EmbeddingRequest setDimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }
    }
}
