package io.auctionsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/io/auctionsystem/Home.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);

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