package br.com.vinicius.pocjavaiaconsultamedica.api;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;

import java.util.List;
import java.util.Map;

/**
 * Resposta HTTP baseada no estado final.
 *
 * "messages", "datetime", "actionSuccess" e "error" aproximam a resposta
 * da estrutura devolvida pelo projeto JavaScript original.
 *
 * Os campos auxiliares "processingError" e "message" tornam a versão Java
 * mais fácil de consumir por quem está estudando.
 */
public record MedicalAppointmentChatResponse(
        List<String> messages,
        String intent,
        Long professionalId,
        String professionalName,
        String datetime,
        String patientName,
        String reason,
        Boolean actionSuccess,
        String actionError,
        String error,
        String processingError,
        Map<String, Object> appointmentData,
        String message
) {

    public static MedicalAppointmentChatResponse fromState(
            MedicalAppointmentGraphState state
    ) {
        String processingError = state.processingError().orElse(null);

        return new MedicalAppointmentChatResponse(
                state.messages(),
                state.intent().externalValue(),
                state.professionalId().orElse(null),
                state.professionalName().orElse(null),
                state.appointmentDateTime().orElse(null),
                state.patientName().orElse(null),
                state.reason().orElse(null),
                state.actionSucceeded().orElse(null),
                state.actionError().orElse(null),
                processingError,
                processingError,
                state.appointmentData().orElse(null),
                state.responseMessage().orElse(null)
        );
    }
}
