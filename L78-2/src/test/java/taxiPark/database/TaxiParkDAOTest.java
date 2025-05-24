package taxiPark.database;

import org.junit.jupiter.api.*;
import taxiPark.model.Car;
import taxiPark.service.TaxiPark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TaxiParkDAOTest {
    private static final String TEST_DB_NAME = "test_taxi_park_db";
    private static DatabaseConnectionManager testConnectionManager;
    private TaxiParkDAO taxiParkDAO;
    private CarDAO carDAO;

    @BeforeAll
    static void setUpBeforeAll() {
        // Змінюємо назву бази даних для тестів
        DatabaseConnectionManager.setDatabaseNameForTest(TEST_DB_NAME);

        // Створюємо тестову базу даних
        testConnectionManager = DatabaseConnectionManager.getInstance();
    }

    @AfterAll
    static void tearDownAfterAll() {
        // Очищаємо тестову базу даних після всіх тестів
        try (Connection connection = testConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS cars");
            statement.executeUpdate("DROP TABLE IF EXISTS taxi_parks");
            testConnectionManager.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    void setUp() {
        // Ініціалізуємо DAO для кожного тесту
        taxiParkDAO = new TaxiParkDAO(testConnectionManager);
        carDAO = new CarDAO(testConnectionManager);

        // Очищаємо таблиці перед кожним тестом
        try (Connection connection = testConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM cars");
            statement.executeUpdate("DELETE FROM taxi_parks");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void createTaxiPark_shouldCreateNewTaxiPark() {
        // Arrange
        TaxiPark taxiPark = new TaxiPark("Test Park", new ArrayList<>());

        // Act
        int createdId = taxiParkDAO.createTaxiPark(taxiPark);

        // Assert
        assertTrue(createdId > 0);
        assertEquals(createdId, taxiPark.getId());
    }

    @Test
    void createTaxiPark_withCars_shouldCreateParkWithCars() {
        // Arrange
        List<Car> cars = new ArrayList<>();
        cars.add(new Car("Model1", 10000, 8.5, 180, "Sedan"));
        cars.add(new Car("Model2", 15000, 10.2, 200, "SUV"));

        TaxiPark taxiPark = new TaxiPark("Park with Cars", cars);

        // Act
        int createdId = taxiParkDAO.createTaxiPark(taxiPark);

        // Assert
        assertTrue(createdId > 0);

        TaxiPark retrievedPark = taxiParkDAO.getTaxiParkById(createdId);
        assertNotNull(retrievedPark);
        assertEquals(2, retrievedPark.getCars().size());
    }

    @Test
    void getAllTaxiParks_shouldReturnAllParks() {
        // Arrange
        taxiParkDAO.createTaxiPark(new TaxiPark("Park 1", new ArrayList<>()));
        taxiParkDAO.createTaxiPark(new TaxiPark("Park 2", new ArrayList<>()));

        // Act
        List<TaxiPark> parks = taxiParkDAO.getAllTaxiParks();

        // Assert
        assertEquals(2, parks.size());
    }

    @Test
    void getTaxiParkById_shouldReturnCorrectPark() {
        // Arrange
        TaxiPark expectedPark = new TaxiPark("Test Park", new ArrayList<>());
        int id = taxiParkDAO.createTaxiPark(expectedPark);

        // Act
        TaxiPark actualPark = taxiParkDAO.getTaxiParkById(id);

        // Assert
        assertNotNull(actualPark);
        assertEquals(expectedPark.getName(), actualPark.getName());
        assertEquals(id, actualPark.getId());
    }

    @Test
    void getTaxiParkById_withInvalidId_shouldReturnNull() {
        // Act
        TaxiPark park = taxiParkDAO.getTaxiParkById(-1);

        // Assert
        assertNull(park);
    }

    @Test
    void updateTaxiPark_shouldUpdateParkName() {
        // Arrange
        TaxiPark park = new TaxiPark("Old Name", new ArrayList<>());
        int id = taxiParkDAO.createTaxiPark(park);

        // Act
        park.setName("New Name");
        boolean result = taxiParkDAO.updateTaxiPark(park);

        // Assert
        assertTrue(result);
    }

    @Test
    void deleteTaxiPark_shouldRemoveParkFromDatabase() {
        // Arrange
        TaxiPark park = new TaxiPark("To Delete", new ArrayList<>());
        int id = taxiParkDAO.createTaxiPark(park);

        // Act
        boolean result = taxiParkDAO.deleteTaxiPark(id);

        // Assert
        assertTrue(result);
    }

    @Test
    void deleteTaxiPark_shouldAlsoDeleteAssociatedCars() {
        // Arrange
        List<Car> cars = new ArrayList<>();
        cars.add(new Car("Model1", 10000, 8.5, 180, "Sedan"));

        TaxiPark park = new TaxiPark("Park with Cars", cars);
        int id = taxiParkDAO.createTaxiPark(park);

        assertTrue(taxiParkDAO.deleteTaxiPark(id));
    }

    @Test
    void getAllTaxiParks_whenDatabaseError_shouldReturnEmptyList() throws SQLException {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException("Database error"));

        TaxiParkDAO daoWithMock = new TaxiParkDAO(new DatabaseConnectionManager() {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        });

        // Act
        List<TaxiPark> result = daoWithMock.getAllTaxiParks();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTaxiParkById_whenDatabaseError_shouldReturnNull() throws SQLException {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        TaxiParkDAO daoWithMock = new TaxiParkDAO(new DatabaseConnectionManager() {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        });

        // Act
        TaxiPark result = daoWithMock.getTaxiParkById(1);

        // Assert
        assertNull(result);
    }

    @Test
    void updateTaxiPark_whenDatabaseError_shouldReturnFalse() throws SQLException {
        // Arrange
        TaxiPark park = new TaxiPark("Test Park", new ArrayList<>());
        int id = taxiParkDAO.createTaxiPark(park);
        park.setId(id);

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        TaxiParkDAO daoWithMock = new TaxiParkDAO(new DatabaseConnectionManager() {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        });

        // Act
        boolean result = daoWithMock.updateTaxiPark(park);

        // Assert
        assertFalse(result);
    }

    @Test
    void deleteTaxiPark_whenDatabaseError_shouldReturnFalse() throws SQLException {
        // Arrange
        TaxiPark park = new TaxiPark("Test Park", new ArrayList<>());
        int id = taxiParkDAO.createTaxiPark(park);

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        TaxiParkDAO daoWithMock = new TaxiParkDAO(new DatabaseConnectionManager() {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        });

        // Act
        boolean result = daoWithMock.deleteTaxiPark(id);

        // Assert
        assertFalse(result);
    }

    @Test
    void createTaxiPark_whenTransactionFails_shouldRollback() throws SQLException {
        // Arrange
        List<Car> cars = new ArrayList<>();
        cars.add(new Car("Model1", 10000, 8.5, 180, "Sedan"));
        cars.add(new Car("Model2", 15000, 10.2, 200, "SUV"));

        TaxiPark taxiPark = new TaxiPark("Park with Cars", cars);

        Connection mockConnection = mock(Connection.class);
        when(mockConnection.getAutoCommit()).thenReturn(true);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenThrow(new SQLException("Database error"));

        TaxiParkDAO daoWithMock = new TaxiParkDAO(new DatabaseConnectionManager() {
            @Override
            public Connection getConnection() {
                return mockConnection;
            }
        });

        // Act
        int result = daoWithMock.createTaxiPark(taxiPark);

        // Assert
        assertEquals(-1, result);
        verify(mockConnection, times(1)).rollback();
    }
}