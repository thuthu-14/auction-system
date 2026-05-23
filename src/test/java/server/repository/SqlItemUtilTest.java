package server.repository;

import org.junit.jupiter.api.Test;
import server.model.Art;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Jewelry;
import server.model.OtherItem;
import server.model.Vehicle;

import static org.junit.jupiter.api.Assertions.*;

class SqlItemUtilTest {
    @Test
    void extractsTypeSpecificFields() {
        Electronics electronics = new Electronics();
        electronics.setBrand("Sony");
        electronics.setWarrantyPeriod("12 months");
        assertEquals("Sony", SqlItemUtil.getBrand(electronics));
        assertEquals("12 months", SqlItemUtil.getWarranty(electronics));

        Fashion fashion = new Fashion();
        fashion.setBrand("Uniqlo");
        fashion.setMaterial("Cotton");
        assertEquals("Uniqlo", SqlItemUtil.getBrand(fashion));
        assertEquals("Cotton", SqlItemUtil.getMaterial(fashion));

        Vehicle vehicle = new Vehicle();
        vehicle.setModel("2020");
        vehicle.setOdometer(1000);
        assertEquals("2020", SqlItemUtil.getModel(vehicle));
        assertEquals(1000, SqlItemUtil.getOdometer(vehicle));

        Jewelry jewelry = new Jewelry();
        jewelry.setMaterial("Gold");
        jewelry.setWeight(1.5);
        assertEquals("Gold", SqlItemUtil.getMaterial(jewelry));
        assertEquals(1.5, SqlItemUtil.getWeight(jewelry));

        Art art = new Art();
        art.setCreator("Painter");
        art.setMaterial("Oil");
        assertEquals("Painter", SqlItemUtil.getCreator(art));
        assertEquals("Oil", SqlItemUtil.getMaterial(art));
    }

    @Test
    void returnsNullForUnsupportedFields() {
        OtherItem item = new OtherItem();

        assertNull(SqlItemUtil.getBrand(item));
        assertNull(SqlItemUtil.getWarranty(item));
        assertNull(SqlItemUtil.getModel(item));
        assertNull(SqlItemUtil.getOdometer(item));
        assertNull(SqlItemUtil.getMaterial(item));
        assertNull(SqlItemUtil.getWeight(item));
        assertNull(SqlItemUtil.getCreator(item));
    }
}
