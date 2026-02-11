package com.liuhang.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class ApiKeys {
    static {
        //我在国内，需要配置 HTTP 代理才能访问 OpenAI，我用的clash代理，端口可以在clash配置中查看
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
    }
    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    public static ChatModel gpt_model = OpenAiChatModel.builder()
            .apiKey(OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .build();

    public static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    public static ChatModel GEMINI_MODEL = GoogleAiGeminiChatModel.builder()
            .apiKey(GEMINI_API_KEY)
            .modelName("gemini-2.0-flash")
            .temperature(0.2)
            .build();
}
