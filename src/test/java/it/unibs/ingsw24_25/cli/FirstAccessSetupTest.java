package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.VolunteerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FirstAccessSetupTest {

    @Mock
    private ConfiguratorService configuratorService;

    @Mock
    private VolunteerService volunteerService;

    private ByteArrayOutputStream output;
    private Printer printer;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        printer = new Printer(new PrintStream(output));
    }

    @Test
    void handleVolunteerFirstAccessStoresPersonalCredentialsAfterSuccessfulFlow() {
        StubPromptReader reader = new StubPromptReader(
                "TempPass123",
                "Guide.One",
                "NuovaPassword!",
                "NuovaPassword!"
        );
        FirstAccessSetup setup = new FirstAccessSetup(configuratorService, volunteerService, reader, printer);

        String result = setup.handleVolunteerFirstAccess("vol001");

        assertThat(result).isEqualTo("Guide.One");
        verify(volunteerService).verifyDefaultCredentials("vol001", "TempPass123");
        verify(volunteerService).setPersonalCredentials("vol001", "Guide.One", "NuovaPassword!");
    }

    @Test
    void handleVolunteerFirstAccessRepeatsUntilPasswordsMatch() {
        StubPromptReader reader = new StubPromptReader(
                "TempPass123",
                "Guide.One",
                "NuovaPassword!",
                "wrong",
                "Guide.One",
                "NuovaPassword!",
                "NuovaPassword!"
        );
        FirstAccessSetup setup = new FirstAccessSetup(configuratorService, volunteerService, reader, printer);

        String result = setup.handleVolunteerFirstAccess("vol001");

        assertThat(result).isEqualTo("Guide.One");
        verify(volunteerService).verifyDefaultCredentials("vol001", "TempPass123");
        verify(volunteerService, times(1)).setPersonalCredentials("vol001", "Guide.One", "NuovaPassword!");
        assertThat(output.toString()).contains("Le password non coincidono.");
    }

    private static final class StubPromptReader extends PromptReader {
        private final Queue<String> responses;

        private StubPromptReader(String... responses) {
            super(new Scanner(new ByteArrayInputStream(new byte[0])));
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public String readLine(String prompt) {
            String next = responses.poll();
            if (next == null) {
                throw new IllegalStateException("Nessuna risposta disponibile per il prompt: " + Objects.toString(prompt));
            }
            return next;
        }
    }
}