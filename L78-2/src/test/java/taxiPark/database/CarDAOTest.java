package taxiPark.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import taxiPark.model.Car;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarDAOTest {

    @Mock
    private DatabaseConnectionManager connectionManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private CarDAO carDAO;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(connectionManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
    }

    @Test
    void createCar_ShouldReturnGeneratedId_WhenSuccessful() throws SQLException {
        // Arrange
        Car car = new Car("Model X", 50000, 8.5, 220, "Sedan");
        int taxiParkId = 1;
        int expectedId = 10;

        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(expectedId);

        // Act
        int actualId = carDAO.createCar(car, taxiParkId);

        // Assert
        assertEquals(expectedId, actualId);
        assertEquals(expectedId, car.getId());
        verify(preparedStatement).setString(1, car.getModel());
        verify(preparedStatement).setDouble(2, car.getPrice());
        verify(preparedStatement).setDouble(3, car.getFuelConsumption());
        verify(preparedStatement).setInt(4, car.getMaxSpeed());
        verify(preparedStatement).setString(5, car.getType());
        verify(preparedStatement).setInt(6, taxiParkId);
    }

    @Test
    void createCar_ShouldReturnMinusOne_WhenExceptionOccurs() throws SQLException {
        // Arrange
        Car car = new Car("Model X", 50000, 8.5, 220, "Sedan");
        int taxiParkId = 1;

        when(connection.prepareStatement(anyString(), anyInt())).thenThrow(new SQLException());

        // Act
        int result = carDAO.createCar(car, taxiParkId);

        // Assert
        assertEquals(-1, result);
    }

    @Test
    void getCarsByTaxiParkId_ShouldReturnListOfCars() throws SQLException {
        // Arrange
        int taxiParkId = 1;
        List<Car> expectedCars = new ArrayList<>();
        expectedCars.add(new Car("Model X", 50000, 8.5, 220, "Sedan"));
        expectedCars.add(new Car("Model Y", 60000, 7.5, 240, "SUV"));

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("id")).thenReturn(1, 2);
        when(resultSet.getString("model")).thenReturn("Model X", "Model Y");
        when(resultSet.getDouble("price")).thenReturn(50000.0, 60000.0);
        when(resultSet.getDouble("fuel_consumption")).thenReturn(8.5, 7.5);
        when(resultSet.getInt("max_speed")).thenReturn(220, 240);
        when(resultSet.getString("type")).thenReturn("Sedan", "SUV");

        // Act
        List<Car> actualCars = carDAO.getCarsByTaxiParkId(taxiParkId);

        // Assert
        assertEquals(expectedCars.size(), actualCars.size());
        for (int i = 0; i < expectedCars.size(); i++) {
            assertEquals(expectedCars.get(i).getModel(), actualCars.get(i).getModel());
            assertEquals(expectedCars.get(i).getPrice(), actualCars.get(i).getPrice());
        }
        verify(preparedStatement).setInt(1, taxiParkId);
    }

    @Test
    void getCarById_ShouldReturnCar_WhenExists() throws SQLException {
        // Arrange
        int carId = 1;
        Car expectedCar = new Car("Model X", 50000, 8.5, 220, "Sedan");
        expectedCar.setId(carId);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(carId);
        when(resultSet.getString("model")).thenReturn(expectedCar.getModel());
        when(resultSet.getDouble("price")).thenReturn(expectedCar.getPrice());
        when(resultSet.getDouble("fuel_consumption")).thenReturn(expectedCar.getFuelConsumption());
        when(resultSet.getInt("max_speed")).thenReturn(expectedCar.getMaxSpeed());
        when(resultSet.getString("type")).thenReturn(expectedCar.getType());

        // Act
        Car actualCar = carDAO.getCarById(carId);

        // Assert
        assertNotNull(actualCar);
        assertEquals(expectedCar.getId(), actualCar.getId());
        assertEquals(expectedCar.getModel(), actualCar.getModel());
        verify(preparedStatement).setInt(1, carId);
    }

    @Test
    void getCarById_ShouldReturnNull_WhenNotExists() throws SQLException {
        // Arrange
        int carId = 999;

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        Car actualCar = carDAO.getCarById(carId);

        // Assert
        assertNull(actualCar);
    }

    @Test
    void updateCar_ShouldReturnTrue_WhenSuccessful() throws SQLException {
        // Arrange
        Car car = new Car("Model X", 50000, 8.5, 220, "Sedan");
        car.setId(1);

        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = carDAO.updateCar(car);

        // Assert
        assertTrue(result);
        verify(preparedStatement).setString(1, car.getModel());
        verify(preparedStatement).setDouble(2, car.getPrice());
        verify(preparedStatement).setDouble(3, car.getFuelConsumption());
        verify(preparedStatement).setInt(4, car.getMaxSpeed());
        verify(preparedStatement).setString(5, car.getType());
        verify(preparedStatement).setInt(6, car.getId());
    }

    @Test
    void updateCar_ShouldReturnFalse_WhenNoRowsAffected() throws SQLException {
        // Arrange
        Car car = new Car("Model X", 50000, 8.5, 220, "Sedan");
        car.setId(1);

        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = carDAO.updateCar(car);

        // Assert
        assertFalse(result);
    }

    @Test
    void deleteCar_ShouldReturnTrue_WhenSuccessful() throws SQLException {
        // Arrange
        int carId = 1;

        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = carDAO.deleteCar(carId);

        // Assert
        assertTrue(result);
        verify(preparedStatement).setInt(1, carId);
    }

    @Test
    void deleteCar_ShouldReturnFalse_WhenNoRowsAffected() throws SQLException {
        // Arrange
        int carId = 999;

        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Act
        boolean result = carDAO.deleteCar(carId);

        // Assert
        assertFalse(result);
    }

    @Test
    void getCarsBySpeedRange_ShouldReturnFilteredCars() throws SQLException {
        // Arrange
        int minSpeed = 200;
        int maxSpeed = 250;
        int taxiParkId = 1;
        List<Car> expectedCars = new ArrayList<>();
        expectedCars.add(new Car("Model X", 50000, 8.5, 220, "Sedan"));
        expectedCars.add(new Car("Model Y", 60000, 7.5, 240, "SUV"));

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("id")).thenReturn(1, 2);
        when(resultSet.getString("model")).thenReturn("Model X", "Model Y");
        when(resultSet.getDouble("price")).thenReturn(50000.0, 60000.0);
        when(resultSet.getDouble("fuel_consumption")).thenReturn(8.5, 7.5);
        when(resultSet.getInt("max_speed")).thenReturn(220, 240);
        when(resultSet.getString("type")).thenReturn("Sedan", "SUV");

        // Act
        List<Car> actualCars = carDAO.getCarsBySpeedRange(minSpeed, maxSpeed, taxiParkId);

        // Assert
        assertEquals(expectedCars.size(), actualCars.size());
        for (int i = 0; i < expectedCars.size(); i++) {
            assertEquals(expectedCars.get(i).getMaxSpeed(), actualCars.get(i).getMaxSpeed());
        }
        verify(preparedStatement).setInt(1, taxiParkId);
        verify(preparedStatement).setInt(2, minSpeed);
        verify(preparedStatement).setInt(3, maxSpeed);
    }

    @Test
    void getCarsSortedByFuelConsumption_ShouldReturnSortedCars() throws SQLException {
        // Arrange
        int taxiParkId = 1;
        List<Car> expectedCars = new ArrayList<>();
        expectedCars.add(new Car("Model Y", 60000, 7.5, 240, "SUV")); // Lower consumption first
        expectedCars.add(new Car("Model X", 50000, 8.5, 220, "Sedan"));

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("id")).thenReturn(2, 1);
        when(resultSet.getString("model")).thenReturn("Model Y", "Model X");
        when(resultSet.getDouble("price")).thenReturn(60000.0, 50000.0);
        when(resultSet.getDouble("fuel_consumption")).thenReturn(7.5, 8.5);
        when(resultSet.getInt("max_speed")).thenReturn(240, 220);
        when(resultSet.getString("type")).thenReturn("SUV", "Sedan");

        // Act
        List<Car> actualCars = carDAO.getCarsSortedByFuelConsumption(taxiParkId);

        // Assert
        assertEquals(expectedCars.size(), actualCars.size());
        assertTrue(actualCars.get(0).getFuelConsumption() <= actualCars.get(1).getFuelConsumption());
        verify(preparedStatement).setInt(1, taxiParkId);
    }

    @Test
    void getTotalCarValueByTaxiParkId_ShouldReturnCorrectSum() throws SQLException {
        // Arrange
        int taxiParkId = 1;
        double expectedTotal = 110000.0;

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("total_value")).thenReturn(expectedTotal);

        // Act
        double actualTotal = carDAO.getTotalCarValueByTaxiParkId(taxiParkId);

        // Assert
        assertEquals(expectedTotal, actualTotal);
        verify(preparedStatement).setInt(1, taxiParkId);
    }

    @Test
    void getTotalCarValueByTaxiParkId_ShouldReturnZero_WhenNoCars() throws SQLException {
        // Arrange
        int taxiParkId = 1;

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        double actualTotal = carDAO.getTotalCarValueByTaxiParkId(taxiParkId);

        // Assert
        assertEquals(0.0, actualTotal);
    }
}