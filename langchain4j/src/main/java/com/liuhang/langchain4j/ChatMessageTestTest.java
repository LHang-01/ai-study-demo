package com.liuhang.langchain4j;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.internal.chat.ImageUrl;
import org.junit.jupiter.api.Test;

import java.util.List;

class ChatMessageTestTest {

    /**
     * 目前有四种类型的聊天消息，每种对应消息的一个"来源"：
     *
     * UserMessage：这是来自用户的消息。 用户可以是您应用程序的最终用户（人类）或您的应用程序本身。 根据 LLM 支持的模态，UserMessage 可以只包含文本（String）， 或其他模态。
     * AiMessage：这是由 AI 生成的消息，通常是对 UserMessage 的回应。 正如您可能已经注意到的，generate 方法返回一个包装在 Response 中的 AiMessage。 AiMessage 可以包含文本响应（String）或执行工具的请求（ToolExecutionRequest）。 我们将在另一节中探讨工具。
     * ToolExecutionResultMessage：这是 ToolExecutionRequest 的结果。
     * SystemMessage：这是来自系统的消息。 通常，您作为开发人员应该定义此消息的内容。 通常，您会在这里写入关于 LLM 角色是什么、它应该如何行为、以什么风格回答等指令。 LLM 被训练为比其他类型的消息更加关注 SystemMessage， 所以要小心，最好不要让最终用户自由定义或在 SystemMessage 中注入一些输入。 通常，它位于对话的开始。
     * CustomMessage：这是一个可以包含任意属性的自定义消息。这种消息类型只能由 支持它的 ChatLanguageModel 实现使用（目前只有 Ollama）。
     *
     * 多个 ChatMessage
     * 现在，为什么您需要提供多个 ChatMessage 作为输入，而不是只有一个？ 这是因为 LLM 本质上是无状态的，意味着它们不维护对话的状态。 因此，如果您想支持多轮对话，您应该负责管理对话的状态。
     *
     * 手动维护和管理这些消息很麻烦。 因此，存在 ChatMemory 的概念，我们将在下一节中探讨。
     */
    @Test
    void test1() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        //我在国内，需要配置 HTTP 代理才能访问 OpenAI，我用的clash代理，端口可以在clash配置中查看
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        UserMessage firstUserMessage = UserMessage.from("Hello, my name is Klaus");
        AiMessage firstAiMessage = model.chat(firstUserMessage).aiMessage(); // Hi Klaus, how can I help you?
        UserMessage secondUserMessage = UserMessage.from("What is my name?");
        //在第二次调用 chat 方法时，我们提供的不仅仅是单个 secondUserMessage， 还有对话中之前的消息。
        AiMessage secondAiMessage = model.chat(firstUserMessage, firstAiMessage, secondUserMessage).aiMessage(); // Klaus
        System.out.println(secondAiMessage.text());
    }

    /**
     * 多模态
     * UserMessage 不仅可以包含文本，还可以包含其他类型的内容。 UserMessage 包含 List<Content> contents。 Content 是一个接口，有以下实现：
     * TextContent
     * ImageContent
     * AudioContent
     * VideoContent
     * PdfFileContent
     */
    @Test
    public void testMultimodalChat() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        //我在国内，需要配置 HTTP 代理才能访问 OpenAI，我用的clash代理，端口可以在clash配置中查看
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");

        // 1. 初始化支持多模态的模型（必须是支持图片的模型，如 gpt-4o-mini）
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini") // 关键：该模型支持图片输入
                .temperature(0.7)
                .build();

        // 2. 构造多模态内容（文本 + 图片URL）
        // 注意：用 List<Content> 包裹多个内容（文本、图片等）
        List<Content> contents = List.of(
                TextContent.from("Describe the following image in detail"), // 文本指令
                ImageContent.from("https://picsum.photos/id/40/800/600" // 真实可访问的图片URL（示例图：狗）
                )
        );

        // 3. 构造 UserMessage（传入多模态内容列表）
        UserMessage userMessage = UserMessage.from(contents);

        // 4. 调用模型
        ChatResponse response = chatModel.chat(userMessage);

        // 5. 打印结果
        System.out.println("AI 对图片的描述：");
        System.out.println(response.aiMessage().text());
    }
}