package com.example.demo.service;

import dev.langchain4j.service.spring.AiService;

@AiService
public interface RagAiService {
    String chat(String userMessage);
}
