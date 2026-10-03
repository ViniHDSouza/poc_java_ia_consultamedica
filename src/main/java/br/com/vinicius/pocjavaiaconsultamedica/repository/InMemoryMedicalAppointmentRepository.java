package br.com.vinicius.pocjavaiaconsultamedica.repository;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointment;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório em memória para manter a POC simples e fiel ao exemplo JavaScript.
 *
 * IMPORTANTE: ao reiniciar a aplicação, as alterações são perdidas.
 */
@Repository
public class InMemoryMedicalAppointmentRepository {

    private final List<MedicalAppointment> appointments = new ArrayList<>();

    public InMemoryMedicalAppointmentRepository(Clock clock) {
        seedExampleAppointments(clock);
    }

    public synchronized Optional<MedicalAppointment> findExactAppointment(
            long professionalId,
            Instant dateTime,
            String patientName
    ) {
        return appointments.stream()
                .filter(appointment -> appointment.professionalId() == professionalId)
                .filter(appointment -> appointment.dateTime().equals(dateTime))
                .filter(appointment -> patientName == null || appointment.patientName().equalsIgnoreCase(patientName))
                .findFirst();
    }

    public synchronized boolean existsAt(long professionalId, Instant dateTime) {
        return appointments.stream()
                .anyMatch(appointment ->
                        appointment.professionalId() == professionalId
                                && appointment.dateTime().equals(dateTime)
                );
    }

    public synchronized MedicalAppointment save(MedicalAppointment appointment) {
        appointments.add(appointment);
        return appointment;
    }

    public synchronized void delete(MedicalAppointment appointment) {
        appointments.remove(appointment);
    }

    public synchronized List<MedicalAppointment> findAll() {
        return List.copyOf(appointments);
    }

    /**
     * Replica a ideia do projeto original:
     * - hoje às 11:00 UTC existe consulta do Dr. Alicio;
     * - amanhã às 14:00 UTC existe consulta da Dra. Ana.
     */
    private void seedExampleAppointments(Clock clock) {
        LocalDate today = LocalDate.now(clock);

        appointments.add(new MedicalAppointment(
                today.atTime(11, 0).toInstant(ZoneOffset.UTC),
                "Joao da Silva",
                "check-up regular",
                1L
        ));

        appointments.add(new MedicalAppointment(
                today.plusDays(1).atTime(14, 0).toInstant(ZoneOffset.UTC),
                "Luana Costa",
                "Erupção cutânea",
                2L
        ));
    }
}
