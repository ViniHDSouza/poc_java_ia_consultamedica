package br.com.vinicius.pocjavaiaconsultamedica.graph.route;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointmentIntent;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import org.bsc.langgraph4j.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aresta condicional do LangGraph4j.
 *
 * O próximo nó é escolhido usando o intent que a IA colocou no estado.
 */
@Component
public class RoutePatientRequestByIntent implements EdgeAction<MedicalAppointmentGraphState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutePatientRequestByIntent.class);

    @Override
    public String apply(MedicalAppointmentGraphState state) {
        if (state.processingError().isPresent()) {
            LOGGER.info("Houve erro na interpretação. Roteando diretamente para a mensagem final.");
            return "message";
        }

        MedicalAppointmentIntent intent = state.intent();

        return switch (intent) {
            case SCHEDULE -> "schedule";
            case CANCEL -> "cancel";
            case UNKNOWN -> "message";
        };
    }
}
