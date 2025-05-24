package taxiPark.application;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import taxiPark.model.Car;
import taxiPark.service.TaxiPark;
import taxiPark.database.CarDAO;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AddCarController {
    private static final Logger logger = Logger.getLogger(AddCarController.class.getName());

    @FXML
    private TextField modelField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField fuelConsumptionField;

    @FXML
    private TextField maxSpeedField;

    @FXML
    private TextField typeField;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private TaxiPark taxiPark;
    private CarDAO carDAO;
    private Stage dialogStage;
    private boolean okClicked = false;

    @FXML
    public void initialize() {
        // Ініціалізація контролера при завантаженні FXML
        this.carDAO = new CarDAO();
    }

    public void setTaxiPark(TaxiPark taxiPark) {
        this.taxiPark = taxiPark;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    void handleOk() {
        if (isInputValid()) {
            try {
                String model = modelField.getText();
                double price = Double.parseDouble(priceField.getText());
                double fuelConsumption = Double.parseDouble(fuelConsumptionField.getText());
                int maxSpeed = Integer.parseInt(maxSpeedField.getText());
                String type = typeField.getText();

                Car newCar = new Car(model, price, fuelConsumption, maxSpeed, type);

                // Додаємо автомобіль до бази даних
                int carId = carDAO.createCar(newCar, taxiPark.getId());

                if (carId > 0) {
                    newCar.setId(carId);
                    taxiPark.addCar(newCar);
                    logger.log(Level.INFO, "Додано новий автомобіль: {0}, тип: {1}, ціна: {2}, витрата пального: {3}, максимальна швидкість: {4}",
                            new Object[]{model, type, price, fuelConsumption, maxSpeed});

                    okClicked = true;
                    dialogStage.close();
                } else {
                    showAlert("Помилка додавання", "Помилка при додаванні автомобіля до бази даних.");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Помилка під час додавання автомобіля: ", e);
                showAlert("Помилка введення", "Сталася помилка під час обробки введених даних. " + e.getMessage());
            }
        }
    }

    @FXML
    void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            logger.log(Level.WARNING, "dialogStage не встановлено, неможливо закрити вікно");
        }
    }

    boolean isInputValid() {
        String errorMessage = "";

        if (modelField.getText() == null || modelField.getText().isEmpty()) {
            errorMessage += "Не вказана модель автомобіля!\n";
        }

        if (priceField.getText() == null || priceField.getText().isEmpty()) {
            errorMessage += "Не вказана ціна автомобіля!\n";
        } else {
            try {
                Double.parseDouble(priceField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Некоректний формат ціни (має бути число)!\n";
            }
        }

        if (fuelConsumptionField.getText() == null || fuelConsumptionField.getText().isEmpty()) {
            errorMessage += "Не вказана витрата пального!\n";
        } else {
            try {
                Double.parseDouble(fuelConsumptionField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Некоректний формат витрати пального (має бути число)!\n";
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

        if (typeField.getText() == null || typeField.getText().isEmpty()) {
            errorMessage += "Не вказаний тип автомобіля!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert("Помилка введення даних", errorMessage);
            return false;
        }
    }

    void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}