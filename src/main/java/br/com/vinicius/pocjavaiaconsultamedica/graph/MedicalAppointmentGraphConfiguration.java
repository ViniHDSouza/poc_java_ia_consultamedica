package br.com.vinicius.pocjavaiaconsultamedica.graph;

import br.com.vinicius.pocjavaiaconsultamedica.config.LangSmithProperties;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.CancelMedicalAppointmentNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.GeneratePatientResponseNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.IdentifyPatientAppointmentIntentNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.node.ScheduleMedicalAppointmentNode;
import br.com.vinicius.pocjavaiaconsultamedica.graph.route.RoutePatientRequestByIntent;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.otel.OTELWrapCallTraceHook;
import org.bsc.langgraph4j.otel.OTELWrapCallTraceSetParentHook;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Desenha o workflow da aplicação.
 *
 * START
 *   -> identifyIntent
 *      -> schedule -> message -> END
 *      -> cancel   -> message -> END
 *      -> message  -> END
 *
 * O mesmo StateGraph é usado:
 * - pela API REST;
 * - pelo LangGraph4j Studio.
 */
@Configuration
public class MedicalAppointmentGraphConfiguration {

    @Bean
    StateGraph<MedicalAppointmentGraphState> medicalAppointmentStateGraph(
            IdentifyPatientAppointmentIntentNode identifyIntentNode,
            ScheduleMedicalAppointmentNode scheduleNode,
            CancelMedicalAppointmentNode cancelNode,
            GeneratePatientResponseNode messageNode,
            RoutePatientRequestByIntent routeByIntent,
            LangSmithProperties langSmithProperties,
            OTELWrapCallTraceHook<MedicalAppointmentGraphState> traceHook,
            OTELWrapCallTraceSetParentHook<MedicalAppointmentGraphState> parentTraceHook
    ) throws GraphStateException {

        StateGraph<MedicalAppointmentGraphState> graph =
                new StateGraph<>(MedicalAppointmentGraphState::new);

        /*
         * A ordem abaixo segue o padrão recomendado pelo LangGraph4j:
         * um hook cria spans de node/edge e o outro cria o agrupamento pai.
         */
        if (langSmithProperties.isTracingEnabled()) {
            graph.addWrapCallNodeHook(traceHook)
                    .addWrapCallEdgeHook(traceHook)
                    .addWrapCallNodeHook(parentTraceHook)
                    .addWrapCallEdgeHook(parentTraceHook);
        }

        return graph
                .addNode("identifyIntent", node_async(identifyIntentNode))
                .addNode("schedule", node_async(scheduleNode))
                .addNode("cancel", node_async(cancelNode))
                .addNode("message", node_async(messageNode))

                .addEdge(START, "identifyIntent")

                .addConditionalEdges(
                        "identifyIntent",
                        edge_async(routeByIntent),
                        Map.of(
                                "schedule", "schedule",
                                "cancel", "cancel",
                                "message", "message"
                        )
                )

                .addEdge("schedule", "message")
                .addEdge("cancel", "message")
                .addEdge("message", END);
    }

    @Bean
    CompiledGraph<MedicalAppointmentGraphState> medicalAppointmentGraph(
            StateGraph<MedicalAppointmentGraphState> medicalAppointmentStateGraph
    ) throws GraphStateException {
        return medicalAppointmentStateGraph.compile(
                CompileConfig.builder()
                        .graphId("medical_appointments")
                        .checkpointSaver(new MemorySaver())
                        /*
                         * A API é single-turn nesta POC.
                         * Após a execução, não precisamos reter threads indefinidamente.
                         */
                        .releaseThread(true)
                        .build()
        );
    }
}
