package br.com.vinicius.pocjavaiaconsultamedica.ai.prompt;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt usado no último nó do grafo.
 *
 * A entrada já não é mais somente a frase original do usuário: agora temos o
 * resultado da ação (sucesso/erro), profissional, data e outras informações.
 */
@Component
public class PatientResponsePromptBuilder {

    private final ObjectMapper objectMapper;

    public PatientResponsePromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSystemPrompt() {
        Map<String, Object> prompt = Map.of(
                "role", "Friendly Medical Receptionist",
                "task", "Generate clear, professional and empathetic messages for patients",
                "tone", "Professional, warm, clear and concise",
                "guidelines", Map.of(
                        "language", "Use simple, non-technical language",
                        "format", "Be concise and avoid jargon",
                        "personalization", "Use only relevant details that were supplied",
                        "empathy", "Be polite, especially when an operation failed"
                ),
                "scenarios", Map.of(
                        "schedule_success", "Confirm the appointment and relevant details",
                        "schedule_error", "Explain why scheduling failed and suggest what to do next",
                        "cancel_success", "Confirm the cancellation",
                        "cancel_error", "Explain why cancellation failed",
                        "unknown", "Explain that this assistant handles scheduling and cancelling appointments"
                )
        );

        return writeJson(prompt);
    }

    public String buildUserPrompt(MedicalAppointmentGraphState state) {
        Map<String, Object> details = new LinkedHashMap<>();

        /*
         * Incluímos a pergunta original para que a instrução
         * "responda no mesmo idioma do paciente" seja realmente possível.
         */
        putWhenPresent(details, "patientQuestion", state.patientQuestion());
        putWhenPresent(details, "professionalName", state.professionalName().orElse(null));
        putWhenPresent(details, "datetime", state.appointmentDateTime().orElse(null));
        putWhenPresent(details, "patientName", state.patientName().orElse(null));
        putWhenPresent(details, "reason", state.reason().orElse(null));
        putWhenPresent(details, "actionError", state.actionError().orElse(null));
        putWhenPresent(details, "processingError", state.processingError().orElse(null));

        Map<String, Object> prompt = Map.of(
                "scenario", resolveScenario(state),
                "details", details,
                "instructions", List.of(
                        "Generate an appropriate message for the scenario",
                        "Answer in the same language used in patientQuestion",
                        "Use simple language",
                        "Use only details actually present in the details object",
                        "Do not invent missing names, dates, times or reasons",
                        "For an error, explain only the error actually supplied in details",
                        "For an unknown intent, guide the patient back to scheduling or cancelling"
                ),
                "examples", Map.of(
                        "schedule_success", "Sua consulta foi agendada com sucesso. Confira os dados informados acima.",
                        "schedule_error", "Não foi possível agendar a consulta porque esse horário não está disponível. Tente outro horário.",
                        "cancel_success", "Sua consulta foi cancelada com sucesso.",
                        "cancel_error", "Não encontrei uma consulta com os dados informados para realizar o cancelamento.",
                        "unknown", "Posso ajudar a agendar ou cancelar consultas médicas. Informe o que você deseja fazer."
                )
        );

        return writeJson(prompt);
    }

    private String resolveScenario(MedicalAppointmentGraphState state) {
        String intent = state.intent().externalValue();

        if ("unknown".equals(intent)) {
            return "unknown";
        }

        boolean succeeded = state.actionSucceeded().orElse(false);
        return intent + "_" + (succeeded ? "success" : "error");
    }

    private void putWhenPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível montar o prompt JSON.", exception);
        }
    }
}
