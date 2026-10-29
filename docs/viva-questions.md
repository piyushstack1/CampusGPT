# Viva Questions

1. What problem does CampusGPT solve? It answers questions from uploaded academic PDFs using retrieval-grounded generation.
2. What is RAG? Retrieval-Augmented Generation retrieves relevant evidence before generating an answer.
3. Why use embeddings? They represent semantic meaning for similarity search.
4. Why pgvector? It stores and searches vectors inside PostgreSQL.
5. Why not send full PDFs to the LLM? It is expensive, slow, and may exceed context limits.
6. What is chunking? Splitting extracted text into smaller overlapping passages.
7. Why preserve page numbers? They make citations verifiable.
8. How are hallucinations reduced? Retrieval checks, grounded prompts, and source citations.
9. What happens when evidence is missing? The API returns a fallback with `grounded=false`.
10. Why use DTOs? They keep API contracts separate from database entities.
11. Why Flyway? It versions database schema changes.
12. What does Spring Data JPA do? It maps Java entities to relational tables.
13. What does Spring AI do here? It provides chat, embedding, and vector store abstractions.
14. Why validate PDFs? To reject unsupported, empty, unsafe, or unreadable files.
15. Why generated stored filenames? To avoid trusting user paths.
16. What is semantic search? Search by meaning rather than exact keyword matching.
17. What is top-K retrieval? Returning the K most relevant chunks.
18. What is a similarity threshold? A minimum relevance score for accepted evidence.
19. Why keep API keys out of React? Frontend code is visible to users.
20. What is CORS? Browser protection controlling cross-origin API access.
21. Why use transactions? To keep related database changes consistent.
22. How does deletion work? It removes metadata and stored files; vector cleanup should be kept aligned with vector metadata.
23. What are project limitations? No OCR, no authentication, and live AI requires credentials.
24. How would you add authentication? Add users, ownership columns, and Spring Security.
25. How do you evaluate RAG? Check retrieval relevance, answer correctness, citation correctness, and latency.
