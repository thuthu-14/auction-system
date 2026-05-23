package util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.model.OtherItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadJsonRoundTripsObject() throws Exception {
        Path file = tempDir.resolve("items").resolve("item.json");
        OtherItem item = new OtherItem("IT1", "Item", "Description", 10.0, "seller", List.of("a.jpg"));

        JsonUtil.saveToJson(file.toString(), item);
        OtherItem loaded = JsonUtil.loadFromJson(file.toString(), OtherItem.class);

        assertTrue(JsonUtil.fileExists(file.toString()));
        assertNotNull(loaded);
        assertEquals("IT1", loaded.getItemId());
        assertEquals("Item", loaded.getName());
    }

    @Test
    void loadFromJsonReturnsNullForMissingOrEmptyFile() throws Exception {
        Path missing = tempDir.resolve("missing.json");
        Path empty = tempDir.resolve("empty.json");
        Files.writeString(empty, "   ");

        assertNull(JsonUtil.loadFromJson(missing.toString(), OtherItem.class));
        assertNull(JsonUtil.loadFromJson(empty.toString(), OtherItem.class));
    }

    @Test
    void loadListFromJsonHandlesMissingEmptyAndPopulatedFiles() throws Exception {
        Path missing = tempDir.resolve("missing-list.json");
        Path emptyList = tempDir.resolve("empty-list.json");
        Path populated = tempDir.resolve("populated-list.json");

        Files.writeString(emptyList, "[]");
        Files.writeString(populated, "[{\"itemId\":\"IT1\",\"name\":\"Item\"}]");

        assertTrue(JsonUtil.loadListFromJson(missing.toString(), OtherItem.class).isEmpty());
        assertTrue(JsonUtil.loadListFromJson(emptyList.toString(), OtherItem.class).isEmpty());
        assertEquals(1, JsonUtil.loadListFromJson(populated.toString(), OtherItem.class).size());
    }

    @Test
    void loadMapFromJsonHandlesMissingEmptyAndPopulatedFiles() throws Exception {
        Path missing = tempDir.resolve("missing-map.json");
        Path empty = tempDir.resolve("empty-map.json");
        Path populated = tempDir.resolve("populated-map.json");

        Files.writeString(empty, "   ");
        Files.writeString(populated, "{\"a\":1,\"b\":2}");

        assertTrue(JsonUtil.loadMapFromJson(missing.toString(), String.class, Integer.class).isEmpty());
        assertTrue(JsonUtil.loadMapFromJson(empty.toString(), String.class, Integer.class).isEmpty());
        Map<String, Integer> loaded = JsonUtil.loadMapFromJson(populated.toString(), String.class, Integer.class);
        assertEquals(2, loaded.size());
        assertEquals(1, loaded.get("a"));
    }

    @Test
    void createFileIfNotExistsCreatesJsonArrayAndKeepsExistingContent() throws IOException {
        Path file = tempDir.resolve("nested").resolve("data.json");

        JsonUtil.createFileIfNotExists(file.toString());
        assertEquals("[]", Files.readString(file));

        Files.writeString(file, "[1]");
        JsonUtil.createFileIfNotExists(file.toString());
        assertEquals("[1]", Files.readString(file));
    }
}
