package br.com.vinicius.pocjavaiaconsultamedica.ai;

import br.com.vinicius.pocjavaiaconsultamedica.ai.model.ExtractedMedicalAppointmentIntent;

/**
 * Contrato responsável por transformar linguagem natural em dados estruturados.
 */
public interface PatientRequestInterpreter {

    ExtractedMedicalAppointmentIntent interpretPatientRequest(String patientQuestion);
}
