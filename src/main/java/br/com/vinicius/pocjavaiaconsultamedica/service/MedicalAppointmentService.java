package br.com.vinicius.pocjavaiaconsultamedica.service;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalAppointment;
import br.com.vinicius.pocjavaiaconsultamedica.repository.InMemoryMedicalAppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Regras de negócio relacionadas ao agendamento.
 *
 * Observe que a IA NÃO agenda diretamente. Ela apenas interpreta o texto.
 * A ação real continua sendo executada por código Java comum.
 */
@Service
public class MedicalAppointmentService {

    private final InMemoryMedicalAppointmentRepository appointmentRepository;
    private final MedicalProfessionalDirectory professionalDirectory;

    public MedicalAppointmentService(
            InMemoryMedicalAppointmentRepository appointmentRepository,
            MedicalProfessionalDirectory professionalDirectory
    ) {
        this.appointmentRepository = appointmentRepository;
        this.professionalDirectory = professionalDirectory;
    }

    public MedicalAppointment scheduleMedicalAppointment(
            long professionalId,
            Instant dateTime,
            String patientName,
            String reason
    ) {
        ensureProfessionalExists(professionalId);

        if (appointmentRepository.existsAt(professionalId, dateTime)) {
            throw new IllegalStateException("Horário indisponível para este profissional");
        }

        MedicalAppointment newAppointment = new MedicalAppointment(
                dateTime,
                patientName,
                reason == null || reason.isBlank() ? "general consultation" : reason,
                professionalId
        );

        return appointmentRepository.save(newAppointment);
    }

    public void cancelMedicalAppointment(
            long professionalId,
            String patientName,
            Instant dateTime
    ) {
        ensureProfessionalExists(professionalId);

        MedicalAppointment appointment = appointmentRepository
                .findExactAppointment(professionalId, dateTime, patientName)
                .orElseThrow(() -> new IllegalStateException("Agendamento não encontrado para cancelamento"));

        appointmentRepository.delete(appointment);
    }

    private void ensureProfessionalExists(long professionalId) {
        professionalDirectory.findProfessionalById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profissional não encontrado: " + professionalId
                ));
    }
}
