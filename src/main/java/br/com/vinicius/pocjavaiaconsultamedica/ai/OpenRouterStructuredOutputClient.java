package br.com.vinicius.pocjavaiaconsultamedica.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wrapper pequeno sobre LangChain4j.
 *
 * Responsabilidade:
 * 1) receber system prompt + user prompt;
 * 2) anexar um JSON Schema como response_format;
 * 3) chamar o OpenRouter por meio do ChatModel;
 * 4) converter o JSON retornado para um record Java.
 *
 * Manter isso em um único lugar evita repetir código nos nós do grafo.
 */
@Component
public class OpenRouterStructuredOutputClient {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public OpenRouterStructuredOutputClient(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public <T> T generateStructuredOutput(
            String systemPrompt,
            String userPrompt,
            JsonSchema jsonSchema,
            Class<T> responseType
    ) {
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(jsonSchema)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)
                ))
                .responseFormat(responseFormat)
                .build();

        ChatResponse response = chatModel.chat(request);
        String responseJson = response.aiMessage().text();

        if (responseJson == null || responseJson.isBlank()) {
            throw new IllegalStateException("A IA retornou uma resposta estruturada vazia.");
        }

        try {
            return objectMapper.readValue(responseJson, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "A IA respondeu, mas não foi possível converter o Structured Output para "
                            + responseType.getSimpleName() + ". Resposta recebida: " + responseJson,
                    exception
            );
        }
    }
}
