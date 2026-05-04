package model;

import org.junit.Test;
import static org.junit.Assert.*;

import common.ItemCategory;
import server.model.*;

import java.util.Collections;

public class ItemTest {

    @Test
    public void testElectronicsCreation() {
        Electronics electronics = new Electronics("IT001", "Laptop",
                "High-performance laptop", 1000, "U001", "Dell", "24 months", Collections.emptyList());


        assertNotNull(electronics);
        assertEquals("IT001", electronics.getItemId());
        assertEquals("Laptop", electronics.getName());
        assertEquals("Dell", electronics.getBrand());
        assertEquals("24 months", electronics.getWarrantyPeriod());
        assertEquals(ItemCategory.ELECTRONICS, electronics.getCategory());

        System.out.println("Test electronicsCreation PASSED");
    }

    @Test
    public void testArtCreation() {
        Art art = new Art("IT002", "Painting", "Beautiful painting",
                500, "U001", "Van Gogh", "Oil on canvas", Collections.emptyList());


        assertNotNull(art);
        assertEquals("IT002", art.getItemId());
        assertEquals("Painting", art.getName());
        assertEquals("Van Gogh", art.getCreator());
        assertEquals("Oil on canvas", art.getMaterial());
        assertEquals(ItemCategory.ART, art.getCategory());

        System.out.println("Test artCreation PASSED");
    }

    @Test
    public void testVehicleCreation() {
        Vehicle vehicle = new Vehicle("IT003", "Honda CB500F",
                "Sports motorcycle", 3000, "U001", "Honda CB500F", 5200, Collections.emptyList());


        assertNotNull(vehicle);
        assertEquals("IT003", vehicle.getItemId());
        assertEquals("Honda CB500F", vehicle.getName());
        assertEquals("Honda CB500F", vehicle.getModel());
        assertEquals(5200, vehicle.getOdometer());
        assertEquals(ItemCategory.VEHICLE, vehicle.getCategory());

        System.out.println("Test vehicleCreation PASSED");
    }

    @Test
    public void testFashionCreation() {
        Fashion fashion = new Fashion("IT004", "Nike Shoes",
                "Running shoes", 150, "U001", "Nike", "Leather", Collections.emptyList());


        assertNotNull(fashion);
        assertEquals("IT004", fashion.getItemId());
        assertEquals("Nike Shoes", fashion.getName());
        assertEquals("Nike", fashion.getBrand());
        assertEquals("Leather", fashion.getMaterial());
        assertEquals(ItemCategory.FASHION, fashion.getCategory());

        System.out.println("Test fashionCreation PASSED");
    }

    @Test
    public void testJewelryCreation() {
        Jewelry jewelry = new Jewelry("IT005", "Gold Ring",
                "Beautiful gold ring", 500, "U001", "Gold", 5.5, Collections.emptyList());


        assertNotNull(jewelry);
        assertEquals("IT005", jewelry.getItemId());
        assertEquals("Gold Ring", jewelry.getName());
        assertEquals("Gold", jewelry.getMaterial());
        assertEquals(5.5, jewelry.getWeight(), 0.01);
        assertEquals(ItemCategory.JEWELRY, jewelry.getCategory());

        System.out.println("Test jewelryCreation PASSED");
    }

    @Test
    public void testElectronicsDetailedInfo() {
        Electronics electronics = new Electronics("IT001", "Laptop",
                "High-performance laptop", 1000, "U001", "Dell", "24 months", Collections.emptyList());



        String info = electronics.getDetailedInfo();
        assertNotNull(info);
        assertTrue(info.contains("Laptop"));
        assertTrue(info.contains("Dell"));
        assertTrue(info.contains("24 months"));
        assertTrue(info.contains("1000"));

        System.out.println("Test electronicsDetailedInfo PASSED");
    }

    @Test
    public void testElectronicsWithNullBrand() {
        Electronics electronics = new Electronics("IT001", "Laptop",
                "Laptop", 1000, "U001", null, "12 months", Collections.emptyList());


        assertNull(electronics.getBrand());

        System.out.println("Test electronicsWithNullBrand PASSED");
    }
}
