package com.liuhang.langchain4j.day5;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhang.langchain4j.ApiKeys;
import dev.langchain4j.agent.tool.*;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 工具（函数调用）
 * 一些 LLM 除了生成文本外，还可以触发操作。
 * 有一个被称为"工具"或"函数调用"的概念。 它允许 LLM 在必要时调用一个或多个可用的工具，通常由开发者定义。
 * 工具可以是任何东西：网络搜索、调用外部 API 或执行特定代码片段等。
 *
 * 两个抽象级别
 * LangChain4j 提供了两个抽象级别来使用工具：
 * 低级别，使用 ChatLanguageModel 和 ToolSpecification API
 * 高级别，使用 AI 服务和带有 @Tool 注解的 Java 方法
 */
public class LowLevelToolTest {

    // ========== 1. 全局常量（抽离，可复用） ==========
    // 多语言工具调用系统提示（通用模板，所有工具都能用）
    private static final String MULTI_LANG_SYSTEM_PROMPT = """
            你是一个智能助手，需遵守以下工具调用规则：
            1. 自动识别用户提问的语言（中文/英文）；
            2. 调用工具时，所有参数的语言必须与用户提问语言完全一致；
            3. 中文提问 → 中文参数（如城市填"伦敦"，单位填"摄氏度"）；
            4. 英文提问 → 英文参数（如城市填"London"，单位填"Celsius"）；
            5. 严格按照工具定义的参数名输出，不新增/修改参数名，不输出无关内容。
            """;

    // 最大循环次数，防止无限调用
    private static final int MAX_LOOP_COUNT = 5;

