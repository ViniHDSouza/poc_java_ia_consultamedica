package br.com.vinicius.pocjavaiaconsultamedica.ai;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;

/**
 * Contrato responsável por transformar o resultado técnico do grafo
 * em uma mensagem clara para o paciente.
 */
public interface PatientResponseGenerator {

    String generatePatientResponse(MedicalAppointmentGraphState state);
}
