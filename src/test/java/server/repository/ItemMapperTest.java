package server.repository;

import common.ItemCategory;
import org.junit.jupiter.api.Test;
import server.model.Art;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.OtherItem;
import server.model.Vehicle;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemMapperTest {
    @Test
    void fromResultSetCreatesSpecificTypesAndMapsCommonFields() throws Exception {
        ResultSet rs = itemRow("Electronics", "ELECTRONICS");
        when(rs.getString("brand")).thenReturn("Sony");
        when(rs.getString("warranty_period")).thenReturn("12 months");

        Item item = ItemMapper.fromResultSet(rs, List.of("IMG1", "IMG2"));

        Electronics electronics = assertInstanceOf(Electronics.class, item);
        assertEquals("I1", electronics.getItemId());
        assertEquals("Camera", electronics.getName());
        assertEquals("Desc", electronics.getDescription());
        assertEquals(100.0, electronics.getStartingPrice());
        assertEquals(ItemCategory.ELECTRONICS, electronics.getCategory());
        assertEquals("S1", electronics.getSellerId());
        assertEquals(List.of("IMG1", "IMG2"), electronics.getImages());
        assertEquals("Sony", electronics.getBrand());
        assertEquals("12 months", electronics.getWarrantyPeriod());
    }

    @Test
    void fromResultSetFallsBackToCategoryWhenTypeIsUnknown() throws Exception {
        assertInstanceOf(Vehicle.class, ItemMapper.fromResultSet(itemRow("Unknown", "VEHICLE"), List.of()));
        assertInstanceOf(Fashion.class, ItemMapper.fromResultSet(itemRow("", "FASHION"), List.of()));
        assertInstanceOf(Jewelry.class, ItemMapper.fromResultSet(itemRow(null, "JEWELRY"), List.of()));
        assertInstanceOf(Art.class, ItemMapper.fromResultSet(itemRow("Other", "ART"), List.of()));
        assertInstanceOf(OtherItem.class, ItemMapper.fromResultSet(itemRow("Other", null), List.of()));
    }

    @Test
    void resolveTypeReturnsSimpleClassNameOrOtherItemForNull() {
        assertEquals("Electronics", ItemMapper.resolveType(new Electronics()));
        assertEquals("Vehicle", ItemMapper.resolveType(new Vehicle()));
        assertEquals("Fashion", ItemMapper.resolveType(new Fashion()));
        assertEquals("Jewelry", ItemMapper.resolveType(new Jewelry()));
        assertEquals("Art", ItemMapper.resolveType(new Art()));
        assertEquals("OtherItem", ItemMapper.resolveType(new OtherItem()));
        assertEquals("OtherItem", ItemMapper.resolveType(null));
    }

    private ResultSet itemRow(String type, String category) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("type")).thenReturn(type);
        when(rs.getString("category")).thenReturn(category);
        when(rs.getString("item_id")).thenReturn("I1");
        when(rs.getString("name")).thenReturn("Camera");
        when(rs.getString("description")).thenReturn("Desc");
        when(rs.getDouble("starting_price")).thenReturn(100.0);
        when(rs.getString("seller_id")).thenReturn("S1");
        return rs;
    }
}
