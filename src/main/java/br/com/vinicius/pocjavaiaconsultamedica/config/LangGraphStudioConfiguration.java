package br.com.vinicius.pocjavaiaconsultamedica.config;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Expõe o mesmo workflow no LangGraph4j Studio.
 *
 * Abra:
 * http://localhost:3000/?instance=medical_appointments
 *
 * O campo de entrada do Studio chama-se "messages", como no GraphState
 * do projeto JavaScript. O valor digitado é convertido para List<String>.
 */
@Configuration
public class LangGraphStudioConfiguration extends LangGraphStudioConfig {

    private final StateGraph<MedicalAppointmentGraphState> medicalAppointmentStateGraph;

    public LangGraphStudioConfiguration(
            StateGraph<MedicalAppointmentGraphState> medicalAppointmentStateGraph
    ) {
        this.medicalAppointmentStateGraph = medicalAppointmentStateGraph;
    }

    @Override
    public Map<String, LangGraphStudioServer.Instance> instanceMap() {
        LangGraphStudioServer.Instance instance =
                LangGraphStudioServer.Instance.builder()
                        .title("POC Java IA - Consulta Médica")
                        .addInputStringArg(
                                MedicalAppointmentGraphState.MESSAGES,
                                true,
                                value -> List.of(Objects.toString(value))
                        )
                        .graph(medicalAppointmentStateGraph)
                        .compileConfig(
                                CompileConfig.builder()
                                        .graphId("medical_appointments")
                                        .checkpointSaver(new MemorySaver())
                                        .releaseThread(true)
                                        .build()
                        )
                        .build();

        return Map.of("medical_appointments", instance);
    }
}
