package client.controller;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;

import static client.controller.ControllerTestSupport.field;
import static client.controller.ControllerTestSupport.invoke;
import static client.controller.ControllerTestSupport.runFx;
import static client.controller.ControllerTestSupport.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileControllerTest {
    @Test
    void setUserDataPopulatesEmailAndRestoresReadableTextColors() throws Exception {
        runFx(() -> {
            ProfileController controller = new ProfileController();
            TextField name = new TextField();
            TextField email = new TextField();
            Label title = new Label("Profile");
            title.setTextFill(Color.WHITE);
            VBox root = new VBox(title, name, email);
            setField(controller, "profileRoot", root);
            setField(controller, "fullNameField", name);
            setField(controller, "emailField", email);

            controller.setUserData(new RegularUser("u1", "Bao Ngoc", "pw", "bn@gmail.com"));
            invoke(controller, "forceProfileTextColors");

            assertEquals("Bao Ngoc", name.getText());
            assertEquals("bn@gmail.com", email.getText());
            assertEquals(Color.web("#1a1a1a"), title.getTextFill());
            assertEquals(1.0, email.getOpacity(), 0.001);
            assertEquals(email, field(controller, "emailField"));
        });
    }
}
