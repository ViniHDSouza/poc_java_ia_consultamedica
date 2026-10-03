package br.com.vinicius.pocjavaiaconsultamedica.graph.node;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointment;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import br.com.vinicius.pocjavaiaconsultamedica.graph.validation.MedicalAppointmentActionStateValidator;
import br.com.vinicius.pocjavaiaconsultamedica.service.MedicalAppointmentService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nó responsável por EXECUTAR o agendamento.
 *
 * A IA já terminou a parte de interpretação. Daqui em diante a ação é Java tradicional.
 */
@Component
public class ScheduleMedicalAppointmentNode implements NodeAction<MedicalAppointmentGraphState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleMedicalAppointmentNode.class);

    private final MedicalAppointmentService medicalAppointmentService;
    private final MedicalAppointmentActionStateValidator stateValidator;

    public ScheduleMedicalAppointmentNode(
            MedicalAppointmentService medicalAppointmentService,
            MedicalAppointmentActionStateValidator stateValidator
    ) {
        this.medicalAppointmentService = medicalAppointmentService;
        this.stateValidator = stateValidator;
    }

    @Override
    public Map<String, Object> apply(MedicalAppointmentGraphState state) {
        LOGGER.info("Agendando consulta médica...");

        List<String> validationErrors = stateValidator.findMissingRequiredFields(state);
        if (!validationErrors.isEmpty()) {
            String errorMessage = String.join(", ", validationErrors);
            LOGGER.warn("Não foi possível agendar: {}", errorMessage);

            return Map.of(
                    MedicalAppointmentGraphState.ACTION_SUCCEEDED, false,
                    MedicalAppointmentGraphState.ACTION_ERROR, errorMessage
            );
        }

        try {
            MedicalAppointment appointment = medicalAppointmentService.scheduleMedicalAppointment(
                    state.professionalId().orElseThrow(),
                    Instant.parse(state.appointmentDateTime().orElseThrow()),
                    state.patientName().orElseThrow(),
                    state.reason().orElse("general consultation")
            );

            LOGGER.info("Consulta agendada com sucesso.");

            return Map.of(
                    MedicalAppointmentGraphState.ACTION_SUCCEEDED, true,
                    MedicalAppointmentGraphState.APPOINTMENT_DATA, toStateMap(appointment)
            );
        } catch (Exception exception) {
            LOGGER.warn("Falha no agendamento: {}", exception.getMessage());

            return Map.of(
                    MedicalAppointmentGraphState.ACTION_SUCCEEDED, false,
                    MedicalAppointmentGraphState.ACTION_ERROR, readableMessage(exception)
            );
        }
    }

    private Map<String, Object> toStateMap(MedicalAppointment appointment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dateTime", appointment.dateTime().toString());
        data.put("patientName", appointment.patientName());
        data.put("reason", appointment.reason());
        data.put("professionalId", appointment.professionalId());
        return data;
    }

    private String readableMessage(Exception exception) {
        return exception.getMessage() == null ? "Scheduling failed" : exception.getMessage();
    }
}
