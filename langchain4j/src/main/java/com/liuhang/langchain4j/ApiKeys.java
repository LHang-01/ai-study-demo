package com.liuhang.langchain4j;

public class ApiKeys {
    static {
        //我在国内，需要配置 HTTP 代理才能访问 OpenAI，我用的clash代理，端口可以在clash配置中查看
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
    }
    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
}
