package server.service;

import org.junit.jupiter.api.Test;
import server.storage.SchemaInitializer;

import static org.junit.jupiter.api.Assertions.*;

public class InitializeDataServiceTest {

    @Test
    public void schemaInit_runsWithoutException() {
        assertDoesNotThrow(SchemaInitializer::init);
    }

    @Test
    public void initializeDefaultData_runsWithoutException() {
        SchemaInitializer.init();

        assertDoesNotThrow(InitializeDataService::initializeDefaultData);
        assertDoesNotThrow(InitializeDataService::initializeDefaultData);
    }
}

