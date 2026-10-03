package br.com.vinicius.pocjavaiaconsultamedica.domain;

import java.time.Instant;

/**
 * Consulta armazenada em memória nesta POC.
 *
 * Em um sistema real, este objeto provavelmente seria persistido em um banco de dados.
 */
public record MedicalAppointment(
        Instant dateTime,
        String patientName,
        String reason,
        long professionalId
) {
}
