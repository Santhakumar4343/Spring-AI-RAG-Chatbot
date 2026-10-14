package com.chatbot.ingestion;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Runs once when the application starts and loads every file in
 * src/main/resources/docs into the vector store.
 *
 * Pipeline: READ -> CHUNK -> EMBED -> INDEX
 */
@Component
public class DocumentIngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;

    // Every file inside resources/docs (PDF, DOCX, TXT, PPTX, HTML ...)
    @Value("classpath:docs/*")
    private Resource[] documents;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        List<Document> allChunks = new ArrayList<>();

        for (Resource file : documents) {
            // 1. READ: Apache Tika extracts plain text from almost any file type
            List<Document> pages = new TikaDocumentReader(file).read();

            // 2. CHUNK: split the text into pieces of ~800 tokens
            List<Document> chunks = new TokenTextSplitter().apply(pages);

            // Remember which file each chunk came from (useful for showing sources)
            chunks.forEach(chunk -> chunk.getMetadata().put("source", file.getFilename()));

            log.info("{} -> {} chunks", file.getFilename(), chunks.size());
            allChunks.addAll(chunks);
        }

        // 3 + 4. EMBED + INDEX: add() sends each chunk to the embedding model
        // and stores (text + vector + metadata) in the vector store
        vectorStore.add(allChunks);

        log.info("Ingestion finished: {} chunks from {} files", allChunks.size(), documents.length);
    }
}
