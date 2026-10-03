package br.com.vinicius.pocjavaiaconsultamedica.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configura OpenTelemetry para exportar traces diretamente para o endpoint OTLP do LangSmith.
 *
 * Por que fazer isso?
 *
 * No ecossistema JavaScript da aula, LangChain/LangGraph conseguem ativar tracing
 * automaticamente por variáveis de ambiente. Nesta versão Java, usamos a integração
 * explícita e verificável:
 *
 * LangGraph4j/LangChain4j -> OpenTelemetry -> LangSmith.
 */
@Configuration
public class LangSmithOpenTelemetryConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LangSmithOpenTelemetryConfiguration.class);

    @Bean(destroyMethod = "close")
    OpenTelemetrySdk langSmithOpenTelemetry(LangSmithProperties properties) {
        if (!properties.isTracingEnabled()) {
            LOGGER.info("LangSmith tracing desabilitado.");
            return OpenTelemetrySdk.builder().build();
        }

        validateLangSmithConfiguration(properties);

        String tracesEndpoint = buildLangSmithOtlpTracesEndpoint(properties.endpoint());

        OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(tracesEndpoint)
                .addHeader("x-api-key", properties.apiKey())
                .addHeader("Langsmith-Project", properties.project())
                .setTimeout(Duration.ofSeconds(10))
                .build();

        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"),
                        "poc_java_ia_consultamedica"
                ))
        );

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                /*
                 * BatchSpanProcessor envia em background.
                 * Assim o listener da chamada ao LLM não precisa executar HTTP bloqueante.
                 */
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        /*
         * Os hooks nativos do LangGraph4j consultam GlobalOpenTelemetry.
         * Por isso registramos este SDK globalmente antes de o grafo ser criado.
         *
         * Esta POC não espera que outro Java Agent/SDK já tenha registrado um
         * provider global. Se isso acontecer, falhamos com uma mensagem clara em
         * vez de deixar dois pipelines de tracing competindo entre si.
         */
        if (GlobalOpenTelemetry.isSet()) {
            sdk.close();
            throw new IllegalStateException(
                    "GlobalOpenTelemetry já foi configurado por outro componente. "
                            + "Remova a configuração externa ou defina LANGSMITH_TRACING=false "
                            + "para usar o pipeline de observabilidade externo."
            );
        }

        GlobalOpenTelemetry.set(sdk);

        LOGGER.info(
                "LangSmith tracing habilitado. project='{}', endpoint='{}'",
                properties.project(),
                tracesEndpoint
        );

        return sdk;
    }

    /**
     * Aceita tanto:
     * - https://api.smith.langchain.com
     * - https://api.smith.langchain.com/otel
     * - https://api.smith.langchain.com/otel/v1/traces
     *
     * Isso também facilita configurar uma instalação self-hosted com um endpoint
     * que já termine em /otel.
     */
    static String buildLangSmithOtlpTracesEndpoint(String configuredEndpoint) {
        String endpoint = configuredEndpoint == null || configuredEndpoint.isBlank()
                ? "https://api.smith.langchain.com"
                : configuredEndpoint.trim();

        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }

        if (endpoint.endsWith("/otel/v1/traces")) {
            return endpoint;
        }

        if (endpoint.endsWith("/otel")) {
            return endpoint + "/v1/traces";
        }

        return endpoint + "/otel/v1/traces";
    }

    private void validateLangSmithConfiguration(LangSmithProperties properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "LANGSMITH_TRACING está habilitado, mas LANGSMITH_API_KEY não foi configurada."
            );
        }

        if (properties.project() == null || properties.project().isBlank()) {
            throw new IllegalStateException(
                    "LANGSMITH_TRACING está habilitado, mas LANGSMITH_PROJECT não foi configurado."
            );
        }
    }
}
