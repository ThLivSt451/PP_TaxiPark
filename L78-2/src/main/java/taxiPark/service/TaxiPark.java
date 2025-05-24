package taxiPark.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import taxiPark.model.Car;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@AllArgsConstructor
public class TaxiPark implements Serializable {
    @Getter
    private int id;
    @Getter
    private String name;
    private List<Car> cars;

    public TaxiPark(String name, List<Car> cars) {
        this.name = name;
        this.cars = cars;
    }

    public void addCar(Car car) {
        cars.add(car);
    }

    public boolean removeCar(int index) {
        if (index >= 0 && index < cars.size()) {
            cars.remove(index);
            return true;
        }
        return false;
    }

    public Car getCar(int index) {
        if (index >= 0 && index < cars.size()) {
            return cars.get(index);
        }
        return null;
    }

    public List<Car> getCars() {
        return new ArrayList<>(cars);
    }

}

