package com.chatbot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the chatbot.
 *
 * For every question the RetrievalAugmentationAdvisor:
 *   1. embeds the question,
 *   2. finds the most similar chunks in the vector store (RETRIEVAL),
 *   3. adds them to the prompt as context (AUGMENTATION),
 * and then the LLM writes the answer (GENERATION).
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions about the user's documents.
            Use only the information in the provided context.
            If the answer is not in the context, say "I couldn't find that in the documents."
            """;

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(4)                    // return the 4 best chunks
                                .similarityThreshold(0.3)   // ignore weak matches (0 = anything, 1 = exact)
                                .build())
                        .build())
                .build();
    }

    // Easy to test in a browser: http://localhost:8080/api/chat?question=What is Auto-Pilot?
    @GetMapping
    public ChatResponse ask(@RequestParam String question) {
        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();
        return new ChatResponse(question, answer);
    }

    // For frontends / Postman: POST /api/chat  body: {"question": "..."}
    @PostMapping
    public ChatResponse askPost(@RequestBody ChatRequest request) {
        return ask(request.question());
    }

    public record ChatRequest(String question) {}

    public record ChatResponse(String question, String answer) {}
}
