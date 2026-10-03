package br.com.vinicius.pocjavaiaconsultamedica.service;

import br.com.vinicius.pocjavaiaconsultamedica.repository.InMemoryMedicalAppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicalAppointmentServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldRejectAlreadyBookedTime() {
        MedicalProfessionalDirectory directory = new MedicalProfessionalDirectory();
        InMemoryMedicalAppointmentRepository repository = new InMemoryMedicalAppointmentRepository(FIXED_CLOCK);
        MedicalAppointmentService service = new MedicalAppointmentService(repository, directory);

        assertThatThrownBy(() -> service.scheduleMedicalAppointment(
                1L,
                Instant.parse("2026-10-03T11:00:00Z"),
                "Outro Paciente",
                "check-up"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Horário indisponível");
    }

    @Test
    void shouldScheduleAndThenCancelAppointment() {
        MedicalProfessionalDirectory directory = new MedicalProfessionalDirectory();
        InMemoryMedicalAppointmentRepository repository = new InMemoryMedicalAppointmentRepository(FIXED_CLOCK);
        MedicalAppointmentService service = new MedicalAppointmentService(repository, directory);

        Instant dateTime = Instant.parse("2026-10-03T16:00:00Z");

        service.scheduleMedicalAppointment(
                2L,
                dateTime,
                "Joao da Silva",
                "retorno"
        );

        assertThat(repository.findExactAppointment(2L, dateTime, "Joao da Silva")).isPresent();

        service.cancelMedicalAppointment(2L, "Joao da Silva", dateTime);

        assertThat(repository.findExactAppointment(2L, dateTime, "Joao da Silva")).isEmpty();
    }
}
