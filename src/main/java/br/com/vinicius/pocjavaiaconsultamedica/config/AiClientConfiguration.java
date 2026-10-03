package br.com.vinicius.pocjavaiaconsultamedica.config;

import br.com.vinicius.pocjavaiaconsultamedica.observability.LangSmithChatModelTraceListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cria o ChatModel do LangChain4j apontando para a API OpenAI-compatible do OpenRouter.
 *
 * O projeto JavaScript usa ChatOpenAI + OpenRouter. Aqui usamos o equivalente
 * idiomático em Java: OpenAiChatModel do LangChain4j apontando para o OpenRouter.
 */
@Configuration
public class AiClientConfiguration {

    @Bean
    ChatModel openRouterChatModel(
            OpenRouterProperties properties,
            LangSmithChatModelTraceListener traceListener
    ) {
        validateRequiredConfiguration(properties);

        Map<String, String> customHeaders = new LinkedHashMap<>();

        if (hasText(properties.httpReferer())) {
            customHeaders.put("HTTP-Referer", properties.httpReferer());
        }

        if (hasText(properties.xTitle())) {
            customHeaders.put("X-Title", properties.xTitle());
        }

        /*
         * O projeto original ordena providers por throughput e não usa partition.
         *
         * require_parameters=true é uma proteção da migração: como esta POC depende
         * de response_format/json_schema, pedimos ao OpenRouter um provider que aceite
         * os parâmetros necessários para Structured Output.
         */
        Map<String, Object> providerPreferences = Map.of(
                "require_parameters", true,
                "sort", Map.of(
                        "by", "throughput",
                        "partition", "none"
                )
        );

        return OpenAiChatModel.builder()
                .apiKey(properties.apiKey())
                .baseUrl(properties.baseUrl())
                .modelName(properties.model())
                .temperature(properties.temperature())
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .customHeaders(customHeaders)
                .customParameters(Map.of(
                        "models", List.of(properties.model()),
                        "provider", providerPreferences
                ))
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true)
                .listeners(List.of(traceListener))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private void validateRequiredConfiguration(OpenRouterProperties properties) {
        if (!hasText(properties.apiKey())) {
            throw new IllegalStateException(
                    "OPENROUTER_API_KEY não foi configurada. "
                            + "Copie .env.example para .env e informe sua chave."
            );
        }

        if (!hasText(properties.model())) {
            throw new IllegalStateException("OPENROUTER_MODEL não foi configurado.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
