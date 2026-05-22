package client.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class AddBankDialogControllerTest {
    @Test
    void normalizesAccountAndInvokesCallbacks() throws Exception {
        runFx(() -> {
            AddBankDialogController controller = new AddBankDialogController();
            TextField bank = new TextField("  ACB  ");
            TextField account = new TextField(" 123-456 789 ");
            AtomicReference<String> linkedBank = new AtomicReference<>();
            AtomicReference<String> linkedAccount = new AtomicReference<>();
            AtomicReference<Double> linkedBalance = new AtomicReference<>();
            boolean[] closed = {false};

            setField(controller, "bankNameField", bank);
            setField(controller, "accountNumberField", account);
            controller.setOnBankLinkedListener((b, a, balance) -> {
                linkedBank.set(b);
                linkedAccount.set(a);
                linkedBalance.set(balance);
            });
            controller.setOnCloseCallback(() -> closed[0] = true);

            invoke(controller, "handleConfirmBankLink", new ActionEvent(new Object(), null));

            assertEquals("ACB", linkedBank.get());
            assertEquals("123456789", linkedAccount.get());
            assertEquals(10_000_000.0, linkedBalance.get());
            assertTrue(closed[0]);
        });
    }
}
