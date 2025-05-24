package taxiPark.application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import taxiPark.database.CarDAO;
import taxiPark.service.TaxiPark;
import taxiPark.database.TaxiParkDAO;
import taxiPark.model.Car;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Label currentTaxiParkLabel;
    @FXML private ListView<Car> carListView;
    @FXML private Button addCarButton;
    @FXML private Button removeCarButton;
    @FXML private Button updateCarButton;
    @FXML private Button sortByFuelButton;
    @FXML private Button findBySpeedButton;
    @FXML private Button calculateValueButton;

    private TaxiPark currentTaxiPark;
    private TaxiParkDAO taxiParkDAO;

    @FXML
    public void initialize() {
        taxiParkDAO = new TaxiParkDAO();
        disableTaxiParkFeatures();

        // Налаштовуємо відображення автомобілів
        carListView.setCellFactory(param -> new ListCell<Car>() {
            @Override
            protected void updateItem(Car car, boolean empty) {
                super.updateItem(car, empty);
                if (empty || car == null) {
                    setText(null);
                } else {
                    setText(car.toString());
                }
            }
        });

        logger.info("MainController initialized");
    }

    @FXML
    public void handleCreateTaxiPark(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Створення таксопарку");
        dialog.setHeaderText("Введіть інформацію про новий таксопарк");
        dialog.setContentText("Назва таксопарку:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String name = result.get().trim();
            TaxiPark newTaxiPark = new TaxiPark(name, List.of());

            int taxiParkId = taxiParkDAO.createTaxiPark(newTaxiPark);
            if (taxiParkId > 0) {
                // Отримуємо оновлений таксопарк з бази даних для коректних даних
                TaxiPark createdTaxiPark = taxiParkDAO.getTaxiParkById(taxiParkId);
                if (createdTaxiPark != null) {
                    setCurrentTaxiPark(createdTaxiPark);

                    logger.info("Створено новий таксопарк: {}", name);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Успіх");
                    alert.setHeaderText(null);
                    alert.setContentText("Таксопарк '" + name + "' успішно створено.");
                    alert.showAndWait();
                } else {
                    logger.error("Не вдалося завантажити створений таксопарк з ID: {}", taxiParkId);
                    showErrorAlert("Таксопарк створено, але не вдалося завантажити його дані. Будь ласка, виберіть таксопарк зі списку.");
                }
            } else {
                logger.error("Помилка при створенні таксопарку в базі даних");
                showErrorAlert("Помилка при створенні таксопарку в базі даних.");
            }
        }
    }

    @FXML
    public void handleChooseTaxiPark(ActionEvent event) {
        // Отримуємо свіжий список таксопарків з бази даних
        List<TaxiPark> taxiParks = taxiParkDAO.getAllTaxiParks();

        if (taxiParks.isEmpty()) {
            showInfoAlert("Немає доступних таксопарків у базі даних. Спочатку створіть таксопарк.");
            return;
        }

        // Створюємо кастомізований діалог вибору
        Dialog<TaxiPark> dialog = new Dialog<>();
        dialog.setTitle("Вибір таксопарку");
        dialog.setHeaderText("Оберіть таксопарк зі списку");

        // Додаємо кнопки OK та Cancel
        ButtonType confirmButtonType = new ButtonType("Обрати", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Створюємо ListView для таксопарків
        ListView<TaxiPark> taxiParkListView = new ListView<>();
        taxiParkListView.getItems().addAll(taxiParks);

        // Встановлюємо власний спосіб відображення елементів
        taxiParkListView.setCellFactory(lv -> new ListCell<TaxiPark>() {
            @Override
            protected void updateItem(TaxiPark taxiPark, boolean empty) {
                super.updateItem(taxiPark, empty);
                if (empty || taxiPark == null) {
                    setText(null);
                } else {
                    setText(taxiPark.getName() + " (ID: " + taxiPark.getId() + ", Автомобілів: " +
                            (taxiPark.getCars() != null ? taxiPark.getCars().size() : 0) + ")");
                }
            }
        });

        // Додаємо ListView до діалогу
        dialog.getDialogPane().setContent(taxiParkListView);

        // Отримуємо обраний таксопарк після натискання кнопки OK
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return taxiParkListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Показуємо діалог та обробляємо результат
        Optional<TaxiPark> result = dialog.showAndWait();

        result.ifPresent(selectedTaxiPark -> {
            // Отримуємо свіжі дані таксопарку з бази даних
            TaxiPark refreshedTaxiPark = taxiParkDAO.getTaxiParkById(selectedTaxiPark.getId());
            if (refreshedTaxiPark != null) {
                setCurrentTaxiPark(refreshedTaxiPark);
                logger.info("Обрано таксопарк: {} (ID: {})", refreshedTaxiPark.getName(), refreshedTaxiPark.getId());
            } else {
                logger.error("Не вдалося завантажити вибраний таксопарк з ID: {}", selectedTaxiPark.getId());
                showErrorAlert("Не вдалося завантажити вибраний таксопарк. Спробуйте ще раз.");
            }
        });
    }

    @FXML
    public void handleAddCar(ActionEvent event) {
        if (currentTaxiPark == null) {
            showInfoAlert("Спочатку оберіть таксопарк.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/addCar.fxml"));
            Parent root = loader.load();

            // Передаємо поточний таксопарк контролеру форми
            AddCarController controller = loader.getController();
            controller.setTaxiPark(currentTaxiPark);

            // Важливо: встановлюємо діалогову сцену для контролера
            Stage stage = new Stage();
            controller.setDialogStage(stage);

            stage.setTitle("Додавання нового автомобіля");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Оновлюємо список автомобілів після закриття вікна
            refreshCarList();

        } catch (IOException e) {
            logger.error("Помилка при завантаженні форми додавання автомобіля", e);
            showErrorAlert("Помилка при завантаженні форми додавання автомобіля: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRemoveCar(ActionEvent event) {
        if (currentTaxiPark == null) {
            showInfoAlert("Спочатку оберіть таксопарк.");
            return;
        }

        Car selectedCar = carListView.getSelectionModel().getSelectedItem();
        if (selectedCar == null) {
            showInfoAlert("Спочатку оберіть автомобіль зі списку.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Підтвердження видалення");
        alert.setHeaderText("Видалення автомобіля");
        alert.setContentText("Ви впевнені, що хочете видалити автомобіль: " + selectedCar.getModel() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Видаляємо автомобіль з бази даних через DAO
            boolean isDeleted = new CarDAO().deleteCar(selectedCar.getId());

            if (isDeleted) {
                logger.info("Автомобіль видалено: {}", selectedCar.getModel());
                showInfoAlert("Автомобіль видалено.");

                // Оновлюємо поточний таксопарк з бази даних
                TaxiPark refreshedTaxiPark = taxiParkDAO.getTaxiParkById(currentTaxiPark.getId());
                if (refreshedTaxiPark != null) {
                    setCurrentTaxiPark(refreshedTaxiPark);
                } else {
                    // Якщо не вдалося отримати оновлені дані, просто оновлюємо список
                    refreshCarList();
                }
            } else {
                logger.error("Помилка при видаленні автомобіля з бази даних");
                showErrorAlert("Помилка при видаленні автомобіля з бази даних.");
            }
        }
    }

    @FXML
    public void handleUpdateCar(ActionEvent event) {
        if (currentTaxiPark == null) {
            showInfoAlert("Спочатку оберіть таксопарк.");
            return;
        }

        Car selectedCar = carListView.getSelectionModel().getSelectedItem();
        if (selectedCar == null) {
            showInfoAlert("Спочатку оберіть автомобіль зі списку.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/updateCar.fxml"));
            Parent root = loader.load();

            // Передаємо вибраний автомобіль контролеру форми
            UpdateCarController controller = loader.getController();
            controller.setCar(selectedCar);

            // Важливо: встановлюємо діалогову сцену для контролера
            Stage stage = new Stage();
            controller.setDialogStage(stage);

            stage.setTitle("Оновлення інформації про автомобіль");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Оновлюємо список автомобілів після закриття вікна
            refreshCarList();

        } catch (IOException e) {
            logger.error("Помилка при завантаженні форми оновлення автомобіля", e);
            showErrorAlert("Помилка при завантаженні форми оновлення автомобіля: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSortByFuel(ActionEvent event) {
        if (currentTaxiPark == null) {
            showInfoAlert("Спочатку оберіть таксопарк.");
            return;
        }

        carListView.getItems().clear();
        carListView.getItems().addAll(new CarDAO().getCarsSortedByFuelConsumption(currentTaxiPark.getId()));
    }

    @FXML
    public void handleFindBySpeed(ActionEvent event) {
        if (currentTaxiPark == null) {
            showInfoAlert("Спочатку оберіть таксопарк.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/speedRangeSearch.fxml"));
            Parent root = loader.load();

            // Передаємо поточний таксопарк контролеру форми
            SpeedRangeController controller = loader.getController();
            controller.setTaxiPark(currentTaxiPark);
            controller.setMainController(this);

            Stage stage = new Stage();
            controller.setDialogStage(stage);

            stage.setTitle("Пошук за діапазоном швидкості");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Помилка при завантаженні форми пошуку за швидкістю", e);
            showErrorAlert("Помилка при завантаженні форми пошуку за швидкістю: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCalculateValue(ActionEvent event) {
        if (currentTaxiPark == null) {
            showInfoAlert("Спочатку оберіть таксопарк.");
            return;
        }

        double totalValue = new CarDAO().getTotalCarValueByTaxiParkId(currentTaxiPark.getId());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Вартість автопарку");
        alert.setHeaderText(null);
        alert.setContentText(String.format("Загальна вартість автопарку '%s': %.2f грн.",
                currentTaxiPark.getName(), totalValue));
        alert.showAndWait();
    }

    // Метод для оновлення списку автомобілів
    public void refreshCarList() {
        if (currentTaxiPark != null) {
            int taxiParkId = currentTaxiPark.getId();
            List<Car> cars = new CarDAO().getCarsByTaxiParkId(taxiParkId);

            carListView.getItems().clear();
            carListView.getItems().addAll(cars);

            logger.info("Оновлено список автомобілів для таксопарку id={}, знайдено {} авто",
                    taxiParkId, cars.size());
        }
    }

    // Встановлення поточного таксопарку
    private void setCurrentTaxiPark(TaxiPark taxiPark) {
        this.currentTaxiPark = taxiPark;
        currentTaxiParkLabel.setText("Поточний таксопарк: " + taxiPark.getName());
        enableTaxiParkFeatures();
        refreshCarList();
        logger.info("Обрано таксопарк: {} (ID: {})", taxiPark.getName(), taxiPark.getId());
    }

    // Увімкнення функцій роботи з таксопарком
    private void enableTaxiParkFeatures() {
        addCarButton.setDisable(false);
        removeCarButton.setDisable(false);
        updateCarButton.setDisable(false);
        sortByFuelButton.setDisable(false);
        findBySpeedButton.setDisable(false);
        calculateValueButton.setDisable(false);
    }

    // Вимкнення функцій роботи з таксопарком
    private void disableTaxiParkFeatures() {
        addCarButton.setDisable(true);
        removeCarButton.setDisable(true);
        updateCarButton.setDisable(true);
        sortByFuelButton.setDisable(true);
        findBySpeedButton.setDisable(true);
        calculateValueButton.setDisable(true);
    }

    // Utilities for showing alerts
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Інформація");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}