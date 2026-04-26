package io.auctionsystem.controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class LoginApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource("/io/auctionsystem/logindemo.fxml");

        if (fxmlLocation == null) {
            System.err.println("Lỗi nghiêm trọng: Không tìm thấy file logindemo.fxml để khởi chạy!");
            System.err.println("Hãy kiểm tra lại xem file đã nằm trong src/main/resources/io.auctionsystem/ chưa.");
            System.exit(1);
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);

        stage.setTitle("Hệ thống Đấu giá");
        stage.setScene(scene);
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}