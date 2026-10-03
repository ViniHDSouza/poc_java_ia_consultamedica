package br.com.vinicius.pocjavaiaconsultamedica.graph;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointmentIntent;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Estado compartilhado entre os nós do LangGraph4j.
 *
 * Pense nele como uma "mochila" que acompanha o fluxo.
 * Cada node lê o que precisa e devolve apenas os campos que alterou.
 *
 * Os nomes das chaves principais foram mantidos próximos do GraphState
 * TypeScript original: messages, datetime, actionSuccess e error.
 */
public class MedicalAppointmentGraphState extends AgentState {

    public static final String MESSAGES = "messages";
    public static final String PATIENT_NAME = "patientName";
    public static final String INTENT = "intent";
    public static final String PROFESSIONAL_ID = "professionalId";
    public static final String PROFESSIONAL_NAME = "professionalName";
    public static final String APPOINTMENT_DATETIME = "datetime";
    public static final String REASON = "reason";

    public static final String ACTION_SUCCEEDED = "actionSuccess";
    public static final String ACTION_ERROR = "actionError";
    public static final String APPOINTMENT_DATA = "appointmentData";

    public static final String PROCESSING_ERROR = "error";

    /*
     * Campo adicional da migração Java.
     * Facilita devolver a mensagem final diretamente na API sem obrigar o
     * controller a conhecer a posição do último item de messages.
     */
    public static final String RESPONSE_MESSAGE = "responseMessage";

    public MedicalAppointmentGraphState(Map<String, Object> initialData) {
        super(initialData);
    }

    @SuppressWarnings("unchecked")
    public List<String> messages() {
        return this.<List<String>>value(MESSAGES).orElse(List.of());
    }

    /**
     * Nesta POC cada requisição REST representa uma conversa de um turno.
     * O primeiro item de messages é a pergunta original do paciente.
     */
    public String patientQuestion() {
        List<String> currentMessages = messages();

        if (currentMessages.isEmpty()) {
            throw new IllegalStateException("messages não contém a pergunta do paciente.");
        }

        return currentMessages.getFirst();
    }

    public Optional<String> patientName() {
        return value(PATIENT_NAME);
    }

    public MedicalAppointmentIntent intent() {
        String value = this.<String>value(INTENT)
                .orElse(MedicalAppointmentIntent.UNKNOWN.externalValue());

        return MedicalAppointmentIntent.fromExternalValue(value);
    }

    public Optional<Long> professionalId() {
        Number value = this.<Number>value(PROFESSIONAL_ID).orElse(null);

        return value == null
                ? Optional.empty()
                : Optional.of(value.longValue());
    }

    public Optional<String> professionalName() {
        return value(PROFESSIONAL_NAME);
    }

    public Optional<String> appointmentDateTime() {
        return value(APPOINTMENT_DATETIME);
    }

    public Optional<String> reason() {
        return value(REASON);
    }

    public Optional<Boolean> actionSucceeded() {
        return value(ACTION_SUCCEEDED);
    }

    public Optional<String> actionError() {
        return value(ACTION_ERROR);
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> appointmentData() {
        return value(APPOINTMENT_DATA);
    }

    public Optional<String> processingError() {
        return value(PROCESSING_ERROR);
    }

    public Optional<String> responseMessage() {
        return value(RESPONSE_MESSAGE);
    }
}
