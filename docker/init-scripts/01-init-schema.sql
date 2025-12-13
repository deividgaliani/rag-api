CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS vector_store;

CREATE TABLE vector_store (
    embedding_id UUID PRIMARY KEY,
    embedding vector(768),
    text TEXT,
    metadata JSONB,
    sequence INTEGER
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
