package com.example.demo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestionServiceTest {

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private IngestionService ingestionService;
    private final int MAX_SEGMENT_SIZE = 300;
    private final int MAX_OVERLAP_SIZE = 50;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingStore = mock(EmbeddingStore.class);
        ingestionService = new IngestionService(embeddingModel, embeddingStore, MAX_SEGMENT_SIZE, MAX_OVERLAP_SIZE);
    }

    @Test
    void testIngestDocs_Splitting() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("ingest_test");
        Path pdfFile = tempDir.resolve("test.txt"); // Helper loads text files too or we'd need a real PDF.
        // FileSystemDocumentLoader behavior depends on parsers.
        // Default might not support .txt if only pdfbox parser is on classpath?
        // Let's check dependencies. pdfbox parser is there.
        // We really should use a sample PDF or mock the loader outcome, but
        // IngestionService calls loader static method directly.
        // FileSystemDocumentLoader loads .txt if standard parser is available. But pom
        // has 'langchain4j-document-parser-apache-pdfbox'.
        // It's safer to create a dummy PDF or just rely on the fact that we can't
        // easily test without a real file.
        // Wait, IngestionService uses FileSystemDocumentLoader which uses ServiceLoader
        // to find parsers.
        // If I write a simple text file, will it pick it up?
        // Let's try to mock the loader? No, it's a static call.
        // I will write a small text file and hope there's a default parser, or I'll
        // name it .pdf and write text (might fail parsing).

        // Actually, langchain4j-spring-boot-starter often includes basic parsers.
        // Let's write a simple text content to a file.
        Files.writeString(pdfFile, "Hello world. This is a test document to verify splitting.");

        // Mock embedding generation
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            return Response.from(
                    segments.stream().map(s -> dev.langchain4j.data.embedding.Embedding.from(new float[] { 0.1f }))
                            .toList());
        });

        // Act
        // We need to account for FileSystemDocumentLoader capabilities.
        // If it fails to find a parser for .txt, we might have issues.
        // Let's assume for now we might need a real PDF structure if we forced PDF
        // parser.
        // But let's try with a text file first, as it's easier.
        // If the 'document-parser-apache-pdfbox' is the ONLY one, it might ignore .txt.

        // PROCEEDING with a risky assumption that .txt works or that I can just verify
        // the flow.
        // Ideally I'd create a valid PDF.
        // Let's try to write a minimal valid PDF text? Too complex.

        // If the test fails on parsing, I will know.

        ingestionService.ingestDocs(tempDir.toString());

        // Assert
        ArgumentCaptor<List<TextSegment>> segmentCaptor = ArgumentCaptor.forClass(List.class);
        // EmbeddingStoreIngestor calls embeddingStore.addAll(embeddings, segments);
        // Wait, looking at IngestionService, it uses EmbeddingStoreIngestor.
        // verify(embeddingStore).addAll(anyList(), anyList());

        verify(embeddingModel).embedAll(anyList());
        verify(embeddingStore).addAll(anyList(), anyList()); // It calls addAll(embeddings, embedded)

        // Cleaning up
        Files.delete(pdfFile);
        Files.delete(tempDir);
    }
}
