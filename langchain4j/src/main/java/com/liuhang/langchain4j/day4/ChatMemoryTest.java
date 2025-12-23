package com.liuhang.langchain4j.day4;

import com.liuhang.langchain4j.Assistant;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.liuhang.langchain4j.ApiKeys.model;

public class ChatMemoryTest {

    /**
     * AI 服务可以使用聊天记忆来"记住"之前的交互：
     * 在这种情况下，所有 AI 服务调用都将使用相同的 ChatMemory 实例。
     * 然而，如果您有多个用户，这种方法将不起作用， 因为每个用户都需要自己的 ChatMemory 实例来维护各自的对话。
     */
    @Test
    public void test1(){
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * 解决这个问题的方法是使用 ChatMemoryProvider：
     */
    @Test
    public void test2(){
        interface Assistant  {
            String chat(@MemoryId int memoryId, @UserMessage String message);
        }

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String answerToKlaus = assistant.chat(1, "Hello, my name is Klaus");
        String answerToFrancine = assistant.chat(2, "Hello, my name is Francine");
    }

    /**
     * 以chatMemoryProvider方式使用 ChatMemory 时，重要的是要清除不再需要的对话记忆，以避免内存泄漏。
     * 要使 AI 服务内部使用的聊天记忆可访问，只需让定义它的接口扩展 ChatMemoryAccess 接口即可。
     * interface Assistant extends ChatMemoryAccess {
     *     String chat(@MemoryId int memoryId, @UserMessage String message);
     * }
     *
     * 这使得可以访问单个对话的 ChatMemory 实例，并在对话终止时删除它。
     */
    @Test
    public void test3(){

    }
}
