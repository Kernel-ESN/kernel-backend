package com.kernelconsulting.plateforme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API de la plateforme d'annonces. Contrat : {@code docs/api/openapi.yaml}. */
@SpringBootApplication
public class PlateformeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlateformeApplication.class, args);
    }
}
