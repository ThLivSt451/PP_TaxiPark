package taxiPark.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Car implements Serializable {
    private int id;
    private String model;
    private double price;
    private double fuelConsumption;
    private int maxSpeed;
    private String type;

    public Car(String model, double price, double fuelConsumption, int maxSpeed, String type) {
        this.model = model;
        this.price = price;
        this.fuelConsumption = fuelConsumption;
        this.maxSpeed = maxSpeed;
        this.type = type;
    }
}