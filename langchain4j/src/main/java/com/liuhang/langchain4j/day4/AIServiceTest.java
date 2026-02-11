package com.liuhang.langchain4j.day4;

import com.liuhang.langchain4j.ApiKeys;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class AIServiceTest {



    interface Assistant {

        String chat(String userMessage);
    }

    /**
     * 最简单的 AI 服务
     */
    @Test
    public void baseService(){
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();
        Assistant assistant = AiServices.create(Assistant.class, model);
        String answer = assistant.chat("Hello");
        System.out.println(answer); // Hello, how can I help you?
    }

    interface Friend {

        @SystemMessage("You are a good friend of mine. Answer using slang.")
        String chat(String userMessage);
    }

    /**
     * 添加了 @SystemMessage 注解，其中包含我们想要使用的系统提示模板。 这将在幕后转换为 SystemMessage 并与 UserMessage 一起发送给 LLM。
     *
     * @SystemMessage 也可以从资源加载提示模板： @SystemMessage(fromResource = "my-prompt-template.txt")
     */
    @Test
    public void test2(){
        Friend friend = AiServices.create(Friend.class, ApiKeys.gpt_model);

        String answer = friend.chat("Hello"); // Hey! What's up?
        System.out.println(answer);
    }

    /**
     * AiServices#systemMessageProvider(java.util.function.Function)是动态配置系统消息（system message） 的关键方法，
     * 它允许你根据上下文（如用户 ID、会话 ID 等）在每次调用 AI 服务时提供不同的系统提示词。
     *
     * 参数详解：Function<Object, String> systemMessageProvider
     * 这个函数接收一个 memory ID（记忆 ID），返回对应的系统消息字符串。
     * 1. 输入：memory ID（Object 类型）
     * 来源：AI 服务方法中标注了 @MemoryId 的参数值。示例：
     *      String chat(@MemoryId String userId, @UserMessage String userMessage);
     *      调用时：chat("user123", "你好") → memory ID = "user123"
     *      如果方法中没有 @MemoryId 参数，则默认传入 "default"。
     * 2. 输出：系统消息（String）
     * 可以是完整消息，示例：
     *      return "你是用户 user123 的专属助手。";
     * 也可以是模板（含 {{variable}} 占位符），示例：
     *      return "你正在为 {{userName}} 提供服务，请保持专业。";
     * 模板变量会自动从方法参数中标注了 @V("userName") 的值进行填充。
     *
     * 坑：
     * 当你在 AI Service 方法中使用 @MemoryId 时，LangChain4j 要求你必须提供一个 ChatMemoryProvider，用于：
     *      根据 memoryId（如用户 ID）获取或创建对应的聊天记忆（ChatMemory）
     *      存储对话历史（以便 LLM 能记住上下文）
     * 否则框架不知道如何管理不同用户的对话历史，就会抛出：
     *      In order to use @MemoryId, please configure the ChatMemoryProvider...
     *
     * 如果同时存在：
     *      接口方法上的 @SystemMessage("固定提示")
     *      通过 systemMessageProvider(...) 配置的动态提示
     * 👉 @SystemMessage 优先级更高，会覆盖 systemMessageProvider 的结果。
     */
    // 1. 定义 AI 服务接口
    interface CustomerService {
        String chat(@MemoryId String userId, @V("userName") String name, @UserMessage String message);
    }
    @Test
    public void test3(){

        // 1. 创建 ChatMemoryStore（存储所有用户的记忆）
        ChatMemoryStore store = new InMemoryChatMemoryStore();

        // 2. 创建 ChatMemoryProvider（根据 memoryId 提供 ChatMemory 实例）
        ChatMemoryProvider memoryProvider = memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .chatMemoryStore(store)
                        .maxMessages(30)   // 可选，限制记忆长度
                        .build();

        // 2. 构建 AI 服务，配置动态 system message
        CustomerService service = AiServices.builder(CustomerService.class)
                .chatModel(ApiKeys.gpt_model)
                .chatMemoryProvider(memoryProvider)// ← 关键：必须配置！
                .systemMessageProvider(userId -> {
                    if ("vip_user".equals(userId)) {
                        return "你是 VIP 客户 {{userName}} 的专属管家，请提供高端服务。";
                    } else {
                        return "你是普通用户 {{userName}} 的客服助手，请友好回答。";
                    }
                })
                .build();

        // 3. 调用
        String reply1 = service.chat("vip_user", "张三", "你好");
        System.out.println(reply1);
        String reply2 = service.chat("guest_001", "李四", "你好");
        System.out.println(reply2);
    }

    /**
     * @UserMessage用于在 AI Service 接口中标识哪个方法参数代表用户输入的消息内容（即用户对 AI 说的话）
     * 它是构建自然语言交互的桥梁 —— 你调用 Java 方法时传入的字符串，会被自动包装成 UserMessage 并发送给 LLM。
     *
     *  使用位置
     * 只能用在 AI Service 接口的方法参数上
     * 一个方法通常只有一个 @UserMessage 参数（代表当前用户输入）
     *
     * ⚖️ 与其他注解的关系
     * 注解	作用
     * @UserMessage 定义用户输入内容（必选其一）
     * @SystemMessage 定义系统提示（可选，方法级或全局）
     * @V("name") 提供模板变量值（用于 @UserMessage 或 @SystemMessage 模板）
     * @MemoryId 指定会话 ID（用于多轮对话记忆）
     */
    @Test
    public void  test4(){
        //基本用法示例
        interface ChatBot {
            // 标记 userQuery 为用户消息
            String chat(@UserMessage String userQuery);
        }

        // 使用
        ChatBot bot = AiServices.builder(ChatBot.class)
                .chatModel(ApiKeys.gpt_model)
                .build();

        String reply = bot.chat("你好，请介绍一下你自己。");

        System.out.println(reply);

        //高级用法：模板支持（配合 @V）
        interface Translator {
            @UserMessage("请将以下文本翻译成{{targetLanguage}}：{{text}}")
            String translate(@V("text") String text, @V("targetLanguage") String targetLang);
        }

        // 使用
        Translator translator = AiServices.builder(Translator.class)
                .chatModel(ApiKeys.gpt_model)
                .build();
        // 调用
        String result = translator.translate("Hello", "中文");
        // 实际发送给 LLM 的用户消息是："请将以下文本翻译成中文：Hello"
        System.out.println(result);
    }
}
