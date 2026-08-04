package com.fedeiatech.sistemagestionpyme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the reintroduction of the removed licensing/premium-gating
 * and business-profile concepts (de-fork). This app is single-tenant
 * Ferrematica only — no LicenseService, no permiteX() gates, no
 * PerfilNegocio picker.
 */
class CodebaseConventionsTest {

    private static final Pattern FORBIDDEN = Pattern.compile("license|premium|PerfilNegocio", Pattern.CASE_INSENSITIVE);

    @Test
    void srcMainContainsNoLicensingOrBusinessProfileReferences() throws IOException {
        Path srcMain = Paths.get("src", "main");
        assertTrue(Files.exists(srcMain), "src/main debe existir");

        List<String> ofensores;
        try (Stream<Path> paths = Files.walk(srcMain)) {
            ofensores = paths
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.toString();
                    return name.endsWith(".java") || name.endsWith(".fxml");
                })
                .filter(this::contieneReferenciaProhibida)
                .map(Path::toString)
                .collect(Collectors.toList());
        }

        assertTrue(ofensores.isEmpty(),
            "Se encontraron referencias a license/premium/PerfilNegocio en: " + ofensores);
    }

    private boolean contieneReferenciaProhibida(Path archivo) {
        try {
            String contenido = Files.readString(archivo);
            return FORBIDDEN.matcher(contenido).find();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer " + archivo, e);
        }
    }
}
