package com.liuhang.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

public class HelloLangchain4j {
    public static void main(String[] args) {
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

        String answer = model.chat("Say 'Hello World'");
        System.out.println(answer); // Hello World
    }

    @Test
    public void test(){
        ChatModel model = ApiKeys.GEMINI_MODEL;
//        String response = model.chat("你好");


        ChatResponse chatResponse = model.chat(ChatRequest.builder()
                .messages(UserMessage.from(
                        "单词'strawberry'中有多少个字母'R'？"))
                .build());

        String response = chatResponse.aiMessage().text();
        System.out.println(response);
    }
}
