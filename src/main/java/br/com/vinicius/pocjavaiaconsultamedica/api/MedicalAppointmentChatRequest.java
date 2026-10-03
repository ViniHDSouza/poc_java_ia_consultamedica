package br.com.vinicius.pocjavaiaconsultamedica.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON de entrada da API.
 */
public record MedicalAppointmentChatRequest(
        @NotBlank(message = "question é obrigatória")
        @Size(min = 10, message = "question deve ter pelo menos 10 caracteres")
        String question
) {
}
