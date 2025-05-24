package taxiPark;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import taxiPark.database.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxiParkApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(TaxiParkApplication.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Ініціалізуємо підключення до бази даних
            DatabaseConnectionManager.getInstance();

            // Завантажуємо FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = loader.load();

            // Налаштовуємо основну сцену
            Scene scene = new Scene(root);
            primaryStage.setTitle("Taxi Park Manager");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            logger.info("JavaFX GUI додаток запущено");

            // Закриваємо з'єднання при виході
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Закриття додатку");
                DatabaseConnectionManager.getInstance().closeConnection();
            });
        } catch (Exception e) {
            logger.error("Помилка при запуску JavaFX додатку", e);
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        launch(args);
    }
}