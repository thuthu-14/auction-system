package server.service;

import org.junit.jupiter.api.Test;
import server.storage.SchemaInitializer;

import static org.junit.jupiter.api.Assertions.*;

public class InitializeDataServiceTest {

    @Test
    public void init_runsWithoutException() {
        // simply call schema init (idempotent)
        assertDoesNotThrow(SchemaInitializer::init);
    }
}

