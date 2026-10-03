package br.com.vinicius.pocjavaiaconsultamedica.ai.schema;

import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;

import java.util.List;

/**
 * Schemas enviados no response_format para exigir Structured Output.
 *
 * O ponto principal da aula é justamente evitar depender de "JSON que parece JSON"
 * dentro de uma String. O schema descreve formalmente os campos aceitos.
 */
public final class MedicalAppointmentStructuredOutputSchemas {

    private MedicalAppointmentStructuredOutputSchemas() {
    }

    public static JsonSchema medicalAppointmentIntentSchema() {
        JsonObjectSchema root = JsonObjectSchema.builder()
                .addEnumProperty(
                        "intent",
                        List.of("schedule", "cancel", "unknown"),
                        "Identified patient intent"
                )
                .addIntegerProperty("professionalId", "ID of the matched medical professional")
                .addStringProperty("professionalName", "Medical professional name")
                .addStringProperty("datetime", "Appointment date and time in ISO-8601 UTC")
                .addStringProperty("patientName", "Patient name extracted from the request")
                .addStringProperty("reason", "Reason for the appointment, when supplied")
                .required("intent")
                .additionalProperties(false)
                .build();

        return JsonSchema.builder()
                .name("MedicalAppointmentIntent")
                .rootElement(root)
                .build();
    }

    public static JsonSchema patientMessageSchema() {
        JsonObjectSchema root = JsonObjectSchema.builder()
                .addStringProperty("message", "Clear and friendly message that will be shown to the patient")
                .required("message")
                .additionalProperties(false)
                .build();

        return JsonSchema.builder()
                .name("PatientMessage")
                .rootElement(root)
                .build();
    }
}
