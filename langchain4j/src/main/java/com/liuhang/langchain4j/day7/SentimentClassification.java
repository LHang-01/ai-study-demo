package com.liuhang.langchain4j.day7;

import com.liuhang.langchain4j.ApiKeys;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

public class SentimentClassification {

    // 定义情感枚举
    enum Sentiment {
        POSITIVE, NEUTRAL, NEGATIVE
    }

    // 定义 AI 驱动的情感分析器接口
    interface SentimentAnalyzer {

        @UserMessage("Analyze sentiment of {{it}}")
        Sentiment analyzeSentimentOf(String text);

        @UserMessage("Does {{it}} have a positive sentiment?")
        boolean isPositive(String text);
    }

    public static void main(String[] args) {

        // 创建 AI 驱动的情感分析器实例
        SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, ApiKeys.GEMINI_MODEL);

        // 情感分析示例
        Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("I love this product!");
        System.out.println(sentiment); // 预期输出: POSITIVE

        boolean positive = sentimentAnalyzer.isPositive("This is a terrible experience.");
        System.out.println(positive); // 预期输出: false
    }
}
