package taxiPark.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

    private static String DATABASE = "taxipark";
    private static final String USER = "root";
    private static final String PASSWORD = "280900";

    private static final String BASE_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_URL = BASE_URL + DATABASE + "?serverTimezone=UTC";

    private static DatabaseConnectionManager instance;
    private Connection connection;

    public DatabaseConnectionManager() {
        try {
            logger.info("Ініціалізація DatabaseConnectionManager (MySQL)");

            createDatabaseIfNotExists();

            logger.info("Спроба підключення до бази даних {} за URL: {}", DATABASE, DB_URL);
            this.connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            logger.info("З'єднання з базою даних {} успішно встановлено", DATABASE);

            initializeDatabase();
        } catch (SQLException e) {
            logger.error("Помилка з'єднання з базою даних: {} (SQL State: {}, Error Code: {})",
                    e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
            throw new RuntimeException("Помилка з'єднання з базою даних: " + e.getMessage(), e);
        }
    }

    public void createDatabaseIfNotExists() throws SQLException {
        try (Connection rootConnection = DriverManager.getConnection(BASE_URL + "?serverTimezone=UTC", USER, PASSWORD)) {
            logger.info("Підключено до MySQL сервера");

            try (Statement statement = rootConnection.createStatement()) {
                String createDBQuery = "CREATE DATABASE IF NOT EXISTS `" + DATABASE + "` DEFAULT CHARACTER SET utf8mb4";
                statement.executeUpdate(createDBQuery);
                logger.info("Базу даних `{}` створено або вже існує", DATABASE);
            }
        } catch (SQLException e) {
            logger.error("Помилка при створенні бази даних: {} (SQL State: {}, Error Code: {})",
                    e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
            throw e;
        }
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                closeConnection();
                connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                logger.info("З'єднання з базою даних відновлено");
            }
        } catch (SQLException e) {
            logger.error("Помилка при отриманні з'єднання з базою даних", e);
            throw new RuntimeException("Помилка при отриманні з'єднання з базою даних", e);
        }
        return connection;
    }

    public void initializeDatabase() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            String createTaxiParkTable = "CREATE TABLE IF NOT EXISTS taxi_parks (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL" +
                    ")";
            statement.executeUpdate(createTaxiParkTable);

            String createCarsTable = "CREATE TABLE IF NOT EXISTS cars (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "model VARCHAR(255) NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "fuel_consumption DOUBLE NOT NULL," +
                    "max_speed INT NOT NULL," +
                    "type VARCHAR(255) NOT NULL," +
                    "taxi_park_id INT NOT NULL," +
                    "FOREIGN KEY (taxi_park_id) REFERENCES taxi_parks(id) ON DELETE CASCADE" +
                    ")";
            statement.executeUpdate(createCarsTable);

            logger.info("Таблиці успішно створено або вже існують");
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("З'єднання з базою даних закрито");
            }
        } catch (SQLException e) {
            logger.error("Помилка при закритті з'єднання з базою даних", e);
        }
    }

    public static String getDatabaseNameForTest() {
        return DATABASE;
    }

    public static void setDatabaseNameForTest(String name) {
        try {
            Field field = DatabaseConnectionManager.class.getDeclaredField("DATABASE");
            field.setAccessible(true);
            field.set(null, name);
        } catch (Exception e) {
            throw new RuntimeException("Не вдалося змінити DATABASE для тесту", e);
        }
    }
}
