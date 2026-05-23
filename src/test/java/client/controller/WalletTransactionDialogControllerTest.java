package client.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class WalletTransactionDialogControllerTest {
    @Test
    void configuresLabelsAndConfirmsAmount() throws Exception {
        runFx(() -> {
            WalletTransactionDialogController controller = new WalletTransactionDialogController();
            Label title = new Label();
            Label subtitle = new Label();
            Label amount = new Label();
            TextField amountField = new TextField(" 1.000.000 ");
            Button confirm = new Button();
            AtomicReference<String> confirmed = new AtomicReference<>();
            boolean[] closed = {false};

            setField(controller, "titleLabel", title);
            setField(controller, "subtitleLabel", subtitle);
            setField(controller, "amountLabel", amount);
            setField(controller, "amountField", amountField);
            setField(controller, "confirmButton", confirm);
            controller.setOnConfirmListener(confirmed::set);
            controller.setOnCloseCallback(() -> closed[0] = true);

            controller.setTransactionData("DEPOSIT", null);
            assertFalse(title.getText().isBlank());
            assertFalse(confirm.getText().isBlank());

            controller.setTransactionData("WITHDRAW", "2.000.000 VND");
            assertTrue(subtitle.getText().contains("2.000.000"));

            invoke(controller, "handleConfirm", new ActionEvent(new Object(), null));
            assertEquals("1.000.000", confirmed.get());
            assertTrue(closed[0]);
        });
    }
}
