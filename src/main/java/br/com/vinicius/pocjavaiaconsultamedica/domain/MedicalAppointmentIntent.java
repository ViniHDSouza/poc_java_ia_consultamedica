package br.com.vinicius.pocjavaiaconsultamedica.domain;

import java.util.Locale;

/**
 * Intenções suportadas por esta POC.
 *
 * O projeto original possui exatamente estas três possibilidades:
 * agendar, cancelar ou não identificar uma intenção conhecida.
 */
public enum MedicalAppointmentIntent {
    SCHEDULE("schedule"),
    CANCEL("cancel"),
    UNKNOWN("unknown");

    private final String externalValue;

    MedicalAppointmentIntent(String externalValue) {
        this.externalValue = externalValue;
    }

    public String externalValue() {
        return externalValue;
    }

    public static MedicalAppointmentIntent fromExternalValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);

        for (MedicalAppointmentIntent intent : values()) {
            if (intent.externalValue.equals(normalizedValue)) {
                return intent;
            }
        }

        return UNKNOWN;
    }
}
