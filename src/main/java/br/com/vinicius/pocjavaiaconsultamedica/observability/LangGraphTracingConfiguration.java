package br.com.vinicius.pocjavaiaconsultamedica.observability;

import br.com.vinicius.pocjavaiaconsultamedica.config.LangSmithProperties;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import org.bsc.langgraph4j.otel.OTELWrapCallTraceHook;
import org.bsc.langgraph4j.otel.OTELWrapCallTraceSetParentHook;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cria os hooks OpenTelemetry usados pelo LangGraph4j.
 *
 * Os hooks não são adicionados automaticamente ao grafo. A classe
 * MedicalAppointmentGraphConfiguration decide se irá registrá-los com base
 * em LANGSMITH_TRACING.
 */
@Configuration
public class LangGraphTracingConfiguration {

    @Bean
    ObjectStreamStateSerializer<MedicalAppointmentGraphState> medicalAppointmentStateSerializer() {
        return new ObjectStreamStateSerializer<>(MedicalAppointmentGraphState::new);
    }

    @Bean
    OTELWrapCallTraceHook<MedicalAppointmentGraphState> medicalAppointmentNodeAndEdgeTraceHook(
            OpenTelemetry openTelemetry,
            ObjectStreamStateSerializer<MedicalAppointmentGraphState> stateSerializer
    ) {
        /*
         * openTelemetry é recebido mesmo não sendo usado diretamente.
         * Isso força a inicialização/registro do SDK global antes dos hooks.
         */
        return new OTELWrapCallTraceHook<>(stateSerializer);
    }

    @Bean
    OTELWrapCallTraceSetParentHook<MedicalAppointmentGraphState> medicalAppointmentParentTraceHook(
            OpenTelemetry openTelemetry,
            LangSmithProperties properties
    ) {
        return OTELWrapCallTraceSetParentHook
                .<MedicalAppointmentGraphState>builder()
                .scope("poc_java_ia_consultamedica")
                .groupName("medical_appointments")
                .groupAttributes(Attributes.builder()
                        .put("langsmith.trace.name", "medical_appointments")
                        .put("langsmith.span.kind", "chain")
                        .put("langsmith.metadata.framework", "langgraph4j")
                        .put("langsmith.metadata.langsmith_enabled", properties.isTracingEnabled())
                        .build())
                .build();
    }
}
