package com.liuhang.langchain4j.day4;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.liuhang.langchain4j.ApiKeys;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.*;
import dev.langchain4j.service.tool.ToolExecution;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.liuhang.langchain4j.ApiKeys.OPENAI_API_KEY;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AIServiceResult {

    /**
     * 返回类型
     * AI 服务方法可以返回以下类型之一：
     *
     * String - 在这种情况下，LLM 生成的输出将不经任何处理/解析直接返回
     * 结构化输出支持的任何类型 - 在这种情况下， AI 服务将在返回之前将 LLM 生成的输出解析为所需类型
     * 任何类型都可以额外包装在 Result<T> 中，以获取有关 AI 服务调用的额外元数据：
     *
     * TokenUsage - AI 服务调用期间使用的令牌总数。如果 AI 服务对 LLM 进行了多次调用 （例如，因为执行了工具），它将汇总所有调用的令牌使用情况。
     * Sources - 在 RAG 检索期间检索到的 Content
     * 已执行的工具
     * FinishReason
     * 示例：
     */
    @Test
    public void test1(){
        interface Assistant {

            @UserMessage("Generate an outline for the article on the following topic: {{it}}")
            Result<List<String>> generateOutlineFor(String topic);
        }
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(ApiKeys.model)
                .build();
        Result<List<String>> result = assistant.generateOutlineFor("Java");

        List<String> outline = result.content();
        TokenUsage tokenUsage = result.tokenUsage();
        List<Content> sources = result.sources();
        List<ToolExecution> toolExecutions = result.toolExecutions();
        FinishReason finishReason = result.finishReason();
    }

    /**
     * 如果您想从 LLM 接收结构化输出（例如，复杂的 Java 对象，而不是 String 中的非结构化文本），
     * 您可以将 AI 服务方法的返回类型从 String 更改为其他类型。
     *
     * 返回类型为 boolean
     */
    @Test
    public void test2(){
        interface SentimentAnalyzer {

            @UserMessage("Does {{it}} has a positive sentiment?")
            boolean isPositive(String text);

        }

        SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, ApiKeys.model);

        boolean positive = sentimentAnalyzer.isPositive("It's wonderful!");
        System.out.println(positive);
    }

    /**
     * 返回类型为 Enum
     */
    @Test
    public void test3(){
        enum Priority {
            CRITICAL, HIGH, LOW
        }

        interface PriorityAnalyzer {

            @UserMessage("Analyze the priority of the following issue: {{it}}")
            Priority analyzePriority(String issueDescription);
        }

        PriorityAnalyzer priorityAnalyzer = AiServices.create(PriorityAnalyzer.class, ApiKeys.model);

        Priority priority = priorityAnalyzer.analyzePriority("The main payment gateway is down, and customers cannot process transactions.");
        // CRITICAL
        System.out.println(priority);
    }

    /**
     * 返回类型为 POJO
     * 注意：在实际运行中 LLM（大语言模型）生成的 JSON 包含了非法字段 this$0，导致反序列化失败。
     * 这通常不是代码的问题，而是 LLM 输出格式不可控 + 缺少对未知字段的容错机制 所致。
     * 而且很多模型（尤其是未加约束的）习惯用markdown：
     * ```json
     * { "key": "value" }
     * 而 Jackson 无法解析这种带 Markdown 包裹的 JSON
     *
     * ✅ 推荐解决方案（组合使用）
     * ✅ 方案 1：给 POJO 添加 @JsonIgnoreProperties(ignoreUnknown = true)
     * ✅ 方案 2：强化提示词（Prompt），明确禁止额外字段
     * 在 @UserMessage 或通过 @SystemMessage 增加约束：
     * interface PersonExtractor {
     *     @SystemMessage("""
     *         You are an expert information extractor.
     *         Extract ONLY the following fields: firstName, lastName, birthDate (as "yyyy-MM-dd"),
     *         and address (with street, streetNumber, city).
     *         DO NOT include any other fields, comments, explanations, or markdown.
     *         Respond with VALID JSON ONLY, wrapped in ```json ... ```.
     *         """)
     *     @UserMessage("Extract information about a person from {{it}}")
     *     Person extractPersonFrom(String text);
     * }
     * ✅ 方案 3（可选）：使用 OpenAI 的 response_format={ "type": "json_object" }
     * 如果你用的是 OpenAI GPT-3.5/4，可以强制其输出合法 JSON：
     * // 构建模型时指定（以 OpenAI 为例）
     * ChatLanguageModel model = OpenAiChatModel.builder()
     *     .apiKey("your-api-key")
     *     .modelName("gpt-4o")
     *     .responseFormat("json_object") // ← 强制输出 JSON
     *     .build();
     * // 然后传入 AiServices
     * PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, model);
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Person {
        @Description("first name of a person") // 您可以添加可选描述，帮助 LLM 更好地理解
        String firstName;
        String lastName;
        LocalDate birthDate;
        Address address;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Description("an address") // 您可以添加可选描述，帮助 LLM 更好地理解
    class Address {
        String street;
        Integer streetNumber;
        String city;
    }

    interface PersonExtractor {
        @SystemMessage("""
        Extract person data as pure JSON only.
        Include fields: firstName, lastName, birthDate (format: "yyyy-MM-dd"),
        address (with street, streetNumber, city).
        DO NOT use markdown, DO NOT add any other text.
        Output the JSON object directly.
        """)
        @UserMessage("Extract information about a person from {{it}}")
        Person extractPersonFrom(String text);
    }
    @Test
    public void test4(){
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .responseFormat("json_object")  // ← 强制纯 JSON
                .build();

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, model);

        String text = """
            In 1968, amidst the fading echoes of Independence Day,
            a child named John arrived under the calm evening sky.
            This newborn, bearing the surname Doe, marked the start of a new journey.
            He was welcomed into the world at 345 Whispering Pines Avenue
            a quaint street nestled in the heart of Springfield
            an abode that echoed with the gentle hum of suburban dreams and aspirations.
            """;

        Person person = personExtractor.extractPersonFrom(text);

        System.out.println(person); // Person { firstName = "John", lastName = "Doe", birthDate = 1968-07-04, address = Address { ... } }
    }

    /**
     * JSON 模式
     * 在提取自定义 POJO（实际上是 JSON，然后解析为 POJO）时， 建议在模型配置中启用"JSON 模式"。 这样，LLM 将被强制以有效的 JSON 格式响应。
     *
     * 以下是如何启用 JSON 模式：
     * 对于 OpenAI：
     * 对于支持结构化输出的较新模型（例如，gpt-4o-mini、gpt-4o-2024-08-06）：
     * OpenAiChatModel.builder()
     *     ...
     *     .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
     *     .strictJsonSchema(true)
     *     .build();
     * 对于较旧的模型（例如 gpt-3.5-turbo、gpt-4）：
     * OpenAiChatModel.builder()
     *     ...
     *     .responseFormat("json_object")
     *     .build();
     * 对于 Azure OpenAI：
     * AzureOpenAiChatModel.builder()
     *     ...
     *     .responseFormat(new ChatCompletionsJsonResponseFormat())
     *     .build();
     * 对于 ...
     */
    @Test
    public void test5(){
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true)
                .build();
    }

    /**
     * 流式处理
     * AI 服务可以使用 TokenStream 返回类型逐个令牌流式处理响应：
     */
    @Test
    public void test6() throws InterruptedException, ExecutionException, TimeoutException {
        interface Assistant {
            TokenStream chat(String message);
        }

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(GPT_4_O_MINI)
                .build();

        Assistant assistant = AiServices.create(Assistant.class, model);

        TokenStream tokenStream = assistant.chat("Tell me a joke");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        tokenStream.onPartialResponse(System.out::print)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();

        ChatResponse chatResponse = futureResponse.get(30, SECONDS);
        System.out.println("\n" + chatResponse);
    }

    /**
     * Flux
     * 您也可以使用 Flux<String> 代替 TokenStream。 为此，请导入 langchain4j-reactor 模块：
     *
     * <dependency>
     *     <groupId>dev.langchain4j</groupId>
     *     <artifactId>langchain4j-reactor</artifactId>
     *     <version>1.0.0-beta3</version>
     * </dependency>
     *
     * interface Assistant {
     *
     *   Flux<String> chat(String message);
     * }
     */
    @Test
    public void test7(){

    }

}
