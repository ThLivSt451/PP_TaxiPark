package taxiPark.database;

import taxiPark.model.Car;
import taxiPark.service.TaxiPark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaxiParkDAO {
    private static final Logger logger = LoggerFactory.getLogger(TaxiParkDAO.class);
    private final DatabaseConnectionManager connectionManager;

    public TaxiParkDAO() {
        this(DatabaseConnectionManager.getInstance());
    }

    public TaxiParkDAO(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public int createTaxiPark(TaxiPark taxiPark) {
        String sql = "INSERT INTO taxi_parks (name) VALUES (?)";
        int generatedId = -1;
        Connection connection = null;

        try {
            // Отримуємо з'єднання
            connection = connectionManager.getConnection();

            try {
                logger.info("Спроба створити таксопарк з назвою: {}", taxiPark.getName());

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, taxiPark.getName());
                    int rowsAffected = preparedStatement.executeUpdate();

                    if (rowsAffected > 0) {
                        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                generatedId = generatedKeys.getInt(1);
                                // ID для об'єкта таксопарку
                                taxiPark.setId(generatedId);
                                logger.info("Створено новий таксопарк з id={}: {}", generatedId, taxiPark.getName());

                                // Зберігаємо автомобілі таксопарку, якщо вони є
                                if (taxiPark.getCars() != null && !taxiPark.getCars().isEmpty()) {
                                    CarDAO carDAO = new CarDAO();
                                    for (Car car : taxiPark.getCars()) {
                                        // передаємо connection для використання тієї ж транзакції
                                        int carId = carDAO.createCar(car, generatedId, connection);
                                        if (carId <= 0) {
                                            throw new SQLException("Не вдалося додати автомобіль до таксопарку");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                connection.commit();
                logger.info("Транзакцію створення таксопарку успішно підтверджено");

            } catch (SQLException e) {
                // Відкликаємо транзакцію в разі помилки
                if (connection != null) {
                    try {
                        connection.rollback();
                        logger.warn("Транзакцію відкликано через помилку");
                    } catch (SQLException re) {
                        logger.error("Помилка при відкликанні транзакції: {}", re.getMessage(), re);
                    }
                }
                logger.error("Помилка при створенні таксопарку: {} (SQL State: {}, Error Code: {})",
                        e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Критична помилка при роботі з базою даних: {} (SQL State: {}, Error Code: {})",
                    e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }

        return generatedId;
    }

    public List<TaxiPark> getAllTaxiParks() {
        List<TaxiPark> taxiParks = new ArrayList<>();
        String sql = "SELECT id, name FROM taxi_parks";

        try (Connection connection = connectionManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            List<Integer> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();

            while (resultSet.next()) {
                ids.add(resultSet.getInt("id"));
                names.add(resultSet.getString("name"));
            }

            CarDAO carDAO = new CarDAO();

            for (int i = 0; i < ids.size(); i++) {
                int id = ids.get(i);
                String name = names.get(i);

                List<Car> cars = carDAO.getCarsByTaxiParkId(id);
                TaxiPark taxiPark = new TaxiPark(name, cars);
                taxiPark.setId(id);
                taxiParks.add(taxiPark);
            }

            logger.info("Отримано {} таксопарків з бази даних", taxiParks.size());

        } catch (SQLException e) {
            logger.error("Помилка при отриманні списку таксопарків: {} (SQL State: {}, Error Code: {})",
                    e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }
        return taxiParks;
    }

    public TaxiPark getTaxiParkById(int id) {
        String sql = "SELECT name FROM taxi_parks WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("name");

                    CarDAO carDAO = new CarDAO();
                    List<Car> cars = carDAO.getCarsByTaxiParkId(id);

                    TaxiPark taxiPark = new TaxiPark(name, cars);
                    taxiPark.setId(id);

                    logger.info("Отримано таксопарк: id={}, назва={}", id, name);
                    return taxiPark;
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка при отриманні таксопарку з id={}: {} (SQL State: {}, Error Code: {})",
                    id, e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }

        return null;
    }

    public boolean updateTaxiPark(TaxiPark taxiPark) {
        String sql = "UPDATE taxi_parks SET name = ? WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, taxiPark.getName());
            preparedStatement.setInt(2, taxiPark.getId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Оновлено таксопарк: id={}, назва={}", taxiPark.getId(), taxiPark.getName());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Помилка при оновленні таксопарку: {} (SQL State: {}, Error Code: {})",
                    e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }

        return false;
    }

    public boolean deleteTaxiPark(int id) {
        String sql = "DELETE FROM taxi_parks WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Видалено таксопарк з id={}", id);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Помилка при видаленні таксопарку: {} (SQL State: {}, Error Code: {})",
                    e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }

        return false;
    }
}