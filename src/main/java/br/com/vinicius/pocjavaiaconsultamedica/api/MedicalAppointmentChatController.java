package br.com.vinicius.pocjavaiaconsultamedica.api;

import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphExecutor;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint REST da POC.
 *
 * POST /chat
 *   Mantém compatibilidade com o projeto JavaScript original e com as aulas.
 *
 * POST /api/medical-appointments/chat
 *   Alias mais descritivo para quem estiver lendo o projeto Java.
 */
@RestController
public class MedicalAppointmentChatController {

    private final MedicalAppointmentGraphExecutor graphExecutor;

    public MedicalAppointmentChatController(MedicalAppointmentGraphExecutor graphExecutor) {
        this.graphExecutor = graphExecutor;
    }

    @PostMapping({"/chat", "/api/medical-appointments/chat"})
    public ResponseEntity<MedicalAppointmentChatResponse> processPatientQuestion(
            @Valid @RequestBody MedicalAppointmentChatRequest request
    ) {
        MedicalAppointmentGraphState finalState =
                graphExecutor.processPatientQuestion(request.question());

        return ResponseEntity.ok(MedicalAppointmentChatResponse.fromState(finalState));
    }
}
