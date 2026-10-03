package br.com.vinicius.pocjavaiaconsultamedica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centraliza as configurações necessárias para acessar o OpenRouter.
 *
 * A ideia é evitar valores espalhados pelo código e facilitar a troca de modelo
 * sem alterar nenhuma classe Java.
 */
@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(
        String apiKey,
        String baseUrl,
        String model,
        String httpReferer,
        String xTitle,
        double temperature,
        long timeoutSeconds
) {
}
