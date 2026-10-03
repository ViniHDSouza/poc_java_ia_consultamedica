package br.com.vinicius.pocjavaiaconsultamedica.ai.prompt;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalProfessional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Monta os prompts em JSON da mesma forma conceitual usada nas transcrições.
 *
 * Aqui JSON não é a resposta da IA. Ele é o FORMATO DO PROMPT enviado para a IA.
 * Isso deixa regras, contexto, exemplos e instruções mais fáceis de localizar.
 */
@Component
public class MedicalAppointmentIntentPromptBuilder {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MedicalAppointmentIntentPromptBuilder(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String buildSystemPrompt(List<MedicalProfessional> professionals) {
        List<Map<String, Object>> professionalData = professionals.stream()
                .map(professional -> Map.<String, Object>of(
                        "id", professional.id(),
                        "name", professional.name(),
                        "specialty", professional.specialty()
                ))
                .toList();

        LocalDate today = LocalDate.now(clock);

        Map<String, Object> prompt = Map.of(
                "role", "Intent Classifier for Medical Appointments",
                "task", "Identify user intent and extract all appointment-related details",
                "professionals", professionalData,
                "current_date", Instant.now(clock).toString(),
                "rules", Map.of(
                        "schedule", Map.of(
                                "description", "User wants to book or schedule a new appointment",
                                "required_fields", List.of("professionalId", "datetime", "patientName"),
                                "optional_fields", List.of("reason")
                        ),
                        "cancel", Map.of(
                                "description", "User wants to cancel an existing appointment",
                                "required_fields", List.of("professionalId", "datetime", "patientName")
                        ),
                        "unknown", Map.of(
                                "description", "Anything not related to scheduling or cancelling appointments"
                        )
                ),
                "extraction_instructions", Map.of(
                        "professionalId", "Match the professional name to the ID from the professionals list. Allow small name variations.",
                        "professionalName", "Return the canonical professional name from the supplied professionals list when a match is found",
                        "datetime", "Parse relative dates such as today/tomorrow and convert to ISO-8601 UTC",
                        "patientName", "Extract the patient name only when it is present in the question",
                        "reason", "Extract the appointment reason only when available"
                ),
                "examples", List.of(
                        Map.of(
                                "input", "Meu nome é Maria Santos e quero uma consulta com o Dr. Alicio da Silva amanhã às 16h para um check-up",
                                "output", Map.of(
                                        "intent", "schedule",
                                        "professionalId", 1,
                                        "professionalName", "Dr. Alicio da Silva",
                                        "datetime", today.plusDays(1).atTime(16, 0).toInstant(ZoneOffset.UTC).toString(),
                                        "patientName", "Maria Santos",
                                        "reason", "check-up"
                                )
                        ),
                        Map.of(
                                "input", "Cancele minha consulta com a Dra. Ana Pereira hoje às 11h. Meu nome é João da Silva",
                                "output", Map.of(
                                        "intent", "cancel",
                                        "professionalId", 2,
                                        "professionalName", "Dra. Ana Pereira",
                                        "datetime", today.atTime(11, 0).toInstant(ZoneOffset.UTC).toString(),
                                        "patientName", "João da Silva"
                                )
                        ),
                        Map.of(
                                "input", "Qual é a previsão do tempo para amanhã?",
                                "output", Map.of("intent", "unknown")
                        )
                )
        );

        return writeJson(prompt);
    }

    public String buildUserPrompt(String patientQuestion) {
        Map<String, Object> prompt = Map.of(
                "question", patientQuestion,
                "instructions", List.of(
                        "Carefully determine the user intent",
                        "Extract only information supported by the text",
                        "Convert date and time to ISO-8601 UTC",
                        "Match professional names to the professionals supplied by the system prompt",
                        "Return only optional fields that are actually supported by the question",
                        "If the request is unrelated, use intent=unknown"
                )
        );

        return writeJson(prompt);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível montar o prompt JSON.", exception);
        }
    }
}
