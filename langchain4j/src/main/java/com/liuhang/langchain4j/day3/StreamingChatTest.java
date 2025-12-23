package com.liuhang.langchain4j.day3;

import com.liuhang.langchain4j.ApiKeys;
import com.liuhang.langchain4j.day2.ServiceWithMemoryExample;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponse;
import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponseAndError;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 低级 LLM API 的响应流式传输
 * LLM 提供商提供了一种方式，可以逐个标记地流式传输响应，而不是等待整个文本生成完毕。
 */
public class StreamingChatTest {

    /**
     * 通过实现 StreamingChatResponseHandler，您可以为以下事件定义操作：
     *
     * 当生成下一个部分响应时：调用 onPartialResponse(String partialResponse)。 部分响应可以由单个或多个标记组成。 例如，您可以在标记可用时立即将其发送到 UI。
     * 当 LLM 完成生成时：调用 onCompleteResponse(ChatResponse completeResponse)。 ChatResponse 对象包含完整的响应（AiMessage）以及 ChatResponseMetadata。
     * 当发生错误时：调用 onError(Throwable error)。
     * 以下是如何使用 StreamingChatLanguageModel 实现流式传输的示例：
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        String userMessage = "Tell me a joke";

        // 创建一个倒计时锁存器，初始值为1
        CountDownLatch latch = new CountDownLatch(1);

        model.chat(userMessage, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse); // 流式输出，不换行
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n--- 流式响应完成 ---");
                System.out.println("最终完整响应: " + completeResponse);
                latch.countDown(); // 完成，释放锁
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("发生错误: " + error.getMessage());
                error.printStackTrace();
                latch.countDown(); // 出错也要释放，避免主线程永久阻塞
            }
        });

        // 主线程等待，直到 latch 被 countDown()
        boolean finished = latch.await(60, TimeUnit.SECONDS); // 最多等60秒
        if (!finished) {
            System.err.println("超时：流式响应未在60秒内完成。");
        }

        System.out.println("\n主线程继续执行...");
    }

    /**
     * 更紧凑的流式传输响应的方式是使用 LambdaStreamingResponseHandler 类。
     * 这个工具类提供了使用 lambda 表达式创建 StreamingChatResponseHandler 的静态方法。
     * 使用 lambda 流式传输响应的方式非常简单。
     * @throws InterruptedException
     */
    @Test
    public void test2() throws InterruptedException {
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();
        model.chat("Tell me a joke", onPartialResponse(System.out::print));
//        model.chat("Tell me a joke", onPartialResponseAndError(System.out::print, Throwable::printStackTrace));
        Thread.sleep(5000);
    }

}
