package br.com.vinicius.pocjavaiaconsultamedica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações da observabilidade no LangSmith.
 *
 * LANGSMITH_API_KEY autentica o envio do trace.
 * LANGSMITH_TRACING decide se o trace deve ser enviado.
 * LANGSMITH_PROJECT agrupa as execuções no painel.
 */
@ConfigurationProperties(prefix = "langsmith")
public record LangSmithProperties(
        String apiKey,
        boolean tracing,
        String project,
        String endpoint,
        boolean includeLlmPayloads
) {

    public boolean isTracingEnabled() {
        return tracing;
    }
}
