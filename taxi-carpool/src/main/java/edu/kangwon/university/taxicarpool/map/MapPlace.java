package edu.kangwon.university.taxicarpool.map;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MapPlace {

    @Column(name = "address")
    private String name;

    @Column(name = "roadAddressName")
    private String roadAddressName;

    @Column(name = "longitude")
    private double x;

    @Column(name = "latitude")
    private double y;

    public MapPlace() {
    }

    public MapPlace(String name, String roadAddressName, double x, double y) {
        this.name = name;
        this.roadAddressName = roadAddressName;
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoadAddressName() {
        return roadAddressName;
    }

    public void setRoadAddressName(String roadAddressName) {
        this.roadAddressName = roadAddressName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
