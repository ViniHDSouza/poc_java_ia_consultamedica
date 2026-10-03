package br.com.vinicius.pocjavaiaconsultamedica.service;

import br.com.vinicius.pocjavaiaconsultamedica.domain.MedicalProfessional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo simples dos profissionais disponíveis.
 */
@Service
public class MedicalProfessionalDirectory {

    private final List<MedicalProfessional> professionals = List.of(
            new MedicalProfessional(1L, "Dr. Alicio da Silva", "Cardiologia"),
            new MedicalProfessional(2L, "Dra. Ana Pereira", "Dermatologia"),
            new MedicalProfessional(3L, "Dra. Carol Gomes", "Neurologia")
    );

    public List<MedicalProfessional> listAvailableProfessionals() {
        return professionals;
    }

    public Optional<MedicalProfessional> findProfessionalById(long professionalId) {
        return professionals.stream()
                .filter(professional -> professional.id() == professionalId)
                .findFirst();
    }
}
