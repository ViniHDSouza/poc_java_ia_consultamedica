package br.com.vinicius.pocjavaiaconsultamedica.ai.model;

/**
 * Resultado estruturado retornado pela IA ao interpretar a frase do paciente.
 *
 * Todos os campos, exceto intent, podem ser nulos porque o paciente pode enviar
 * uma frase incompleta. A validação de negócio acontece novamente no nó que executa a ação.
 */
public record ExtractedMedicalAppointmentIntent(
        String intent,
        Long professionalId,
        String professionalName,
        String datetime,
        String patientName,
        String reason
) {
}
