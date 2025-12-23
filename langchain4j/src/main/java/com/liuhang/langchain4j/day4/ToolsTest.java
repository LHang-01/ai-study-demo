package com.liuhang.langchain4j.day4;

import com.liuhang.langchain4j.Assistant;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.junit.jupiter.api.Test;

import static com.liuhang.langchain4j.ApiKeys.model;

/**
 * 工具（函数调用）
 * AI 服务可以配置 LLM 可以使用的工具：
 *
 * 在这种情况下，LLM 将在提供最终答案之前请求执行 add(1, 2) 和 multiply(3, 4) 方法。 LangChain4j 将自动执行这些方法。
 */
public class ToolsTest {

    interface Assistant {
        // 标记 userQuery 为用户消息
        String chat(@UserMessage String userQuery);
    }

    class Tools {

        @Tool
        int add(int a, int b) {
            return a + b;
        }

        @Tool
        int multiply(int a, int b) {
            return a * b;
        }
    }

    @Test
    public void test1(){
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new Tools())
                .build();

        String answer = assistant.chat("What is 1+2 and 3*4?");
        System.out.println(answer);
    }


}
