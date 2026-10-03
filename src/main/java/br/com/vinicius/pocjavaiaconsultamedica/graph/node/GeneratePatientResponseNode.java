package br.com.vinicius.pocjavaiaconsultamedica.graph.node;

import br.com.vinicius.pocjavaiaconsultamedica.ai.PatientResponseGenerator;
import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointmentIntent;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Último node do fluxo.
 *
 * Ele transforma o estado técnico do grafo em texto amigável e acrescenta
 * a resposta em "messages", como faz o messageGeneratorNode TypeScript original.
 */
@Component
public class GeneratePatientResponseNode implements NodeAction<MedicalAppointmentGraphState> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GeneratePatientResponseNode.class);

    private final PatientResponseGenerator patientResponseGenerator;

    public GeneratePatientResponseNode(
            PatientResponseGenerator patientResponseGenerator
    ) {
        this.patientResponseGenerator = patientResponseGenerator;
    }

    @Override
    public Map<String, Object> apply(MedicalAppointmentGraphState state) {
        LOGGER.info("Gerando mensagem final para o paciente...");

        String patientMessage;

        try {
            patientMessage = patientResponseGenerator.generatePatientResponse(state);
        } catch (Exception exception) {
            LOGGER.error(
                    "Falha ao gerar a mensagem com IA. Usando fallback determinístico.",
                    exception
            );

            patientMessage = buildFallbackMessage(state);
        }

        return Map.of(
                MedicalAppointmentGraphState.MESSAGES,
                appendMessage(state.messages(), patientMessage),
                MedicalAppointmentGraphState.RESPONSE_MESSAGE,
                patientMessage
        );
    }

    private List<String> appendMessage(
            List<String> currentMessages,
            String newMessage
    ) {
        List<String> updatedMessages = new ArrayList<>(currentMessages);
        updatedMessages.add(newMessage);
        return List.copyOf(updatedMessages);
    }

    private String buildFallbackMessage(MedicalAppointmentGraphState state) {
        MedicalAppointmentIntent intent = state.intent();

        if (intent == MedicalAppointmentIntent.UNKNOWN) {
            return "Posso ajudar a agendar ou cancelar consultas médicas. "
                    + "Informe o que deseja fazer.";
        }

        if (state.actionSucceeded().orElse(false)) {
            return intent == MedicalAppointmentIntent.SCHEDULE
                    ? "A consulta foi agendada com sucesso."
                    : "A consulta foi cancelada com sucesso.";
        }

        String reason = state.actionError()
                .or(() -> state.processingError())
                .orElse("não foi possível concluir a operação");

        return "Desculpe, " + reason + ".";
    }
}
