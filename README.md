# RAG API

Este projeto é uma API REST para **RAG (Retrieval-Augmented Generation)** desenvolvida com **Spring Boot**, utilizando **LangChain4j**, **Ollama** e **PostgreSQL (pgvector)**. A aplicação permite a ingestão de documentos PDF e a realização de chats baseados no conteúdo desses documentos.

## 🚀 Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.4.0**
- **LangChain4j 0.35.0**
- **Ollama** (LLM e Embeddings)
- **PostgreSQL + pgvector** (Vector Database)
- **Docker & Docker Compose**

## 📋 Pré-requisitos

Certifique-se de ter instalado em sua máquina:

- [Java JDK 21](https://adoptium.net/)
- [Maven](https://maven.apache.org/)
- [Docker](https://www.docker.com/) e Docker Compose
- [Ollama](https://ollama.com/) (executando localmente ou remotamente)

## 🛠️ Configuração e Execução

### 1. Carregar Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto (ou utilize os valores padrão do `application.yml`) com as seguintes configurações:

```properties
DB_URL=jdbc:postgresql://localhost:5432/vector_store
DB_USER=user
DB_PASSWORD=password
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL_NAME=nomic-embed-text
OLLAMA_CHAT_MODEL=llama3.2
```

### 2. Iniciar o Banco de Dados

Utilize o Docker Compose para subir o container do PostgreSQL com a extensão pgvector já configurada:

```bash
docker-compose up -d
```

### 3. Executar a Aplicação

Você pode executar a aplicação via Maven:

```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

## 📚 Documentação da API (Swagger)

A documentação interativa da API pode ser acessada através do Swagger UI:

- **URL:** `http://localhost:8080/swagger-ui.html`

## 🔌 Endpoints Principais

### Ingestão de Documentos

1. **Ingerir Diretório Local:**
   - **POST** `/api/ingest`
   - **Query Param:** `path` (Padrão: `docs`) - Caminho absoluto para a pasta contendo os PDFs.

2. **Upload de PDF Único:**
   - **POST** `/api/ingest/pdf`
   - **Body:** `multipart/form-data` com o campo `file` contendo o arquivo PDF.

### Chat

- **POST** `/api/chat`
- **Body:**
  ```json
  {
    "question": "O que diz o documento sobre a arquitetura?"
  }
  ```

## 🐳 Docker (Opcional)

Se desejar rodar a aplicação via Docker, certifique-se de criar a imagem ou utilizar um Dockerfile apropriado (não incluído por padrão na raiz, mas configurável).

## 🔄 Fluxo Completo de Recuperação (RAG)

1.  **Entrada (`RagController`)**: O usuário envia um POST com a pergunta.
2.  **Busca (`ContentRetriever`)**:
    *   A pergunta vira um vetor (usando `nomic-embed-text`).
    *   O sistema busca no PostgreSQL (`vector_store`) os 3 segmentos mais similares (score > 0.7).
3.  **Prompt**: O LangChain4j monta um prompt com os segmentos recuperados + a pergunta do usuário.
4.  **Geração (`ChatLanguageModel`)**: A LLM (`llama3.2`) gera a resposta usando o contexto.

## ⚙️ Detalhamento das Configurações

### 1. `application.yml` (Estrutura)
Define os parâmetros de ingestão e conexão:
*   `rag.ingestion.document-splitter`:
    *   `max-segment-size`: Tamanho do pedaço de texto (ex: 2000 chars).
    *   `max-overlap-size`: Repetição para manter contexto (ex: 400 chars).

### 2. `.env` (Ambiente)
Controla variáveis dinâmicas sem mexer no código:
*   `OLLAMA_MODEL_NAME`: Modelo de Embedding (ex: `nomic-embed-text`).
*   `OLLAMA_CHAT_MODEL`: Modelo de Chat (ex: `llama3.2`, `mistral`).

### 3. `RagConfig.java` (Comportamento)
Define regras de negócio do RAG:
*   `maxResults(3)`: Quantidade de trechos de documentos a enviar para a IA.
*   `minScore(0.7)`: Filtragem de relevância (0.0 a 1.0).

## 📝 Notas

- O modelo de embedding configurado deve estar disponível no seu servidor Ollama (ex: `ollama pull nomic-embed-text`).
- O modelo de chat também deve estar baixado (ex: `ollama pull llama3.2`).
