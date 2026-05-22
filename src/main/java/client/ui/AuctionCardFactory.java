package client.ui;

import client.controller.AuctionCardController;
import client.controller.HomeScreenController;
import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import server.model.Auction;

import java.net.URL;

public class AuctionCardFactory {
    private static final String CARD_FXML = "/fxml/BidderView/AuctionCard.fxml";

    public VBox create(Auction auction, HomeScreenController homeScreenController, String context) {
        try {
            URL resource = getClass().getResource(CARD_FXML);
            if (resource == null) {
                ClientExceptionHandler.handle(ClientErrorType.NAVIGATION,
                        "Load auction card in " + context,
                        new IllegalStateException(CARD_FXML));
                return null;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent cardNode = loader.load();
            AuctionCardController cardController = loader.getController();
            if (cardController != null) {
                if (homeScreenController != null) {
                    cardController.setHomeScreenController(homeScreenController);
                }
                cardController.setAuctionData(auction);
            }

            if (cardNode instanceof VBox box) {
                return box;
            }
            VBox wrapper = new VBox(cardNode);
            wrapper.setPrefWidth(280);
            return wrapper;
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.NAVIGATION, "Load auction card in " + context, e);
            return null;
        }
    }
}
