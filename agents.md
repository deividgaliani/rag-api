# Project Context: Local RAG with LangChain4j & Ollama

## Objective
Desenvolver uma aplicação Java/Spring Boot para RAG (Retrieval-Augmented Generation) 100% local, utilizando Ollama para Embeddings e Chat.

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.3+
- **AI Integration:** LangChain4j (latest version)
- **Vector Store:** PostgreSQL (pgvector) running via Docker
- **Local LLM Runner:** Ollama

## Core Components

### 1. Document Ingestion (ETL)
- **Input:** Arquivos PDF localizados em um diretório específico.
- **Parsing:** Utilizar Apache PDFBox (via LangChain4j `FileSystemDocumentLoader`).
- **Splitting:** `DocumentSplitter` com janelas de tokens (ex: 300 tokens com overlap de 30).
- **Embedding Generation:**
  - **Model:** `nomic-embed-text`
  - **Provider:** Ollama (Localhost)
  - **Config:** `OllamaEmbeddingModel` apontando para `http://localhost:11434`.

### 2. Retrieval & Generation (RAG)
- **Interface:** API REST ou CLI para receber o prompt do usuário.
- **Retrieval:** `EmbeddingStoreContentRetriever` buscando na base vetorial.
- **Chat Model:** Llama 3 ou Mistral (via Ollama).
- **Orchestration:** Utilizar a interface `AiServices` do LangChain4j para ligar o ChatModel ao ContentRetriever.

## Infrastructure Constraints
- **Docker Compose:** Obrigatório para subir o PostgreSQL com a extensão `vector` ativada.
- **Ollama:** Assume-se que o Ollama está rodando no host (`host.docker.internal` ou `localhost`) escutando na porta 11434.

## Specific Configuration: Embedding Model
O modelo de embedding DEVE ser configurado explicitamente para usar o `nomic-embed-text`:
```java
OllamaEmbeddingModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("nomic-embed-text")
    .timeout(Duration.ofMinutes(1))
    .build();
```

---

## Infraestrutura (`docker-compose.yml`)

Use `pgvector` para persistência robusta. Soluções em memória não são escaláveis para produção.

```yaml
services:
  # Base Vetorial
  vector-db:
    image: pgvector/pgvector:pg16
    container_name: rag-vector-db
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
      POSTGRES_DB: vector_store
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

## Plano de Desenvolvimento (Roadmap Técnico)
Siga esta ordem estrita para garantir testabilidade isolada.

### Fase 1: Configuração e Infraestrutura
- **Dependências:** Adicionar langchain4j-spring-boot-starter, langchain4j-ollama, langchain4j-pgvector e dev.langchain4j:langchain4j-document-parser-apache-pdfbox.
- **Docker:** Subir o container do Postgres.
- **Schema:** Criar a tabela de embeddings no Postgres (o LangChain4j pode fazer isso automaticamente se configurado, ou via Flyway).

### Fase 2: Componente de Ingestão (ETL)
- **Configurar OllamaEmbeddingModel:** Instanciar o bean apontando para nomic-embed-text.
- **Pipeline de Ingestão:** Criar um `@Service` que:
  - Varre uma pasta local.
  - Carrega PDFs.
  - Aplica o `DocumentSplitter` (Recomendado: `DocumentSplitters.recursive(300, 20)`).
  - Gera embeddings e salva no `PgVectorEmbeddingStore`.
- **Teste de Unidade (Mock):** Validar se o texto está sendo quebrado corretamente antes de enviar para o Ollama.

### Fase 3: Implementação do RAG (AiServices)
- **Configurar OllamaChatModel:** Apontar para um modelo gerativo (ex: llama3).
- **Definir Interface AI:** Criar interface anotada com `@AiService`.
- **Content Retriever:** Configurar `EmbeddingStoreContentRetriever` conectando o `PgVectorEmbeddingStore` ao `OllamaEmbeddingModel`.
- **Wiring:** Injetar o ContentRetriever na interface do AiService.

### Fase 4: API e Validação
- **Endpoint de Ingestão:** `POST /api/ingest` (aciona o processamento dos PDFs).
- **Endpoint de Chat:** `POST /api/chat` (recebe `{ prompt: "..." }` e retorna a resposta com base nos documentos).

## Implementação Chave (Snippet de Configuração)
Para garantir que o nomic-embed-text seja usado corretamente:

```java
@Configuration
public class RagConfig {

    @Bean
    EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text") // CRÍTICO: Define o modelo específico
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database("vector_store")
                .user("user")
                .password("password")
                .table("vector_store")
                .dimension(768) // Nomic-embed-text v1.5 usa 768 dimensões
                .build();
    }
}
```
