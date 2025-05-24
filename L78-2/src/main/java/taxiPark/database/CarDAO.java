package taxiPark.database;

import taxiPark.model.Car;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarDAO {
    private static final Logger logger = LoggerFactory.getLogger(CarDAO.class);
    private final DatabaseConnectionManager connectionManager;

    public CarDAO() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    public CarDAO(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // Додавання нового автомобіля
    public int createCar(Car car, int taxiParkId, Connection existingConnection) throws SQLException {
        String sql = "INSERT INTO cars (model, price, fuel_consumption, max_speed, type, taxi_park_id) VALUES (?, ?, ?, ?, ?, ?)";
        int generatedId = -1;
        boolean useExistingConnection = (existingConnection != null);
        Connection connection = null;

        try {
            // Використовуємо існуюче з'єднання або отримуємо нове
            connection = useExistingConnection ? existingConnection : connectionManager.getConnection();

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, car.getModel());
                preparedStatement.setDouble(2, car.getPrice());
                preparedStatement.setDouble(3, car.getFuelConsumption());
                preparedStatement.setInt(4, car.getMaxSpeed());
                preparedStatement.setString(5, car.getType());
                preparedStatement.setInt(6, taxiParkId);

                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            generatedId = generatedKeys.getInt(1);
                            car.setId(generatedId);
                            logger.info("Додано новий автомобіль: id={}, модель={}, ціна={}",
                                    generatedId, car.getModel(), car.getPrice());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка при створенні автомобіля", e);
            throw e;  // Перекидаємо виняток для коректної обробки транзакції
        }

        return generatedId;
    }

    public int createCar(Car car, int taxiParkId) {
        try {
            // Call the three-parameter version with null as the connection
            // This will cause the method to create a new connection
            return createCar(car, taxiParkId, null);
        } catch (SQLException e) {
            logger.error("Помилка при створенні автомобіля", e);
            return -1;
        }
    }

    // Отримання всіх автомобілів для певного таксопарку
    public List<Car> getCarsByTaxiParkId(int taxiParkId) {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT id, model, price, fuel_consumption, max_speed, type FROM cars WHERE taxi_park_id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, taxiParkId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String model = resultSet.getString("model");
                    double price = resultSet.getDouble("price");
                    double fuelConsumption = resultSet.getDouble("fuel_consumption");
                    int maxSpeed = resultSet.getInt("max_speed");
                    String type = resultSet.getString("type");

                    Car car = new Car(model, price, fuelConsumption, maxSpeed, type);
                    car.setId(id);
                    cars.add(car);
                }
            }

            logger.info("Отримано {} автомобілів для таксопарку id={}", cars.size(), taxiParkId);
        } catch (SQLException e) {
            logger.error("Помилка при отриманні автомобілів для таксопарку id={}", taxiParkId, e);
        }

        return cars;
    }

    // Отримання автомобіля за ID
    public Car getCarById(int id) {
        String sql = "SELECT model, price, fuel_consumption, max_speed, type FROM cars WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String model = resultSet.getString("model");
                    double price = resultSet.getDouble("price");
                    double fuelConsumption = resultSet.getDouble("fuel_consumption");
                    int maxSpeed = resultSet.getInt("max_speed");
                    String type = resultSet.getString("type");

                    Car car = new Car(model, price, fuelConsumption, maxSpeed, type);
                    car.setId(id);

                    logger.info("Отримано автомобіль: id={}, модель={}", id, model);
                    return car;
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка при отриманні автомобіля з id={}", id, e);
        }

        return null;
    }

    // Оновлення автомобіля
    public boolean updateCar(Car car) {
        String sql = "UPDATE cars SET model = ?, price = ?, fuel_consumption = ?, max_speed = ?, type = ? WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, car.getModel());
            preparedStatement.setDouble(2, car.getPrice());
            preparedStatement.setDouble(3, car.getFuelConsumption());
            preparedStatement.setInt(4, car.getMaxSpeed());
            preparedStatement.setString(5, car.getType());
            preparedStatement.setInt(6, car.getId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Оновлено автомобіль: id={}, модель={}", car.getId(), car.getModel());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Помилка при оновленні автомобіля", e);
        }

        return false;
    }

    // Видалення автомобіля
    public boolean deleteCar(int id) {
        String sql = "DELETE FROM cars WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Видалено автомобіль з id={}", id);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Помилка при видаленні автомобіля", e);
        }

        return false;
    }

    // Отримання автомобілів за діапазоном швидкості
    public List<Car> getCarsBySpeedRange(int minSpeed, int maxSpeed, int taxiParkId) {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT id, model, price, fuel_consumption, max_speed, type FROM cars " +
                "WHERE taxi_park_id = ? AND max_speed BETWEEN ? AND ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, taxiParkId);
            preparedStatement.setInt(2, minSpeed);
            preparedStatement.setInt(3, maxSpeed);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String model = resultSet.getString("model");
                    double price = resultSet.getDouble("price");
                    double fuelConsumption = resultSet.getDouble("fuel_consumption");
                    int speed = resultSet.getInt("max_speed");
                    String type = resultSet.getString("type");

                    Car car = new Car(model, price, fuelConsumption, speed, type);
                    car.setId(id);
                    cars.add(car);
                }
            }

            logger.info("Знайдено {} автомобілів в діапазоні швидкості від {} до {} для таксопарку id={}",
                    cars.size(), minSpeed, maxSpeed, taxiParkId);
        } catch (SQLException e) {
            logger.error("Помилка при пошуку автомобілів за діапазоном швидкості", e);
        }

        return cars;
    }

    // Отримання автомобілів відсортованих за витратою пального
    public List<Car> getCarsSortedByFuelConsumption(int taxiParkId) {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT id, model, price, fuel_consumption, max_speed, type FROM cars " +
                "WHERE taxi_park_id = ? ORDER BY fuel_consumption";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, taxiParkId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String model = resultSet.getString("model");
                    double price = resultSet.getDouble("price");
                    double fuelConsumption = resultSet.getDouble("fuel_consumption");
                    int maxSpeed = resultSet.getInt("max_speed");
                    String type = resultSet.getString("type");

                    Car car = new Car(model, price, fuelConsumption, maxSpeed, type);
                    car.setId(id);
                    cars.add(car);
                }
            }

            logger.info("Отримано відсортований список автомобілів за витратою пального для таксопарку id={}", taxiParkId);
        } catch (SQLException e) {
            logger.error("Помилка при отриманні відсортованого списку автомобілів", e);
        }

        return cars;
    }

    // Отримання загальної вартості автомобілів таксопарку
    public double getTotalCarValueByTaxiParkId(int taxiParkId) {
        String sql = "SELECT SUM(price) as total_value FROM cars WHERE taxi_park_id = ?";
        double totalValue = 0.0;

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, taxiParkId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    totalValue = resultSet.getDouble("total_value");
                }
            }

            logger.info("Обчислена загальна вартість автомобілів для таксопарку id={}: {}", taxiParkId, totalValue);
        } catch (SQLException e) {
            logger.error("Помилка при обчисленні загальної вартості автомобілів", e);
        }

        return totalValue;
    }
}