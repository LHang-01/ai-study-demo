package com.liuhang.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;

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
}
