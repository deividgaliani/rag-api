package com.example.demo.service;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

import dev.langchain4j.data.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@lombok.extern.slf4j.Slf4j
public class IngestionService {

    private final EmbeddingStoreIngestor ingestor;

    public IngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 20))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    public void ingestDocs(String directoryPath) {
        Path path = Paths.get(directoryPath);
        log.info("Starting ingestion from directory: {}", path.toAbsolutePath());
        try {
            var documents = FileSystemDocumentLoader.loadDocuments(path, new ApachePdfBoxDocumentParser());
            log.info("Loaded {} documents from {}", documents.size(), path);

            var sanitizedDocuments = documents.stream()
                    .map(doc -> {
                        if (doc.text() == null)
                            return doc;
                        // Postgres text cannot contain null bytes (0x00)
                        String sanitizedText = doc.text().replace("\u0000", "");
                        return Document.from(sanitizedText, doc.metadata());
                    })
                    .collect(Collectors.toList());

            this.ingestor.ingest(sanitizedDocuments);
            log.info("Ingestion completed successfully.");
        } catch (Exception e) {
            log.error("Error during ingestion", e);
            throw e;
        }
    }

    public void ingestPdf(MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("ingestion-", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved upload to temp file: {}", tempFile);

            Document document = FileSystemDocumentLoader.loadDocument(tempFile, new ApachePdfBoxDocumentParser());

            // Postgres text cannot contain null bytes (0x00)
            String sanitizedText = document.text() == null ? "" : document.text().replace("\u0000", "");
            Document sanitizedDocument = Document.from(sanitizedText, document.metadata());

            this.ingestor.ingest(Collections.singletonList(sanitizedDocument));
            log.info("Ingestion of file {} completed successfully.", file.getOriginalFilename());

            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.error("Error during single file ingestion", e);
            throw new RuntimeException("Failed to ingest file", e);
        }
    }
}
