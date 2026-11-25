package com.serv.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Application code must log through SLF4J: console prints bypass levels, format and request ids. */
class NoConsoleOutputTest {

    private static final Pattern FORBIDDEN = Pattern.compile("System\\.(out|err)\\.|\\.printStackTrace\\(");

    @Test
    @DisplayName("No System.out / System.err / printStackTrace in production code")
    void noConsoleOutput() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            List<String> offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try { return FORBIDDEN.matcher(Files.readString(p)).find(); }
                        catch (IOException e) { throw new RuntimeException(e); }
                    })
                    .map(Path::toString)
                    .toList();
            assertThat(offenders).isEmpty();
        }
    }
}
