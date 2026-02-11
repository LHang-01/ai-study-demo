package com.liuhang.langchain4j.day5;

import com.liuhang.langchain4j.ApiKeys;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.Test;
/**
 * 工具（函数调用）
 * 一些 LLM 除了生成文本外，还可以触发操作。
 * 有一个被称为"工具"或"函数调用"的概念。 它允许 LLM 在必要时调用一个或多个可用的工具，通常由开发者定义。
 * 工具可以是任何东西：网络搜索、调用外部 API 或执行特定代码片段等。
 *
 * 两个抽象级别
 * LangChain4j 提供了两个抽象级别来使用工具：
 * 低级别，使用 ChatLanguageModel 和 ToolSpecification API
 * 高级别，使用 AI 服务和带有 @Tool 注解的 Java 方法
 */
public class HighLevelToolTest {

    /**
     * 在高级抽象层面，您可以使用 @Tool 注解任何 Java 方法， 并在创建 AI 服务时指定它们。
     *
     * AI 服务会自动将这些方法转换为 ToolSpecification， 并在每次与 LLM 交互的请求中包含它们。
     * 当 LLM 决定调用工具时，AI 服务将自动执行相应的方法， 并将方法的返回值（如果有）发送回 LLM。 您可以在 DefaultToolExecutor 中找到实现细节。
     * 示例：
     * @Tool("使用给定的查询在 Google 中搜索相关 URL")
     * public List<String> searchGoogle(@P("搜索查询") String query) {
     *     return googleSearchService.search(query);
     * }
     *
     * 工具方法限制
     *  1 带有 @Tool 注解的方法：
     *      可以是静态或非静态的
     *      可以有任何可见性（public、private 等）。
     *  2 工具方法参数
     *      带有 @Tool 注解的方法可以接受各种类型的任意数量参数：
     *          基本类型：int、double 等
     *          对象类型：String、Integer、Double 等
     *          自定义 POJO（可以包含嵌套 POJO）
     *          enum（枚举）
     *          List<T>/Set<T>，其中 T 是上述类型之一
     *          Map<K,V>（您需要在参数描述中使用 @P 手动指定 K 和 V 的类型）也支持没有参数的方法。
     *  3 必需和可选
     *      默认情况下，所有工具方法参数都被视为必需的。 这意味着 LLM 必须为这样的参数生成一个值。 可以通过使用 @P(required = false) 注解使参数成为可选的：
     *      @Tool
     *      void getTemperature(String location, @P(required = false) Unit unit) {
     *          ...
     *      }
     *      复杂参数的字段和子字段默认也被视为必需的。 您可以通过使用 @JsonProperty(required = false) 注解使字段成为可选的：
     *      record User(String name, @JsonProperty(required = false) String email) {}
     *      @Tool
     *      void add(User user) {
     *          ...
     *      }
     * 4 @Tool 注解有 2 个可选字段：
     *      name：工具名称。如果未提供，方法名将作为工具名称。
     *      value：工具描述。
     *  根据工具的不同，LLM 可能即使没有任何描述也能很好地理解它 （例如，add(a, b) 是显而易见的）， 但通常最好提供清晰有意义的名称和描述。
     *  这样，LLM 有更多信息来决定是否调用给定的工具，以及如何调用。
     *  @P
     *      方法参数可以选择使用 @P 注解。
     *  @P 注解有 2 个字段
     *      value：参数描述。必填字段。
     *      required：参数是否必需，默认为 true。可选字段。
     *  @Description
     *      类和字段的描述可以使用 @Description 注解指定：
     *  @ToolMemoryId
     *      如果您的 AI 服务方法有一个带有 @MemoryId 注解的参数， 您也可以使用 @ToolMemoryId 注解 @Tool 方法的参数。
     *      提供给 AI 服务方法的值将自动传递给 @Tool 方法。 如果您有多个用户和/或每个用户有多个聊天/记忆， 并希望在 @Tool 方法内区分它们，这个功能很有用。
     */
    @Test
    public void highLevel(){
        interface MathGenius {

            Result<String> ask(String question);
        }

        class Calculator {

            @Tool
            double add(int a, int b) {
                return a + b;
            }

            @Tool
            double squareRoot(double x) {
                return Math.sqrt(x);
            }
        }

        MathGenius mathGenius = AiServices.builder(MathGenius.class)
                .chatModel(ApiKeys.GEMINI_MODEL)
                .tools(new Calculator())
                .build();

        Result<String> answer = mathGenius.ask("475与695之和的平方根是多少？");
        System.out.println(answer.content()); // 475695037565 的平方根是 689706.486532。
    }
}
