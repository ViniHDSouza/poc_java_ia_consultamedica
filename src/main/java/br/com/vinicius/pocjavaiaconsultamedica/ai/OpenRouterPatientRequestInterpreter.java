package br.com.vinicius.pocjavaiaconsultamedica.ai;

import br.com.vinicius.pocjavaiaconsultamedica.ai.model.ExtractedMedicalAppointmentIntent;
import br.com.vinicius.pocjavaiaconsultamedica.ai.prompt.MedicalAppointmentIntentPromptBuilder;
import br.com.vinicius.pocjavaiaconsultamedica.ai.schema.MedicalAppointmentStructuredOutputSchemas;
import br.com.vinicius.pocjavaiaconsultamedica.service.MedicalProfessionalDirectory;
import org.springframework.stereotype.Service;

/**
 * Implementação real do interpretador usando OpenRouter + LangChain4j.
 */
@Service
public class OpenRouterPatientRequestInterpreter implements PatientRequestInterpreter {

    private final OpenRouterStructuredOutputClient structuredOutputClient;
    private final MedicalAppointmentIntentPromptBuilder promptBuilder;
    private final MedicalProfessionalDirectory professionalDirectory;

    public OpenRouterPatientRequestInterpreter(
            OpenRouterStructuredOutputClient structuredOutputClient,
            MedicalAppointmentIntentPromptBuilder promptBuilder,
            MedicalProfessionalDirectory professionalDirectory
    ) {
        this.structuredOutputClient = structuredOutputClient;
        this.promptBuilder = promptBuilder;
        this.professionalDirectory = professionalDirectory;
    }

    @Override
    public ExtractedMedicalAppointmentIntent interpretPatientRequest(String patientQuestion) {
        String systemPrompt = promptBuilder.buildSystemPrompt(professionalDirectory.listAvailableProfessionals());
        String userPrompt = promptBuilder.buildUserPrompt(patientQuestion);

        return structuredOutputClient.generateStructuredOutput(
                systemPrompt,
                userPrompt,
                MedicalAppointmentStructuredOutputSchemas.medicalAppointmentIntentSchema(),
                ExtractedMedicalAppointmentIntent.class
        );
    }
}
