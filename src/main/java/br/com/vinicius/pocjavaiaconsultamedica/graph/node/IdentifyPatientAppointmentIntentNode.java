package br.com.vinicius.pocjavaiaconsultamedica.graph.node;

import br.com.vinicius.pocjavaiaconsultamedica.ai.PatientRequestInterpreter;
import br.com.vinicius.pocjavaiaconsultamedica.ai.model.ExtractedMedicalAppointmentIntent;
import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointmentIntent;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Primeiro nó do grafo.
 *
 * Converte a frase livre do paciente em campos estruturados.
 */
@Component
public class IdentifyPatientAppointmentIntentNode implements NodeAction<MedicalAppointmentGraphState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifyPatientAppointmentIntentNode.class);

    private final PatientRequestInterpreter patientRequestInterpreter;

    public IdentifyPatientAppointmentIntentNode(PatientRequestInterpreter patientRequestInterpreter) {
        this.patientRequestInterpreter = patientRequestInterpreter;
    }

    @Override
    public Map<String, Object> apply(MedicalAppointmentGraphState state) {
        LOGGER.info("Identificando a intenção do paciente...");

        try {
            ExtractedMedicalAppointmentIntent extracted =
                    patientRequestInterpreter.interpretPatientRequest(state.patientQuestion());

            MedicalAppointmentIntent identifiedIntent =
                    MedicalAppointmentIntent.fromExternalValue(extracted.intent());

            Map<String, Object> stateUpdate = new HashMap<>();
            stateUpdate.put(MedicalAppointmentGraphState.INTENT, identifiedIntent.externalValue());

            putWhenPresent(stateUpdate, MedicalAppointmentGraphState.PROFESSIONAL_ID, extracted.professionalId());
            putWhenPresent(stateUpdate, MedicalAppointmentGraphState.PROFESSIONAL_NAME, extracted.professionalName());
            putWhenPresent(stateUpdate, MedicalAppointmentGraphState.APPOINTMENT_DATETIME, extracted.datetime());
            putWhenPresent(stateUpdate, MedicalAppointmentGraphState.PATIENT_NAME, extracted.patientName());
            putWhenPresent(stateUpdate, MedicalAppointmentGraphState.REASON, extracted.reason());

            LOGGER.info("Intenção identificada: {}", identifiedIntent.externalValue());
            return stateUpdate;
        } catch (Exception exception) {
            LOGGER.error("Falha ao identificar a intenção do paciente.", exception);

            return Map.of(
                    MedicalAppointmentGraphState.INTENT, MedicalAppointmentIntent.UNKNOWN.externalValue(),
                    MedicalAppointmentGraphState.PROCESSING_ERROR, readableMessage(exception)
            );
        }
    }

    private void putWhenPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String readableMessage(Exception exception) {
        return exception.getMessage() == null ? "Intent identification failed" : exception.getMessage();
    }
}
