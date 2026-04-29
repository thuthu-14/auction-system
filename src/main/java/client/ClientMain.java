package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import navigation.NavigationManager;


public class ClientMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        //load font
        Font.loadFont(getClass().getResourceAsStream("/CSS/DarleySans-Regular.otf"), 14);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/SellerHome.fxml")
        );
        Parent root = loader.load();
        Scene scene = new Scene(root);
        String css = getClass().getResource("/CSS/style.css").toExternalForm();
        scene.getStylesheets().add(css);//

        stage.setTitle("BubblyBid");
        stage.setResizable(true);
        stage.setMinWidth(900.0);
        stage.setMinHeight(600.0);

        NavigationManager.getInstance().setMainStage(stage);
        NavigationManager.getInstance().goToHome();

        stage.setScene(scene);
        stage.show();
        stage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch();
    }
}