package taxiPark.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseConnectionManagerTest {
    private static DatabaseConnectionManager dbManager;
    private static final String TEST_DATABASE = "test_database";

    @BeforeAll
    static void setUp() {
        dbManager = DatabaseConnectionManager.getInstance();
    }

    @AfterAll
    static void tearDown() {
        dbManager.closeConnection();
    }

    @Test
    void testSingletonInstance() {
        DatabaseConnectionManager anotherInstance = DatabaseConnectionManager.getInstance();
        assertSame(dbManager, anotherInstance, "Обидва виклики getInstance() мають повертати той самий об'єкт");
    }

    @Test
    void testConnectionIsValid() throws SQLException {
        Connection connection = dbManager.getConnection();

        assertNotNull(connection, "З'єднання не має бути null");
        assertFalse(connection.isClosed(), "З'єднання має бути відкрите");

        assertTrue(connection.isValid(2), "З'єднання має бути валідним");
    }


    @Test
    void testConnectionReopening() throws SQLException {
        Connection initialConnection = dbManager.getConnection();

        initialConnection.close();

        Connection newConnection = dbManager.getConnection();

        assertNotSame(initialConnection, newConnection, "Має бути створене нове з'єднання");
        assertFalse(newConnection.isClosed(), "Нове з'єднання має бути відкрите");
    }

    @Test
    void testConnectionErrorHandling() {
        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            SQLException testException = new SQLException("Test connection error", "08000", 1045);

            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenThrow(testException);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> new DatabaseConnectionManager(),
                    "Очікувалось, що конструктор викине RuntimeException при помилці підключення");

            assertTrue(thrown.getMessage().contains("Помилка з'єднання з базою даних"),
                    "Повідомлення про помилку має містити правильний текст");
            assertSame(testException, thrown.getCause(),
                    "Причина винятку має бути оригінальний SQLException");
        }
    }

    @Test
    void testDatabaseCreationErrorHandling() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);

        SQLException testException = new SQLException("Test creation error", "42501", 42501);
        when(mockStatement.executeUpdate(anyString())).thenThrow(testException);

        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            when(mockConnection.createStatement().executeQuery(anyString()))
                    .thenReturn(mock(ResultSet.class));
            when(mockConnection.createStatement().executeQuery(anyString()).next())
                    .thenReturn(false);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> new DatabaseConnectionManager(),
                    "Очікувалось, що конструктор викине RuntimeException при помилці створення БД");

            assertTrue(thrown.getCause() instanceof SQLException);
            SQLException sqlEx = (SQLException) thrown.getCause();

            assertEquals("Test creation error", sqlEx.getMessage());
            assertEquals("42501", sqlEx.getSQLState());
            assertEquals(42501, sqlEx.getErrorCode());

        }
    }

    @Test
    void testDatabaseCreationSuccess() throws SQLException {
        String originalDbName = DatabaseConnectionManager.getDatabaseNameForTest();

        DatabaseConnectionManager.setDatabaseNameForTest(TEST_DATABASE);

        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);


        when(mockStatement.executeUpdate(anyString())).thenReturn(1);

        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            when(mockConnection.createStatement().executeQuery(anyString()))
                    .thenReturn(mock(ResultSet.class));
            when(mockConnection.createStatement().executeQuery(anyString()).next())
                    .thenReturn(false);

            DatabaseConnectionManager manager = new DatabaseConnectionManager();
            assertDoesNotThrow(() -> manager.createDatabaseIfNotExists(),
                    "Метод не повинен викидати виняток при успішному створенні БД");
        }
    }

}