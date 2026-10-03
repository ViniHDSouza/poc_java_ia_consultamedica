package br.com.vinicius.pocjavaiaconsultamedica.ai.model;

/**
 * Structured Output usado para a última etapa do fluxo:
 * converter o estado técnico da aplicação em uma mensagem amigável.
 */
public record GeneratedPatientMessage(
        String message
) {
}
