package taxiPark.service;

import taxiPark.model.Car;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaxiParkTest {
    private TaxiPark taxiPark;
    private List<Car> cars;

    @BeforeEach
    void setUp() {
        cars = new ArrayList<>();
        cars.add(new Car("Tesla Model 3", 45000, 5.5, 250, "Electric"));
        cars.add(new Car("Toyota Camry", 30000, 7.2, 210, "Gasoline"));
        taxiPark = new TaxiPark("My Taxi Park", cars);
    }

    @Test
    void getIdAndSetId() {
        taxiPark.setId(1);
        assertEquals(1, taxiPark.getId());
    }

    @Test
    void getNameAndSetName() {
        assertEquals("My Taxi Park", taxiPark.getName());
        taxiPark.setName("Updated Taxi Park");
        assertEquals("Updated Taxi Park", taxiPark.getName());
    }

    @Test
    void getCars() {
        List<Car> returnedCars = taxiPark.getCars();
        assertEquals(2, returnedCars.size());
        assertEquals("Tesla Model 3", returnedCars.get(0).getModel());
    }

    @Test
    void addCar() {
        Car newCar = new Car("BMW i3", 42000, 4.8, 230, "Electric");
        taxiPark.addCar(newCar);
        assertEquals(3, taxiPark.getCars().size());
        assertEquals("BMW i3", taxiPark.getCars().get(2).getModel());
    }

    @Test
    void removeCar_Success() {
        boolean result = taxiPark.removeCar(0);
        assertTrue(result);
        assertEquals(1, taxiPark.getCars().size());
        assertEquals("Toyota Camry", taxiPark.getCars().get(0).getModel());
    }

    @Test
    void removeCar_Failure() {
        boolean result = taxiPark.removeCar(5);
        assertFalse(result);
        assertEquals(2, taxiPark.getCars().size());
    }

    @Test
    void getCar_Success() {
        Car car = taxiPark.getCar(0);
        assertNotNull(car);
        assertEquals("Tesla Model 3", car.getModel());
    }

    @Test
    void getCar_Failure() {
        Car car = taxiPark.getCar(5);
        assertNull(car);
    }
}