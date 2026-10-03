package br.com.vinicius.pocjavaiaconsultamedica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicação.
 *
 * A anotação ConfigurationPropertiesScan faz o Spring localizar automaticamente
 * classes que representam propriedades externas, como as configurações do OpenRouter.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PocJavaIaConsultaMedicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocJavaIaConsultaMedicaApplication.class, args);
    }
}
