package com.example.demo.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.util.List;

@Configuration
@Slf4j
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

    @Value("${rag.ollama.chat-model}")
    private String ollamaChatModelName;

    @Value("${rag.retriever.max-results}")
    private Integer maxResults;

    @Value("${rag.retriever.min-score}")
    private Double minScore;

    @Bean
    EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .timeout(ollamaTimeout)
                .build();
    }

    @Bean
    dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel() {
        dev.langchain4j.model.chat.ChatLanguageModel model = dev.langchain4j.model.ollama.OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaChatModelName)
                .timeout(ollamaTimeout)
                .build();

        return (dev.langchain4j.model.chat.ChatLanguageModel) java.lang.reflect.Proxy.newProxyInstance(
                model.getClass().getClassLoader(),
                new Class[] { dev.langchain4j.model.chat.ChatLanguageModel.class },
                (proxy, method, args) -> {
                    if ("generate".equals(method.getName()) && args != null && args.length > 0) {
                        log.info("Invoking ChatModel: {}", method.getName());
                        for (Object arg : args) {
                            log.info("Argument: {}", arg);
                        }
                    }
                    try {
                        return method.invoke(model, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                });
    }

    @Bean
    dev.langchain4j.rag.content.retriever.ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        dev.langchain4j.rag.content.retriever.ContentRetriever retriever = dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return query -> {
            log.info("Retrieving content for query: {}", query.text());
            List<dev.langchain4j.rag.content.Content> contents = retriever.retrieve(query);
            contents.forEach(content -> log.info("Retrieved content segment: {}", content.textSegment().text()));
            return contents;
        };
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
