package com.liuhang.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ChatMemoryTest {
    /**
     * 方式一：最常用（推荐）— AiServices + @MemoryId
     *
     * 你想要让每个用户有独立记忆，这种方式最简单。
     */
    @Test
    public void test1() {
        //我在国内，需要配置 HTTP 代理才能访问 OpenAI，我用的clash代理，端口可以在clash配置中查看
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");

        // 1) OpenAI 模型
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // 2) 用于保存所有 session 的记忆
        InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

        // 3) 根据 sessionId 创建独立 Memory
        ChatMemoryProvider memoryProvider = memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .chatMemoryStore(memoryStore)
                        .maxMessages(30)   // 可选，限制记忆长度
                        .build();

        // 4) 创建 AI 服务
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryProvider)
                .build();

        // 5) 使用，所有对话自动记忆
        String session = "user-001";

        System.out.println(assistant.chat(session, "我的名字是小刘"));
        System.out.println(assistant.chat(session, "我叫什么名字？"));
    }
}
