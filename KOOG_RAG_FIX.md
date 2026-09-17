# Koog + Semantic RAG integration

The previous integration has been replaced with a real tool-driven agent flow.

- `webSearch` is now a first-class Koog tool. It checks the semantic search cache first and calls Gemini Google Search grounding only on a miss.
- `retrieveMemory` performs embedding-based semantic retrieval instead of dumping all memories.
- `SearchMemoryRepository` stores query embeddings and performs cosine-similarity cache lookup after exact-match lookup.
- `Fact` and `SearchMemory` received nullable `embeddingJson` columns with a Room 1 -> 2 migration.
- `gemini-embedding-2` is used for embeddings. Existing rows without vectors are lazily embedded.
- `AssistantEngine` no longer pre-routes "current" queries to the old gateway. Koog chooses whether to search.
- If the Koog agent fails, the existing gateway remains a compatibility fallback.
