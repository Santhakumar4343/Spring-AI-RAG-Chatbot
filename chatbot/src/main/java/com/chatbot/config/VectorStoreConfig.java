package com.chatbot.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the vector store: the "database" that holds every chunk of text
 * together with its embedding (vector).
 *
 * SimpleVectorStore keeps everything in memory, so it is emptied on every
 * restart. That is fine for learning; swap it for PGVector/Redis/Chroma later.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // The store uses the EmbeddingModel (OpenAI text-embedding-3-small,
        // auto-configured from application.properties) to turn text into vectors.
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
