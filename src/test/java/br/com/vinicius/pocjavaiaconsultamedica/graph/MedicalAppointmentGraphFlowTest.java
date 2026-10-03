package br.com.vinicius.pocjavaiaconsultamedica.graph;

import br.com.vinicius.pocjavaiaconsultamedica.ai.PatientRequestInterpreter;
import br.com.vinicius.pocjavaiaconsultamedica.ai.PatientResponseGenerator;
import br.com.vinicius.pocjavaiaconsultamedica.ai.model.ExtractedMedicalAppointmentIntent;
import br.com.vinicius.pocjavaiaconsultamedica.config.LangSmithProperties;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.CancelMedicalAppointmentNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.GeneratePatientResponseNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.IdentifyPatientAppointmentIntentNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.ScheduleMedicalAppointmentNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.route.RoutePatientRequestByIntent;
import br.com.vinicius.pocjavaiaconsultamedica.graph.validation.MedicalAppointmentActionStateValidator;
import br.com.vinicius.pocjavaiaconsultamedica.repository.InMemoryMedicalAppointmentRepository;
import br.com.vinicius.pocjavaiaconsultamedica.service.MedicalAppointmentService;
import br.com.vinicius.pocjavaiaconsultamedica.service.MedicalProfessionalDirectory;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.otel.OTELWrapCallTraceHook;
import org.bsc.langgraph4j.otel.OTELWrapCallTraceSetParentHook;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o workflow sem chamar uma IA externa.
 *
 * O objetivo é validar LangGraph4j + regras Java sem gastar tokens.
 * O tracing fica desligado neste teste porque observabilidade não deve ser
 * pré-requisito para testar a regra de negócio.
 */
class MedicalAppointmentGraphFlowTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldScheduleAppointmentWhenIntentIsSchedule() throws Exception {
        PatientRequestInterpreter interpreter = question ->
                new ExtractedMedicalAppointmentIntent(
                        "schedule",
                        1L,
                        "Dr. Alicio da Silva",
                        "2026-10-04T16:00:00Z",
                        "Maria Santos",
                        "check-up regular"
                );

        PatientResponseGenerator responseGenerator =
                state -> "Consulta agendada com sucesso.";

        var graph = buildGraph(interpreter, responseGenerator);

        MedicalAppointmentGraphState finalState = graph.invoke(
                GraphInput.args(Map.of(
                        MedicalAppointmentGraphState.MESSAGES,
                        List.of("Quero marcar uma consulta amanhã às 16h.")
                )),
                RunnableConfig.empty()
        ).orElseThrow();

        assertThat(finalState.intent().externalValue()).isEqualTo("schedule");
        assertThat(finalState.actionSucceeded()).contains(true);
        assertThat(finalState.responseMessage())
                .contains("Consulta agendada com sucesso.");
    }

    @Test
    void shouldRouteUnknownIntentDirectlyToMessageNode() throws Exception {
        PatientRequestInterpreter interpreter = question ->
                new ExtractedMedicalAppointmentIntent(
                        "unknown",
                        null,
                        null,
                        null,
                        null,
                        null
                );

        PatientResponseGenerator responseGenerator =
                state -> "Posso ajudar a agendar ou cancelar consultas médicas.";

        var graph = buildGraph(interpreter, responseGenerator);

        MedicalAppointmentGraphState finalState = graph.invoke(
                GraphInput.args(Map.of(
                        MedicalAppointmentGraphState.MESSAGES,
                        List.of("Qual a previsão do tempo para amanhã?")
                )),
                RunnableConfig.empty()
        ).orElseThrow();

        assertThat(finalState.intent().externalValue()).isEqualTo("unknown");
        assertThat(finalState.actionSucceeded()).isEmpty();
        assertThat(finalState.responseMessage().orElseThrow()).contains("agendar ou cancelar");
    }

    private org.bsc.langgraph4j.CompiledGraph<MedicalAppointmentGraphState> buildGraph(
            PatientRequestInterpreter interpreter,
            PatientResponseGenerator responseGenerator
    ) throws Exception {

        MedicalProfessionalDirectory directory =
                new MedicalProfessionalDirectory();

        InMemoryMedicalAppointmentRepository repository =
                new InMemoryMedicalAppointmentRepository(FIXED_CLOCK);

        MedicalAppointmentService appointmentService =
                new MedicalAppointmentService(repository, directory);

        MedicalAppointmentActionStateValidator validator =
                new MedicalAppointmentActionStateValidator();

        var identifyNode =
                new IdentifyPatientAppointmentIntentNode(interpreter);
        var scheduleNode =
                new ScheduleMedicalAppointmentNode(appointmentService, validator);
        var cancelNode =
                new CancelMedicalAppointmentNode(appointmentService, validator);
        var messageNode =
                new GeneratePatientResponseNode(responseGenerator);
        var router =
                new RoutePatientRequestByIntent();

        LangSmithProperties tracingDisabled = new LangSmithProperties(
                "",
                false,
                "test-project",
                "https://api.smith.langchain.com",
                false
        );

        ObjectStreamStateSerializer<MedicalAppointmentGraphState> serializer =
                new ObjectStreamStateSerializer<>(MedicalAppointmentGraphState::new);

        OTELWrapCallTraceHook<MedicalAppointmentGraphState> traceHook =
                new OTELWrapCallTraceHook<>(serializer);

        OTELWrapCallTraceSetParentHook<MedicalAppointmentGraphState> parentHook =
                OTELWrapCallTraceSetParentHook
                        .<MedicalAppointmentGraphState>builder()
                        .scope("test")
                        .groupName("medical_appointments_test")
                        .build();

        MedicalAppointmentGraphConfiguration configuration =
                new MedicalAppointmentGraphConfiguration();

        var stateGraph = configuration.medicalAppointmentStateGraph(
                identifyNode,
                scheduleNode,
                cancelNode,
                messageNode,
                router,
                tracingDisabled,
                traceHook,
                parentHook
        );

        return configuration.medicalAppointmentGraph(stateGraph);
    }
}
