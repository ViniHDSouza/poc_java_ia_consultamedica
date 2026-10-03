package br.com.vinicius.pocjavaiaconsultamedica.ai;

import br.com.vinicius.pocjavaiaconsultamedica.ai.model.GeneratedPatientMessage;
import br.com.vinicius.pocjavaiaconsultamedica.ai.prompt.PatientResponsePromptBuilder;
import br.com.vinicius.pocjavaiaconsultamedica.ai.schema.MedicalAppointmentStructuredOutputSchemas;
import br.com.vinicius.pocjavaiaconsultamedica.graph.MedicalAppointmentGraphState;
import org.springframework.stereotype.Service;

/**
 * Gera a mensagem final que será apresentada ao paciente.
 */
@Service
public class OpenRouterPatientResponseGenerator implements PatientResponseGenerator {

    private final OpenRouterStructuredOutputClient structuredOutputClient;
    private final PatientResponsePromptBuilder promptBuilder;

    public OpenRouterPatientResponseGenerator(
            OpenRouterStructuredOutputClient structuredOutputClient,
            PatientResponsePromptBuilder promptBuilder
    ) {
        this.structuredOutputClient = structuredOutputClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String generatePatientResponse(MedicalAppointmentGraphState state) {
        GeneratedPatientMessage generatedMessage = structuredOutputClient.generateStructuredOutput(
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(state),
                MedicalAppointmentStructuredOutputSchemas.patientMessageSchema(),
                GeneratedPatientMessage.class
        );

        if (generatedMessage.message() == null || generatedMessage.message().isBlank()) {
            throw new IllegalStateException("A IA retornou uma mensagem vazia.");
        }

        /*
         * O MessageSchema Zod do projeto TypeScript exige min(10).
         * Mantemos a mesma regra na fronteira Java após desserializar o JSON.
         */
        if (generatedMessage.message().trim().length() < 10) {
            throw new IllegalStateException(
                    "A IA retornou uma mensagem menor que 10 caracteres."
            );
        }

        return generatedMessage.message();
    }
}
