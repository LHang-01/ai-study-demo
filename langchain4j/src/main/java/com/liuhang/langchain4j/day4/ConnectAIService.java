package com.liuhang.langchain4j.day4;

import com.liuhang.langchain4j.ApiKeys;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.junit.jupiter.api.Test;

/**
 * 链接多个 AI 服务
 *
 * 简单的例子。 我想为我的公司构建一个聊天机器人。
 *      如果用户向聊天机器人打招呼， 我希望它用预定义的问候语回应，而不依赖 LLM 生成问候语。
 *      如果用户提出问题，我希望 LLM 使用公司的内部知识库生成回应（即 RAG）。
 */
public class ConnectAIService {
    interface GreetingExpert {

        @UserMessage("Is the following text a greeting? Text: {{it}}")
        boolean isGreeting(String text);
    }

    interface ChatBot {

        @SystemMessage("You are a polite chatbot of a company called Miles of Smiles.")
        String reply(String userMessage);
    }

    static class MilesOfSmiles {

        private final GreetingExpert greetingExpert;
        private final ChatBot chatBot;

        MilesOfSmiles(GreetingExpert greetingExpert, ChatBot chatBot) {
            this.greetingExpert = greetingExpert;
            this.chatBot = chatBot;
        }

        public String handle(String userMessage) {
            if (greetingExpert.isGreeting(userMessage)) {
                return "Greetings from Miles of Smiles! How can I make your day better?";
            } else {
                return chatBot.reply(userMessage);
            }
        }
    }


    /**
     * 注意我们如何使用qwen 来完成识别文本是否为问候语这一简单任务， 而使用更昂贵的 GPT-4 和内容检索器（RAG）来完成更复杂的任务。
     *
     * 这是一个非常简单且有些幼稚的例子，但希望它能说明这个想法。
     *
     * 现在，我可以模拟 GreetingExpert 和 ChatBot，并单独测试 MilesOfSmiles。 我还可以分别对 GreetingExpert 和 ChatBot 进行集成测试。
     * 我可以分别评估它们，并为每个子任务找到最优参数， 或者从长远来看，甚至可以为每个特定子任务微调一个小型专用模型。
     * @param args
     */
    public static void main(String[] args) {

        // 创建通义千问模型实例（以 qwen-max 为例）
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("ZHIPU_API_KEY")) // 智谱 API Key
                .modelName("glm-4-flash")               // 或 glm-4 / glm-4-air
                .temperature(0.7)
                .build();

        GreetingExpert greetingExpert = AiServices.create(GreetingExpert.class, model);
        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(ApiKeys.model)
                .build();

        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(greetingExpert, chatBot);

        String greeting = milesOfSmiles.handle("Hello");
        System.out.println(greeting); // Greetings from Miles of Smiles! How can I make your day better?

        String answer = milesOfSmiles.handle("Which services do you provide?");
        System.out.println(answer); // At Miles of Smiles, we provide a wide range of services ...
    }

    @Test
    public void test(){
        System.out.println(System.getenv("ZHIPU_API_KEY"));
    }
}
