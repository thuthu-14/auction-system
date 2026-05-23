package client.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class BecomeSellerControllerTest {
    @Test
    void populatesEmailAndTogglesPasswordField() throws Exception {
        runFx(() -> {
            BecomeSellerController controller = new BecomeSellerController();
            TextField email = new TextField();
            PasswordField hiddenPassword = new PasswordField();
            TextField visiblePassword = new TextField();
            Button toggle = new Button("Hien");
            hiddenPassword.setText("secret");

            setField(controller, "nameField", new TextField("Shop"));
            setField(controller, "emailField", email);
            setField(controller, "phoneField", new TextField("0123456789"));
            setField(controller, "addressField", new TextField("Ha Noi"));
            setField(controller, "passwordField", hiddenPassword);
            setField(controller, "passwordTextField", visiblePassword);
            setField(controller, "btnTogglePassword", toggle);

            controller.setCurrentUser(new RegularUser("u1", "alice", "pw", "alice@example.com"));
            assertEquals("alice@example.com", email.getText());
            assertFalse(email.isEditable());

            controller.togglePasswordVisibility(new ActionEvent());
            assertTrue(visiblePassword.isVisible());
            assertFalse(hiddenPassword.isVisible());
            assertEquals("secret", visiblePassword.getText());

            controller.togglePasswordVisibility(new ActionEvent());
            assertFalse(visiblePassword.isVisible());
            assertTrue(hiddenPassword.isVisible());
            assertEquals("secret", hiddenPassword.getText());

            assertNotNull(invoke(controller, "validateInput"));
        });
    }
}
