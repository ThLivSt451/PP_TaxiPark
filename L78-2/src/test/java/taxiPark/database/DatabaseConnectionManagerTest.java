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
        // Отримуємо єдиний екземпляр DatabaseConnectionManager
        dbManager = DatabaseConnectionManager.getInstance();
    }

    @AfterAll
    static void tearDown() {
        // Закриваємо з'єднання після всіх тестів
        dbManager.closeConnection();
    }

    @Test
    void testSingletonInstance() {
        // Перевіряємо, що getInstance() повертає той самий екземпляр
        DatabaseConnectionManager anotherInstance = DatabaseConnectionManager.getInstance();
        assertSame(dbManager, anotherInstance, "Обидва виклики getInstance() мають повертати той самий об'єкт");
    }

    @Test
    void testConnectionIsValid() throws SQLException {
        // Отримуємо з'єднання
        Connection connection = dbManager.getConnection();

        // Перевіряємо, що з'єднання не null і воно відкрите
        assertNotNull(connection, "З'єднання не має бути null");
        assertFalse(connection.isClosed(), "З'єднання має бути відкрите");

        // Додатково перевіряємо, що з'єднання дійсно працює
        assertTrue(connection.isValid(2), "З'єднання має бути валідним");
    }

    /*@Test
    void testTablesExist() throws SQLException {
        // Отримуємо з'єднання
        Connection connection = dbManager.getConnection();

        // Перевіряємо наявність таблиці taxi_parks
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                             "WHERE table_schema = 'taxiparks' AND table_name = 'taxi_parks')")) {
            assertTrue(rs.next(), "Результат має містити хоча б один рядок");
            assertTrue(rs.getBoolean(1), "Таблиця taxi_parks має існувати");
        }

        // Перевіряємо наявність таблиці cars
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables " +
                             "WHERE table_schema = 'taxipark' AND table_name = 'taxi_parks'")) {
            assertTrue(rs.next(), "Результат має містити хоча б один рядок");
            assertTrue(rs.getBoolean(1), "Таблиця cars має існувати");
        }
    }*/

    @Test
    void testConnectionReopening() throws SQLException {
        // Отримуємо початкове з'єднання
        Connection initialConnection = dbManager.getConnection();

        // Закриваємо з'єднання
        initialConnection.close();

        // Отримуємо нове з'єднання
        Connection newConnection = dbManager.getConnection();

        // Перевіряємо, що нове з'єднання дійсно нове (не те саме)
        assertNotSame(initialConnection, newConnection, "Має бути створене нове з'єднання");
        assertFalse(newConnection.isClosed(), "Нове з'єднання має бути відкрите");
    }

    @Test
    void testConnectionErrorHandling() {
        // Мокуємо DriverManager, щоб імітувати помилку підключення
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
        // Створюємо мок для Connection та Statement
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);

        // Імітуємо ситуацію, коли база даних не існує
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Імітуємо помилку при створенні бази даних
        SQLException testException = new SQLException("Test creation error", "42501", 42501);
        when(mockStatement.executeUpdate(anyString())).thenThrow(testException);

        // Мокуємо DriverManager для повернення нашого мок-з'єднання
        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Імітуємо, що база даних не існує
            when(mockConnection.createStatement().executeQuery(anyString()))
                    .thenReturn(mock(ResultSet.class));
            when(mockConnection.createStatement().executeQuery(anyString()).next())
                    .thenReturn(false);

            // Очікуємо RuntimeException, а не SQLException
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> new DatabaseConnectionManager(),
                    "Очікувалось, що конструктор викине RuntimeException при помилці створення БД");

            // Перевіряємо, що причина - наш тестовий SQLException
            assertTrue(thrown.getCause() instanceof SQLException);
            SQLException sqlEx = (SQLException) thrown.getCause();

            assertEquals("Test creation error", sqlEx.getMessage());
            assertEquals("42501", sqlEx.getSQLState());
            assertEquals(42501, sqlEx.getErrorCode());

        }
    }

    @Test
    void testDatabaseCreationSuccess() throws SQLException {
        // Зберігаємо оригінальну назву бази даних
        String originalDbName = DatabaseConnectionManager.getDatabaseNameForTest();

        // Встановлюємо тестову назву бази даних
        DatabaseConnectionManager.setDatabaseNameForTest(TEST_DATABASE);

        // Створюємо мок для Connection та Statement
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Імітуємо успішне створення бази даних
        when(mockStatement.executeUpdate(anyString())).thenReturn(1);

        // Мокуємо DriverManager
        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Імітуємо, що база даних не існує
            when(mockConnection.createStatement().executeQuery(anyString()))
                    .thenReturn(mock(ResultSet.class));
            when(mockConnection.createStatement().executeQuery(anyString()).next())
                    .thenReturn(false);

            DatabaseConnectionManager manager = new DatabaseConnectionManager();
            assertDoesNotThrow(() -> manager.createDatabaseIfNotExists(),
                    "Метод не повинен викидати виняток при успішному створенні БД");
        }
    }

    /*@Test
    void testDatabaseAlreadyExists() throws SQLException {
        // Створюємо мок для Connection та Statement
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);

        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Імітуємо, що база даних вже існує
        when(mockConnection.createStatement().executeQuery(anyString()))
                .thenReturn(mock(ResultSet.class));
        when(mockConnection.createStatement().executeQuery(anyString()).next())
                .thenReturn(true);

        // Мокуємо DriverManager
        try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseConnectionManager manager = new DatabaseConnectionManager();
            assertDoesNotThrow(() -> manager.createDatabaseIfNotExists(),
                    "Метод не повинен викидати виняток, коли БД вже існує");

            // Перевіряємо, що CREATE DATABASE не викликався
            verify(mockStatement, never()).executeUpdate(contains("CREATE DATABASE"));
        }
    }*/
}