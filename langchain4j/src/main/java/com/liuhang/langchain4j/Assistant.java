package com.liuhang.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public interface Assistant {
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
