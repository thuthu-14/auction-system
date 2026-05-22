package client.controller;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import server.model.Bid;

import java.util.List;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SellerAuctionBidHistoryControllerTest {
    @Test
    void formatsAndRendersTableRows() throws Exception {
        runFx(() -> {
            SellerAuctionBidHistoryController controller = new SellerAuctionBidHistoryController();
            Label summary = new Label();
            TableView<Bid> table = new TableView<>();
            TableColumn<Bid, String> bidder = new TableColumn<>();
            TableColumn<Bid, String> amount = new TableColumn<>();
            TableColumn<Bid, String> time = new TableColumn<>();
            TableColumn<Bid, String> status = new TableColumn<>();
            table.getColumns().addAll(bidder, amount, time, status);
            setField(controller, "summaryLabel", summary);
            setField(controller, "bidTable", table);
            setField(controller, "colBidder", bidder);
            setField(controller, "colAmount", amount);
            setField(controller, "colTime", time);
            setField(controller, "colStatus", status);
            setField(controller, "auctionId", "A1");

            invoke(controller, "setupTable");
            Bid bid = new Bid("B1", "A1", "u1", "Alice", 1_500_000.0);
            bid.setBidTime(1_700_000_000_000L);
            bid.setStatus("WINNING");
            invoke(controller, "renderBidHistory", List.of(bid));

            assertEquals(1, table.getItems().size());
            assertTrue(summary.getText().contains("1"));
            assertEquals("Alice", bidder.getCellObservableValue(bid).getValue());
            assertTrue(amount.getCellObservableValue(bid).getValue().contains("1"));
            assertNotEquals("--", time.getCellObservableValue(bid).getValue());
            assertEquals("WINNING", status.getCellObservableValue(bid).getValue());

            assertEquals("--", invoke(controller, "safe", " "));
            assertEquals("--", invoke(controller, "formatDate", 0L));
            assertEquals("A1", invoke(controller, "resolveAuctionId", "--"));

            invoke(controller, "renderBidHistory", new Object[]{null});
            assertEquals(0, table.getItems().size());
            assertTrue(summary.getText().contains("0"));

            setField(controller, "auctionId", null);
            controller.loadAuctionBidHistory("--");
            assertTrue(summary.getText().contains("Ch"));

            controller.loadAuctionBidHistory("A2");
            assertFalse(summary.getText().isBlank());
        });
    }
}
