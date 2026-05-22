package client.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSearchControllerTest {
    @Test
    void searchNormalizesKeywordWithoutRequiringInjectedFxmlFields() throws Exception {
        ProductSearchController controller = new ProductSearchController();

        controller.search(null);
        assertEquals("", keyword(controller));

        controller.search("   ");
        assertEquals("", keyword(controller));

        controller.search("  clock  ");
        assertEquals("clock", keyword(controller));
    }

    private static String keyword(ProductSearchController controller) throws Exception {
        Field field = ProductSearchController.class.getDeclaredField("keyword");
        field.setAccessible(true);
        return (String) field.get(controller);
    }
}
