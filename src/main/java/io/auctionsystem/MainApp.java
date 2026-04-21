package io.auctionsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Font.loadFont(getClass().getResourceAsStream("/io/auctionsystem/css/DarleySans-Regular.otf"), 14);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/io/auctionsystem/BecomeSeller.fxml")
        );

        Parent root = loader.load();
        Scene scene = new Scene(root);

        String css = getClass().getResource("/io/auctionsystem/css/style.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("BubblyBid");
        stage.setScene(scene);
        stage.show();
        stage.setResizable(true);
        stage.setMaximized(true);

        stage.setMinWidth(900.0);
        stage.setMinHeight(600.0);
    }

    public static void main(String[] args) {
        launch();
    }
}