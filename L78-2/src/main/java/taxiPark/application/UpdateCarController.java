package taxiPark.application;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import taxiPark.model.Car;
import taxiPark.database.CarDAO;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateCarController {
    private static final Logger logger = Logger.getLogger(UpdateCarController.class.getName());

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

    private Car car;
    private CarDAO carDAO;
    private Stage dialogStage;
    private boolean okClicked = false;

    @FXML
    public void initialize() {
        // Ініціалізація контролера при завантаженні FXML
        this.carDAO = new CarDAO();
    }

    public void setCar(Car car) {
        this.car = car;

        // Заповнення полів форми даними автомобіля
        modelField.setText(car.getModel());
        priceField.setText(String.valueOf(car.getPrice()));
        fuelConsumptionField.setText(String.valueOf(car.getFuelConsumption()));
        maxSpeedField.setText(String.valueOf(car.getMaxSpeed()));
        typeField.setText(car.getType());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleOk() {
        if (isInputValid()) {
            try {
                if (car == null) {
                    throw new IllegalStateException("Об'єкт автомобіля не був встановлений");
                }

                car.setModel(modelField.getText());
                car.setPrice(Double.parseDouble(priceField.getText()));
                car.setFuelConsumption(Double.parseDouble(fuelConsumptionField.getText()));
                car.setMaxSpeed(Integer.parseInt(maxSpeedField.getText()));
                car.setType(typeField.getText());

                // Оновлюємо автомобіль в базі даних
                if (carDAO.updateCar(car)) {
                    logger.log(Level.INFO, "Оновлено інформацію про автомобіль: {0}, тип: {1}, ціна: {2}, витрата пального: {3}, максимальна швидкість: {4}",
                            new Object[]{car.getModel(), car.getType(), car.getPrice(), car.getFuelConsumption(), car.getMaxSpeed()});

                    okClicked = true;

                    if (dialogStage != null) {
                        dialogStage.close();
                    } else {
                        logger.log(Level.WARNING, "dialogStage не встановлено, неможливо закрити вікно");
                    }
                } else {
                    showAlert("Помилка оновлення", "Помилка при оновленні автомобіля в базі даних.");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Помилка під час оновлення автомобіля: ", e);
                showAlert("Помилка введення", "Сталася помилка під час обробки введених даних: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            logger.log(Level.WARNING, "dialogStage не встановлено, неможливо закрити вікно");
        }
    }

    private boolean isInputValid() {
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}