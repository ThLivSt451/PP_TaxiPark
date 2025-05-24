package taxiPark.application;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import taxiPark.model.Car;
import taxiPark.service.TaxiPark;
import taxiPark.database.CarDAO;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpeedRangeController {
    private static final Logger logger = Logger.getLogger(SpeedRangeController.class.getName());

    @FXML
    private TextField minSpeedField;

    @FXML
    private TextField maxSpeedField;

    @FXML
    private ListView<Car> resultsListView;

    @FXML
    private Button searchButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private TaxiPark taxiPark;
    private CarDAO carDAO;
    private Stage dialogStage;
    private MainController mainController;
    private boolean okClicked = false;

    @FXML
    public void initialize() {
        this.carDAO = new CarDAO();
    }

    public void setTaxiPark(TaxiPark taxiPark) {
        this.taxiPark = taxiPark;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleSearch() {
        if (isInputValid()) {
            try {
                int minSpeed = Integer.parseInt(minSpeedField.getText());
                int maxSpeed = Integer.parseInt(maxSpeedField.getText());

                List<Car> foundCars = carDAO.getCarsBySpeedRange(minSpeed, maxSpeed, taxiPark.getId());

                resultsListView.getItems().clear();
                resultsListView.getItems().addAll(foundCars);

                logger.log(Level.INFO, "Пошук автомобілів за діапазоном швидкості: {0} - {1}, знайдено: {2}",
                        new Object[]{minSpeed, maxSpeed, foundCars.size()});

                okClicked = true;

                if (foundCars.isEmpty()) {
                    showAlert("Інформація", "Автомобілі в заданому діапазоні швидкостей не знайдені.");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Помилка під час пошуку автомобілів: ", e);
                showAlert("Помилка пошуку", "Сталася помилка під час пошуку автомобілів: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            logger.log(Level.WARNING, "dialogStage не встановлено, неможливо закрити вікно");
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (minSpeedField.getText() == null || minSpeedField.getText().isEmpty()) {
            errorMessage += "Не вказана мінімальна швидкість!\n";
        } else {
            try {
                Integer.parseInt(minSpeedField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Некоректний формат мінімальної швидкості (має бути ціле число)!\n";
            }
        }

        if (maxSpeedField.getText() == null || maxSpeedField.getText().isEmpty()) {
            errorMessage += "Не вказана максимальна швидкість!\n";
        } else {
            try {
                Integer.parseInt(maxSpeedField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Некоректний формат максимальної швидкості (має бути ціле число)!\n";
            }
        }

        if (errorMessage.isEmpty()) {
            int minSpeed = Integer.parseInt(minSpeedField.getText());
            int maxSpeed = Integer.parseInt(maxSpeedField.getText());

            if (minSpeed > maxSpeed) {
                errorMessage += "Мінімальна швидкість не може бути більша за максимальну!\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert("Помилка введення даних", errorMessage);
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert.AlertType alertType = title.startsWith("Помилка") ?
                Alert.AlertType.ERROR :
                Alert.AlertType.INFORMATION;
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}