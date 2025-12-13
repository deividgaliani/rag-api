package com.example.demo.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Configuration
public class RagConfig {

    @Value("${rag.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${rag.ollama.model-name}")
    private String ollamaModelName;

    @Value("${rag.ollama.timeout}")
    private Duration ollamaTimeout;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${rag.pgvector.table}")
    private String vectorTable;

    @Value("${rag.pgvector.dimension}")
    private Integer vectorDimension;

    @Bean
    EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .timeout(ollamaTimeout)
                .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        // Parse host/port/db from JDBC URL or just use simplistic parsing for now
        // Ideally, PgVectorEmbeddingStore builder might take a DataSource or connection
        // details
        // But for now, let's keep it simple or assume standard format.
        // Actually, PgVectorEmbeddingStore.builder() takes individual fields.

        // Let's assume standard JDBC URL format: jdbc:postgresql://host:port/database
        // We can parse it.
        String cleanUrl = dbUrl.replace("jdbc:postgresql://", "");
        String[] hostPortDb = cleanUrl.split("/");
        String[] hostPort = hostPortDb[0].split(":");
        String host = hostPort[0];
        Integer port = Integer.parseInt(hostPort[1]);
        String database = hostPortDb[1];

        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(dbUser)
                .password(dbPassword)
                .table(vectorTable)
                .dimension(vectorDimension)
                .build();
    }
}
