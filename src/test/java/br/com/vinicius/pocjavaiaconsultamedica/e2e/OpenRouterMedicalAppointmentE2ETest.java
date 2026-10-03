package br.com.vinicius.pocjavaiaconsultamedica.e2e;

import br.com.vinicius.pocjavaiaconsultamedica.PocJavaIaConsultaMedicaApplication;
import br.com.vinicius.pocjavaiaconsultamedica.api.MedicalAppointmentChatRequest;
import br.com.vinicius.pocjavaiaconsultamedica.api.MedicalAppointmentChatResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste end-to-end REAL com OpenRouter.
 *
 * Caminho exercitado:
 * HTTP -> Controller -> LangGraph4j -> LangChain4j -> OpenRouter
 * -> Structured Output -> agendamento -> mensagem -> HTTP.
 *
 * LangSmith fica desligado neste teste de OpenRouter porque ele possui seu
 * próprio objetivo de validação. O tracing é validado por configuração separada.
 */
@Tag("openrouter")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "runOpenRouterE2E", matches = "true")
@SpringBootTest(
        classes = PocJavaIaConsultaMedicaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "langsmith.tracing=false"
)
class OpenRouterMedicalAppointmentE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldScheduleAppointmentFromNaturalLanguageUsingRealOpenRouterCall() {
        MedicalAppointmentChatRequest request = new MedicalAppointmentChatRequest(
                "Meu nome é Maria Santos. Quero marcar uma consulta com o "
                        + "Dr. Alicio da Silva amanhã às 16h para um check-up regular."
        );

        ResponseEntity<MedicalAppointmentChatResponse> response =
                restTemplate.postForEntity(
                        "/chat",
                        request,
                        MedicalAppointmentChatResponse.class
                );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().intent()).isEqualTo("schedule");
        assertThat(response.getBody().professionalId()).isEqualTo(1L);
        assertThat(response.getBody().patientName()).isEqualTo("Maria Santos");
        assertThat(response.getBody().actionSuccess()).isTrue();
        assertThat(response.getBody().message()).isNotBlank();
    }
}
