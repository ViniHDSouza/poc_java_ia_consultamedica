package br.com.vinicius.pocjavaiaconsultamedica.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LangSmithOpenTelemetryConfigurationTest {

    @Test
    void shouldBuildLangSmithOtlpEndpointFromBaseUrl() {
        assertThat(
                LangSmithOpenTelemetryConfiguration.buildLangSmithOtlpTracesEndpoint(
                        "https://api.smith.langchain.com"
                )
        ).isEqualTo("https://api.smith.langchain.com/otel/v1/traces");
    }

    @Test
    void shouldAppendOnlyTracesPathWhenEndpointAlreadyEndsWithOtel() {
        assertThat(
                LangSmithOpenTelemetryConfiguration.buildLangSmithOtlpTracesEndpoint(
                        "https://api.smith.langchain.com/otel/"
                )
        ).isEqualTo("https://api.smith.langchain.com/otel/v1/traces");
    }

    @Test
    void shouldKeepCompleteOtlpTracesEndpoint() {
        assertThat(
                LangSmithOpenTelemetryConfiguration.buildLangSmithOtlpTracesEndpoint(
                        "https://api.smith.langchain.com/otel/v1/traces"
                )
        ).isEqualTo("https://api.smith.langchain.com/otel/v1/traces");
    }
}
