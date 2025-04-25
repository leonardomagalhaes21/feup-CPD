package chat.server.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class OllamaService {

    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "llama3";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final String ollamaUrl;
    private final String model;
    private final HttpClient httpClient;

    public OllamaService() {
        this(DEFAULT_OLLAMA_URL, DEFAULT_MODEL);
    }

    public OllamaService(String ollamaUrl, String model) {
        this.ollamaUrl = ollamaUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Asynchronously generates a response from the AI model based on the given
     * prompt and message history.
     *
     * @param basePrompt The initial system prompt/instruction for the AI
     * @param messageHistory Recent conversation messages (can be empty)
     * @param onSuccess Consumer that receives the AI response
     * @param onError Consumer that receives any error message
     * @return CompletableFuture that completes when the response is received
     */
    public CompletableFuture<Void> generateResponse(String basePrompt,
            List<String> messageHistory,
            Consumer<String> onSuccess,
            Consumer<String> onError) {
        try {
            // Format the prompt by combining the base prompt and message history
            StringBuilder fullPrompt = new StringBuilder();
            fullPrompt.append(basePrompt).append("\n\n");

            // Add message history (limited to the last 10 messages for context)
            int startIndex = Math.max(0, messageHistory.size() - 10);
            for (int i = startIndex; i < messageHistory.size(); i++) {
                fullPrompt.append(messageHistory.get(i)).append("\n");
            }

            // Create the request payload
            String payload = String.format("{\"model\": \"%s\", \"prompt\": \"%s\"}",
                    model,
                    escapeJson(fullPrompt.toString()));

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            // Send the async request and handle the response
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            // Parse the response
                            String responseBody = response.body();
                            String aiResponse = parseOllamaResponse(responseBody);
                            onSuccess.accept(aiResponse);
                        } else {
                            onError.accept("Error from Ollama API: " + response.statusCode() + " - " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        onError.accept("Exception while calling Ollama API: " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            onError.accept("Error preparing Ollama request: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Parses the Ollama API response to extract the generated text. Ollama API
     * returns each chunk of generated text as a JSON object, and we need to
     * collect the complete response.
     */
    private String parseOllamaResponse(String responseBody) {
        StringBuilder fullResponse = new StringBuilder();

        // The response might have multiple lines of JSON
        String[] lines = responseBody.split("\\n");

        for (String line : lines) {
            // Process each line that contains a response
            if (line.contains("\"response\":")) {
                try {
                    // Extract the text between quotes after "response":
                    int startIndex = line.indexOf("\"response\":\"") + "\"response\":\"".length();
                    int endIndex = line.indexOf("\"", startIndex);

                    if (endIndex > startIndex) {
                        String chunk = line.substring(startIndex, endIndex)
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                        fullResponse.append(chunk);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing Ollama response chunk: " + e.getMessage());
                }
            }
        }

        String result = fullResponse.toString().trim();

        // Check if we have a reasonable response
        if (result.isEmpty() || result.length() < 5) {
            return "I apologize, but I couldn't generate a proper response to your query. Please try again.";
        }

        return result;
    }

    /**
     * Escapes special characters in the prompt for JSON inclusion
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
