# Architecture

CampusGPT uses a monorepo with a Spring Boot backend, React frontend, and PostgreSQL/pgvector database. Controllers expose DTO-based REST APIs. Services own business behavior: document upload and indexing, RAG chat, dashboard aggregation, summary, and quiz generation. JPA stores relational records, while Spring AI `VectorStore` stores embeddings and metadata for semantic search.

The RAG request flow is: user question -> vector similarity search -> guarded prompt with retrieved context -> LLM answer -> citations constructed from retrieved metadata.
