package br.com.vinicius.pocjavaiaconsultamedica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expõe um Clock como dependência.
 *
 * Parece um detalhe pequeno, mas torna código que depende de "hoje" e "amanhã"
 * testável sem precisar alterar o relógio real do computador.
 */
@Configuration
public class TimeConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
