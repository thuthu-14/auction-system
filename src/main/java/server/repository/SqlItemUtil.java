package server.repository;

import server.model.*;

public class SqlItemUtil {

    public static String getBrand(Item item) {
        if (item instanceof Electronics e) return e.getBrand();
        if (item instanceof Fashion f) return f.getBrand();
        return null;
    }

    public static String getWarranty(Item item) {
        if (item instanceof Electronics e) return e.getWarrantyPeriod();
        return null;
    }

    public static String getModel(Item item) {
        if (item instanceof Vehicle v) return v.getModel();
        return null;
    }

    public static Integer getOdometer(Item item) {
        if (item instanceof Vehicle v) return v.getOdometer();
        return null;
    }

    public static String getMaterial(Item item) {
        if (item instanceof Fashion f) return f.getMaterial();
        if (item instanceof Jewelry j) return j.getMaterial();
        if (item instanceof Art a) return a.getMaterial();
        return null;
    }

    public static Double getWeight(Item item) {
        if (item instanceof Jewelry j) return j.getWeight();
        return null;
    }

    public static String getCreator(Item item) {
        if (item instanceof Art a) return a.getCreator();
        return null;
    }
}