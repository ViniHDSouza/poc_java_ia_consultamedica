package br.com.vinicius.pocjavaiaconsultamedica.domain;

/**
 * Profissional que pode receber uma consulta.
 */
public record MedicalProfessional(
        long id,
        String name,
        String specialty
) {
}
