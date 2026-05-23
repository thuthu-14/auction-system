package client.controller;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AddAuctionProductControllerTest {
    @Test
    void initializeLoadsCategoryLabelsAndTimeOptions() throws Exception {
        runFx(() -> {
            AddAuctionProductController controller = baseController();

            controller.initialize(null, null);

            ComboBox<String> category = combo(controller, "categoryComboBox");
            assertFalse(category.getItems().isEmpty());
            @SuppressWarnings("unchecked")
            List<String> timeOptions = (List<String>) field(controller, "timeOptions");
            assertEquals(48, timeOptions.size());
            assertEquals("00:00", timeOptions.get(0));
            assertEquals("23:30", timeOptions.get(47));
        });
    }

    @Test
    void setupTimeOptionsPopulatesDatePickersAndComboBoxes() throws Exception {
        runFx(() -> {
            AddAuctionProductController controller = baseController();
            controller.initialize(null, null);
            VBox form = new VBox();
            DatePicker startDate = new DatePicker();
            startDate.setId("startDate");
            DatePicker endDate = new DatePicker();
            endDate.setId("endDate");
            ComboBox<String> startHour = new ComboBox<>();
            startHour.setId("startHour");
            ComboBox<String> endHour = new ComboBox<>();
            endHour.setId("endHour");
            form.getChildren().addAll(startDate, endDate, startHour, endHour);
            Object config = categoryFormConfig("Any", "/missing.fxml", "#startDate", "#startHour", "#endDate", "#endHour", "#startNowCheckbox");

            invoke(controller, "setupTimeOptions", form, config);

            assertNotNull(startDate.getValue());
            assertEquals(startDate.getValue().plusDays(1), endDate.getValue());
            assertEquals(48, startHour.getItems().size());
            assertEquals(startHour.getValue(), endHour.getValue());
        });
    }

    @Test
    void fieldReadersReturnDefaultsAndValuesFromCategoryForm() throws Exception {
        runFx(() -> {
            AddAuctionProductController controller = baseController();
            VBox pane = (VBox) field(controller, "categoryFormPane");

            assertEquals("default", invoke(controller, "getFieldValue", "#brand", "default"));
            assertNull(invoke(controller, "getDateValue", "#date"));
            assertNull(invoke(controller, "getComboValue", "#hour"));

            VBox form = new VBox();
            TextField brand = new TextField("Canon");
            brand.setId("brand");
            DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
            date.setId("date");
            ComboBox<String> hour = new ComboBox<>();
            hour.setId("hour");
            hour.getItems().add("10:30");
            hour.setValue("10:30");
            form.getChildren().addAll(brand, date, hour);
            pane.getChildren().add(form);

            assertEquals("Canon", invoke(controller, "getFieldValue", "#brand", "default"));
            assertEquals(date.getValue(), invoke(controller, "getDateValue", "#date"));
            assertEquals("10:30", invoke(controller, "getComboValue", "#hour"));
            assertSame(hour, invoke(controller, "lookupCategoryNode", "#hour"));
        });
    }

    @Test
    void clearFormResetsFieldsImagesAndPlaceholder() throws Exception {
        runFx(() -> {
            AddAuctionProductController controller = baseController();
            TextField name = textField(controller, "productNameField");
            TextArea description = (TextArea) field(controller, "descriptionArea");
            ComboBox<String> category = combo(controller, "categoryComboBox");
            HBox previews = (HBox) field(controller, "imagePreviewContainer");
            VBox pane = (VBox) field(controller, "categoryFormPane");
            ScrollPane scrollPane = (ScrollPane) field(controller, "formScrollPane");
            name.setText("Camera");
            description.setText("desc");
            category.getItems().add("Electronics");
            category.getSelectionModel().selectFirst();
            previews.getChildren().add(new Label("image"));
            pane.getChildren().add(new Label("form"));
            setField(controller, "currentCategory", "Electronics");
            setField(controller, "selectedImagePaths", new ArrayList<>(List.of("a.png")));

            invoke(controller, "clearForm");

            assertEquals("", name.getText());
            assertEquals("", description.getText());
            assertNull(category.getValue());
            assertNull(field(controller, "currentCategory"));
            assertTrue(previews.getChildren().isEmpty());
            assertTrue(((List<?>) field(controller, "selectedImagePaths")).isEmpty());
            assertEquals(1, pane.getChildren().size());
            assertTrue(pane.getChildren().get(0) instanceof VBox);
            assertEquals(0.0, scrollPane.getVvalue());
        });
    }

    @Test
    void calendarStylesheetIsAppliedToParentAndDatePickers() throws Exception {
        runFx(() -> {
            AddAuctionProductController controller = baseController();
            String css = (String) invoke(controller, "getCalendarStylesheet");
            assertNotNull(css);

            VBox form = new VBox();
            invoke(controller, "applyCalendarStylesheet", form);
            assertTrue(form.getStylesheets().contains(css));

            DatePicker picker = new DatePicker();
            invoke(controller, "applyCalendarStyles", new Object[]{new DatePicker[]{picker}});
            assertTrue(picker.getStylesheets().contains(css));
        });
    }

    @Test
    void nextAvailableStartDateTimeIsRoundedToHourOrHalfHour() throws Exception {
        AddAuctionProductController controller = new AddAuctionProductController();
        LocalDateTime next = (LocalDateTime) invoke(controller, "nextAvailableStartDateTime");

        assertEquals(0, next.getSecond());
        assertEquals(0, next.getNano());
        assertTrue(next.getMinute() == 0 || next.getMinute() == 30);
        assertTrue(next.isAfter(LocalDateTime.now()));
    }

    private static AddAuctionProductController baseController() throws Exception {
        AddAuctionProductController controller = new AddAuctionProductController();
        setField(controller, "productNameField", new TextField());
        setField(controller, "categoryComboBox", new ComboBox<String>());
        setField(controller, "descriptionArea", new TextArea());
        setField(controller, "categoryFormPane", new VBox());
        setField(controller, "imagePreviewContainer", new HBox());
        setField(controller, "formScrollPane", new ScrollPane());
        return controller;
    }

    private static Object categoryFormConfig(Object... args) throws Exception {
        Class<?> type = Class.forName("client.service.AddAuctionProductClientService$CategoryFormConfig");
        for (var constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            }
        }
        throw new IllegalArgumentException("CategoryFormConfig constructor not found");
    }

    private static TextField textField(Object target, String name) throws Exception {
        return (TextField) field(target, name);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> combo(Object target, String name) throws Exception {
        return (ComboBox<String>) field(target, name);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, Object[] args) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                return method;
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }

    private static void runFx(ThrowingRunnable action) throws Exception {
        startFx();
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX action timed out");
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    private static synchronized void startFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX startup timed out");
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit is process-wide; another controller test may have started it.
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
