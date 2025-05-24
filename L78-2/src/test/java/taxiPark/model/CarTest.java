package taxiPark.model;

import org.junit.jupiter.api.Test;
import taxiPark.model.Car;

import static org.junit.jupiter.api.Assertions.*;

class CarTest {
    @Test
    void getIdAndSetId() {
        Car car = new Car("Tesla Model 3", 45000, 5.5, 250, "Electric");
        car.setId(1);
        assertEquals(1, car.getId());
    }

    @Test
    void getModelAndSetModel() {
        Car car = new Car("Tesla Model 3", 45000, 5.5, 250, "Electric");
        assertEquals("Tesla Model 3", car.getModel());
        car.setModel("Tesla Model S");
        assertEquals("Tesla Model S", car.getModel());
    }

    @Test
    void getPriceAndSetPrice() {
        Car car = new Car("Tesla Model 3", 45000, 5.5, 250, "Electric");
        assertEquals(45000, car.getPrice());
        car.setPrice(50000);
        assertEquals(50000, car.getPrice());
    }

    @Test
    void getFuelConsumptionAndSetFuelConsumption() {
        Car car = new Car("Tesla Model 3", 45000, 5.5, 250, "Electric");
        assertEquals(5.5, car.getFuelConsumption());
        car.setFuelConsumption(6.0);
        assertEquals(6.0, car.getFuelConsumption());
    }

    @Test
    void getMaxSpeedAndSetMaxSpeed() {
        Car car = new Car("Tesla Model 3", 45000, 5.5, 250, "Electric");
        assertEquals(250, car.getMaxSpeed());
        car.setMaxSpeed(260);
        assertEquals(260, car.getMaxSpeed());
    }

    @Test
    void getTypeAndSetType() {
        Car car = new Car("Tesla Model 3", 45000, 5.5, 250, "Electric");
        assertEquals("Electric", car.getType());
        car.setType("Hybrid");
        assertEquals("Hybrid", car.getType());
    }
}