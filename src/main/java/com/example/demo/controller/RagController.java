package com.example.demo.controller;

import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.service.IngestionService;
import com.example.demo.service.RagAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class RagController implements RagControllerApi {

    private final IngestionService ingestionService;
    private final RagAiService ragAiService;

    @Override
    public ResponseEntity<String> ingestDocs(String path) {
        log.info("Received ingestion request for path: {}", path);
        ingestionService.ingestDocs(path);
        return ResponseEntity.ok("Ingestion started for directory: " + path);
    }

    @Override
    public ResponseEntity<ChatResponse> chat(ChatRequest request) {
        log.info("Received chat request: {}", request.getPrompt());
        String answer = ragAiService.chat(request.getPrompt());
        log.info("Generated answer: {}", answer);
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
