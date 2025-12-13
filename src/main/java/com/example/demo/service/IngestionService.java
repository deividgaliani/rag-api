package com.example.demo.service;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

import dev.langchain4j.data.document.Document;
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
            var documents = FileSystemDocumentLoader.loadDocuments(path);
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
}
