package com.liuhang.langchain4j.day6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhang.langchain4j.day4.AIServiceResult;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.junit.jupiter.api.Test;
import dev.langchain4j.data.message.UserMessage;

import java.time.LocalDate;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

/**
 * 结构化输出
 * 许多 LLM 和 LLM 提供商支持以结构化格式（通常是 JSON）生成输出。 这些输出可以轻松映射到 Java 对象，并在应用程序的其他部分使用。
 * 例如，假设我们有一个 Person 类：
 * record Person(String name, int age, double height, boolean married) {
 * }
 * 我们的目标是从像这样的非结构化文本中提取 Person 对象：
 * John is 42 years old and lives an independent life.
 * He stands 1.75 meters tall and carries himself with confidence.
 * Currently unmarried, he enjoys the freedom to focus on his personal goals and interests.
 *
 * 目前，根据 LLM 和 LLM 提供商的不同，有三种方式可以实现这一目标 （从最可靠到最不可靠）：
 *  JSON Schema
 *  提示 + JSON 模式
 *  提示
 *
 * 本类是介绍JSON Schema
 * 一些 LLM 提供商（目前包括 Azure OpenAI、Google AI Gemini、Mistral、Ollama 和 OpenAI）允许 为所需输出指定 JSON schema。
 * 可以在"JSON Schema"列中查看所有支持的 LLM 提供商 这里。
 *
 * 当在请求中指定 JSON schema 时，LLM 预期生成符合该 schema 的输出。
 * 请注意：JSON schema 是在请求中作为专用属性指定给 LLM 提供商的 API 的， 不需要在提示中包含任何自由格式的指令（例如，在系统或用户消息中）。
 *
 * LangChain4j 在低级 ChatLanguageModel API 和高级 AI 服务 API 中都支持 JSON Schema 功能。
 */
public class JsonSchemaTest {

    /**
     * 在低级 ChatLanguageModel API中 的 JSON Schema
     * 在低级 ChatLanguageModel API 中，可以使用与 LLM 提供商无关的 ResponseFormat 和 JsonSchema 在创建 ChatRequest 时指定 JSON schema：
     *
     * 注意：
     * [1] - 在大多数情况下，根元素必须是 JsonObjectSchema 类型， 但 Gemini 也允许 JsonEnumSchema 和 JsonArraySchema。
     * [2] - 必须明确指定必需的属性；否则，它们被视为可选的。
     *
     * JSON schema 的结构使用 JsonSchemaElement 接口定义， 具有以下子类型：
     * JsonObjectSchema - 用于对象类型。
     * JsonStringSchema - 用于 String、char/Character 类型。
     * JsonIntegerSchema - 用于 int/Integer、long/Long、BigInteger 类型。
     * JsonNumberSchema - 用于 float/Float、double/Double、BigDecimal 类型。
     * JsonBooleanSchema - 用于 boolean/Boolean 类型。
     * JsonEnumSchema - 用于 enum 类型。
     * JsonArraySchema - 用于数组和集合（例如，List、Set）。
     * JsonReferenceSchema - 支持递归（例如，Person 有一个 Set<Person> children 字段）。
     * JsonAnyOfSchema - 支持多态性（例如，Shape 可以是 Circle 或 Rectangle）。
     * JsonNullSchema - 支持可空类型。
     * JsonObjectSchema
     */
    @Test
    public void test1() throws JsonProcessingException {
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // 类型可以是 TEXT（默认）或 JSON
                .jsonSchema(JsonSchema.builder()
                        .name("Person") // OpenAI 要求为 schema 指定名称
                        .rootElement(JsonObjectSchema.builder() // 见下面的 [1]
                                .addStringProperty("name")
                                .addIntegerProperty("age")
                                .addNumberProperty("height")
                                .addBooleanProperty("married")
                                .required("name", "age", "height", "married") // 见下面的 [2]
                                .build())
                        .build())
                .build();

        UserMessage userMessage = UserMessage.from("""
        John is 42 years old and lives an independent life.
        He stands 1.75 meters tall and carries himself with confidence.
        Currently unmarried, he enjoys the freedom to focus on his personal goals and interests.
        """);

        ChatRequest chatRequest = ChatRequest.builder()
                .responseFormat(responseFormat)
                .messages(userMessage)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();

// 或
//        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
//                .apiKey(System.getenv("GOOGLE_AI_GEMINI_API_KEY"))
//                .modelName("gemini-1.5-flash")
//                .logRequestsAndResponses(true)
//                .build();
// 或 ...

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();
        System.out.println(output); // {"name":"John","age":42,"height":1.75,"married":false}

        Person person = new ObjectMapper().readValue(output, Person.class);
        System.out.println(person); // Person[name=John, age=42, height=1.75, married=false]
    }

    /**
     * 使用 AI 服务的 JSON Schema
     * 在高级 AI 服务 API 中，JSON Schema 功能是自动处理的。 您只需定义一个返回 POJO 的 AI 服务方法：
     *
     * 注意：
     * [1] - 在 Quarkus 或 Spring Boot 应用程序中，无需显式创建 ChatLanguageModel 和 AI 服务， 因为这些 bean 是自动创建的。
     *      更多信息： 对于 Quarkus， 对于 Spring Boot。
     * [2] - 这是启用 OpenAI 的 JSON Schema 功能所必需的，详见此处。
     * [3] - 这是启用 Azure OpenAI 的 JSON Schema 功能所必需的。
     * [4] - 这是启用 Google AI Gemini 的 JSON Schema 功能所必需的。
     * [5] - 这是启用 Ollama 的 JSON Schema 功能所必需的。
     * [6] - 这是启用 Mistral 的 JSON Schema 功能所必需的。
     *
     * 当满足以下所有条件时, AI 服务方法返回 POJO：
     *  使用的 ChatLanguageModel 支持 JSON Schema 功能
     *  在使用的 ChatLanguageModel 上启用了 JSON Schema 功能
     *  那么将根据指定的返回类型自动生成带有 JsonSchema 的 ResponseFormat。
     *
     * 必需和可选
     * 添加描述
     * 限制
     * 等见：https://docs.langchain4j.info/tutorials/structured-outputs
     */
    @Test
    public void test2(){
        interface PersonExtractor {
            @SystemMessage("你是一个专门从文本中提取人物信息的助手。")
            Person extractPersonFrom(String text);
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .responseFormat(ResponseFormat.builder().type(JSON).build()) // 见下面的 [2]
                .logRequests(true)
                .logResponses(true)
                .build();
// 或
//        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
//                .apiKey(System.getenv("GOOGLE_AI_GEMINI_API_KEY"))
//                .modelName("gemini-1.5-flash")
//                .responseFormat(ResponseFormat.builder().type(JSON).build()) // 见下面的 [4]
//                .logRequestsAndResponses(true)
//                .build();
// 或 ...

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, chatModel); // 见下面的 [1]

        String text = """
        John is 42 years old and lives an independent life.
        He stands 1.75 meters tall and carries himself with confidence.
        Currently unmarried, he enjoys the freedom to focus on his personal goals and interests.
        """;

        Person person = personExtractor.extractPersonFrom(text);

        System.out.println(person); // Person[name=John, age=42, height=1.75, married=false]
    }
}
class Person {
    String name;
    int age;
    double height;
    boolean married;
}
