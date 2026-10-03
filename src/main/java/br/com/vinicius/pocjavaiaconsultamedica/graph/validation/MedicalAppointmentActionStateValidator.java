package br.com.vinicius.pocjavaiaconsultamedica.graph.validation;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Faz a "segunda validação" ensinada na aula.
 *
 * Mesmo que a IA tenha devolvido um JSON válido, o nó responsável pela ação
 * não deve confiar cegamente que todos os campos necessários estão presentes.
 */
@Component
public class MedicalAppointmentActionStateValidator {

    public List<String> findMissingRequiredFields(MedicalAppointmentGraphState state) {
        List<String> errors = new ArrayList<>();

        if (state.professionalId().isEmpty()) {
            errors.add("Professional ID is required");
        }
        if (state.appointmentDateTime().isEmpty()) {
            errors.add("Appointment datetime is required");
        }
        if (state.patientName().filter(name -> !name.isBlank()).isEmpty()) {
            errors.add("Patient name is required");
        }

        return errors;
    }
}
