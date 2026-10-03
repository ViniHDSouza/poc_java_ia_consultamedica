package br.com.vinicius.pocjavaiaconsultamedica.graph;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Esconde do controller os detalhes da API do LangGraph4j.
 */
@Service
public class MedicalAppointmentGraphExecutor {

    private final CompiledGraph<MedicalAppointmentGraphState> medicalAppointmentGraph;

    public MedicalAppointmentGraphExecutor(
            CompiledGraph<MedicalAppointmentGraphState> medicalAppointmentGraph
    ) {
        this.medicalAppointmentGraph = medicalAppointmentGraph;
    }

    public MedicalAppointmentGraphState processPatientQuestion(String patientQuestion) {
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(UUID.randomUUID().toString())
                .build();

        /*
         * O projeto TypeScript inicializa o State com:
         * messages: [new HumanMessage(question)]
         *
         * Aqui preservamos o mesmo conceito usando List<String>.
         * Para esta POC de um único turno, não precisamos colocar objetos específicos
         * do provider/LLM dentro do State.
         */
        return medicalAppointmentGraph.invoke(
                        GraphInput.args(Map.of(
                                MedicalAppointmentGraphState.MESSAGES,
                                List.of(patientQuestion)
                        )),
                        runnableConfig
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "O LangGraph4j terminou sem produzir estado final."
                        )
                );
    }
}
