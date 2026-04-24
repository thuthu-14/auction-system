package util;

import java.io.*;
import java.nio.file.*;

public class SerializationUtil {

    public static <T> void serialize(String filePath, T data) throws IOException {
        Files.createDirectories(Paths.get(filePath).getParent());

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath))) {
            oos.writeObject(data);
            LoggerUtil.info("Serialized to: " + filePath);
        }
    }


    public static <T> T deserialize(String filePath, Class<T> clazz)
            throws IOException, ClassNotFoundException {
        if (!Files.exists(Paths.get(filePath))) {
            LoggerUtil.warn("Serialized file not found: " + filePath);
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filePath))) {
            LoggerUtil.info("Deserialized from: " + filePath);
            return (T) ois.readObject();
        }
    }

    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public static void createFileIfNotExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            LoggerUtil.info("Created serialization file: " + filePath);
        }
    }
}
