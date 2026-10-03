package br.com.vinicius.pocjavaiaconsultamedica.graph.node;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import br.com.vinicius.pocjavaiaconsultamedica.graph.validation.MedicalAppointmentActionStateValidator;
import br.com.vinicius.pocjavaiaconsultamedica.service.MedicalAppointmentService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Nó responsável por executar o cancelamento da consulta.
 */
@Component
public class CancelMedicalAppointmentNode implements NodeAction<MedicalAppointmentGraphState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelMedicalAppointmentNode.class);

    private final MedicalAppointmentService medicalAppointmentService;
    private final MedicalAppointmentActionStateValidator stateValidator;

    public CancelMedicalAppointmentNode(
            MedicalAppointmentService medicalAppointmentService,
            MedicalAppointmentActionStateValidator stateValidator
    ) {
        this.medicalAppointmentService = medicalAppointmentService;
        this.stateValidator = stateValidator;
    }

    @Override
    public Map<String, Object> apply(MedicalAppointmentGraphState state) {
        LOGGER.info("Cancelando consulta médica...");

        List<String> validationErrors = stateValidator.findMissingRequiredFields(state);
        if (!validationErrors.isEmpty()) {
            String errorMessage = String.join(", ", validationErrors);
            LOGGER.warn("Não foi possível cancelar: {}", errorMessage);

            return Map.of(
                    MedicalAppointmentGraphState.ACTION_SUCCEEDED, false,
                    MedicalAppointmentGraphState.ACTION_ERROR, errorMessage
            );
        }

        try {
            medicalAppointmentService.cancelMedicalAppointment(
                    state.professionalId().orElseThrow(),
                    state.patientName().orElseThrow(),
                    Instant.parse(state.appointmentDateTime().orElseThrow())
            );

            LOGGER.info("Consulta cancelada com sucesso.");
            return Map.of(MedicalAppointmentGraphState.ACTION_SUCCEEDED, true);
        } catch (Exception exception) {
            LOGGER.warn("Falha no cancelamento: {}", exception.getMessage());

            return Map.of(
                    MedicalAppointmentGraphState.ACTION_SUCCEEDED, false,
                    MedicalAppointmentGraphState.ACTION_ERROR, readableMessage(exception)
            );
        }
    }

    private String readableMessage(Exception exception) {
        return exception.getMessage() == null ? "Cancellation failed" : exception.getMessage();
    }
}