    /**
     * 在低级别，您可以使用 ChatLanguageModel 的 chat(ChatRequest) 方法。 StreamingChatLanguageModel 中也存在类似的方法。
     *
     * 您可以在创建 ChatRequest 时指定一个或多个 ToolSpecification。
     *
     * ToolSpecification 是一个包含工具所有信息的对象：
     *
     * 工具的 name（名称）
     * 工具的 description（描述）
     * 工具的 parameters（参数）及其描述
     * 建议提供尽可能多的工具信息： 清晰的名称、全面的描述以及每个参数的描述等。
     */
    @Test
    public void lowLevel() throws Exception {
        //创建 ToolSpecification 有两种方式：
        //1 手动创建
        ToolSpecification toolSpecification = ToolSpecification.builder()
                .name("getWeather")
                .description("返回给定城市的天气预报")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "应返回天气预报的城市")
                        .addEnumProperty("temperatureUnit", List.of("CELSIUS", "FAHRENHEIT"))
                        .required("city") // 必须明确指定必需的属性
                        .build())
                .build();
        //2 使用辅助方法：
        //ToolSpecifications.toolSpecificationsFrom(Class)
        //ToolSpecifications.toolSpecificationsFrom(Object)
        //ToolSpecifications.toolSpecificationFrom(Method)
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(WeatherTools.class);

        UserMessage userMessage = UserMessage.from("查询伦敦明天的天气，单位用摄氏度");
        ChatRequest request = ChatRequest.builder()
                .messages(
                        // 系统提示（通用多语言规则）
                        SystemMessage.from(MULTI_LANG_SYSTEM_PROMPT),
                        // 用户提问
                        userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse response = ApiKeys.GEMINI_MODEL.chat(request);
        //AiMessage { text = null, thinking = null, toolExecutionRequests = [ToolExecutionRequest { id = "call_Ffo0uvbxgtZ0XI9elqm9gVhw", name = "getWeather", arguments = "{"arg0":"London","arg1":"CELSIUS"}" }], attributes = {} }
        AiMessage aiMessage = response.aiMessage();

        //返回的 AiMessage 将在 toolExecutionRequests 字段中包含数据。
        // 在这种情况下，AiMessage.hasToolExecutionRequests() 将返回 true。
        // 根据 LLM 的不同，它可以包含一个或多个 ToolExecutionRequest 对象 （一些 LLM 支持并行调用多个工具）。
        //
        //每个 ToolExecutionRequest 应包含：
        //  工具调用的 id（某些 LLM 不提供）
        //  要调用的工具的 name，例如：getWeather
        //  arguments（参数），例如：{ "city": "London", "temperatureUnit": "CELSIUS" }
        //您需要使用 ToolExecutionRequest 中的信息手动执行工具。
        //如果您想将工具执行的结果发送回 LLM， 您需要创建一个 ToolExecutionResultMessage（每个 ToolExecutionRequest 对应一个） 并将其与所有先前的消息一起发送：
        if (!aiMessage.hasToolExecutionRequests()){
            return;
        }

        ToolExecutionRequest req = aiMessage.toolExecutionRequests().get(0);

        // 3️⃣ 手动执行工具
        ToolExecutionResultMessage toolResult = executeTool(new WeatherTools(), req);

        ChatRequest request2 = ChatRequest.builder()
                .messages(List.of(userMessage, aiMessage, toolResult))
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse response2 = ApiKeys.GEMINI_MODEL.chat(request2);
        System.out.println(response2);
    }

    @Test
    public void multiFunctionCall() throws Exception {
        // 1. 定义工具（支持多个工具）
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(Calculator.class);
        // 可新增其他工具：
        toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(WeatherTools.class));

        // 2. 初始化上下文（保存所有消息记录，包括用户、AI、工具结果）
        List<ChatMessage> conversationHistory = new ArrayList<>();
        UserMessage userMessage = UserMessage.from("4757654的平方根 与 5666543的平方根 相加是多少？");
        conversationHistory.add(userMessage);

        // 3. 多轮工具调用（支持多个工具同时调用）
        AiMessage aiMessage = null;
        ChatResponse response = null;
        int loopCount = 0;
        while (loopCount < MAX_LOOP_COUNT) {
            loopCount++;

            // 构建请求（复用完整上下文）
            ChatRequest request = ChatRequest.builder()
                    .messages(conversationHistory)
                    .toolSpecifications(toolSpecifications)
                    .build();

            response = ApiKeys.GEMINI_MODEL.chat(request);
            aiMessage = response.aiMessage();
            conversationHistory.add(aiMessage); // 将AI回复加入上下文

            // 检查是否有工具调用请求（处理所有请求，而非仅第一个）
            List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
            if (toolRequests==null||toolRequests.isEmpty()) {
                break; // 无工具调用，结束循环
            }

            // 处理所有工具调用请求（关键：遍历所有请求，而非只取第一个）
            List<ToolExecutionResultMessage> toolResults = new ArrayList<>();
            for (ToolExecutionRequest req : toolRequests) {
                try {
                    // 执行单个工具调用
                    ToolExecutionResultMessage toolResult = executeTool(new Calculator(), req);
                    toolResults.add(toolResult);
                } catch (Exception e) {
                    // 工具执行失败时，返回错误结果给模型
                    ToolExecutionResultMessage errorResult = ToolExecutionResultMessage.toolExecutionResultMessage(req,e.getMessage());
                    toolResults.add(errorResult);
                }
            }

            // 将所有工具结果加入上下文
            conversationHistory.addAll(toolResults);
        }

        // 输出最终结果
        System.out.println("最终响应：" + aiMessage.text());
    }

    public ToolExecutionResultMessage executeTool(
            Object tool,
            ToolExecutionRequest request
    ) throws Exception {

        // 1. 解析参数
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> args = mapper.readValue(
                request.arguments(),
                new TypeReference<Map<String, Object>>() {}
        );

        // 2. 找方法
        Method method = Arrays.stream(tool.getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(request.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Tool method not found: " + request.name()));

        // 3. 组装参数
        Object[] invokeArgs = new Object[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < invokeArgs.length; i++) {
            Object rawValue = args.get("arg" + i);
            Class<?> targetType = parameters[i].getType();

            if (rawValue == null) {
                invokeArgs[i] = null;
                continue;
            }

            // ⭐ enum 特殊处理（关键）
            if (targetType.isEnum()) {
                invokeArgs[i] = Enum.valueOf(
                        (Class<Enum>) targetType,
                        rawValue.toString()
                );
            } else {
                // 其它类型交给 Jackson
                invokeArgs[i] = mapper.convertValue(rawValue, targetType);
            }
        }

        // 4. 执行
        Object result = method.invoke(tool, invokeArgs);

        // 5. 返回结果
        return ToolExecutionResultMessage.from(
                request,
                String.valueOf(result)
        );
    }
}
class WeatherTools {

    @Tool("返回给定城市的天气预报")
    String getWeather(
            @P("应返回天气预报的城市") String city,
            TemperatureUnit temperatureUnit) {
        // 模拟真实业务逻辑
        if ("伦敦".equalsIgnoreCase(city)) {
            return "London weather: 15 " + temperatureUnit;
        }
        return "Unknown city";
    }
}
enum TemperatureUnit{
    CELSIUS, FAHRENHEIT;
}
class Calculator {

    @Tool
    double add(int a, int b) {
        return a + b;
    }

    @Tool
    double squareRoot(double x) {
        return Math.sqrt(x);
    }
}
