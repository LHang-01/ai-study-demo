package com.liuhang.langchain4j.day6;

/**
 * 使用提示时（这是默认选择，除非启用了JSON schema支持）， AI服务将自动生成格式指令并将其附加到 UserMessage 的末尾， 指示 LLM 应以何种格式响应。
 * 在方法返回之前，AI 服务将 LLM 的输出解析为所需类型。
 *
 * 您可以通过启用日志记录观察附加的指令。
 * 备注：这种方法相当不可靠。 如果 LLM 和 LLM 提供商支持上述方法，最好使用那些方法。
 */
public class PromptWithJsonModeTest {
}
