package com.example.demo.controller;

import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api")
@Tag(name = "RAG Operations", description = "Endpoints for Document Ingestion and Chat")
public interface RagControllerApi {

        @Operation(summary = "Ingest Documents", description = "Scans a directory for PDF files and ingests them into the vector store.")
        @ApiResponse(responseCode = "200", description = "Ingestion started successfully")
        @PostMapping("/ingest")
        ResponseEntity<String> ingestDocs(
                        @Parameter(description = "Absolute path to the directory containing PDFs") @RequestParam(required = false, defaultValue = "docs") String path);

        @Operation(summary = "Chat with RAG", description = "Ask a question to the RAG system based on ingested documents.")
        @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ChatResponse.class)))
        @PostMapping("/chat")
        ResponseEntity<ChatResponse> chat(
                        @RequestBody ChatRequest request);

        @Operation(summary = "Inserir PDF", description = "Faz upload de um PDF, extrai texto, divide em chunks e salva embeddings")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "PDF processado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Arquivo inválido ou erro no processamento")
        })
        @PostMapping(value = "/ingest/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        void insertPdf(
                        @Parameter(description = "Arquivo PDF", required = true) @RequestParam("file") MultipartFile file);
}
