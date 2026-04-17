package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

public class SignupController implements Initializable {

    @FXML
    private TextField Ho;

    @FXML
    private TextField Ten;

    @FXML
    private TextField Email;

    @FXML
    private PasswordField mk;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        String lastName = Ho.getText();
        String firstName = Ten.getText();
        String email = Email.getText();
        String password = mk.getText();

        if (email.isEmpty() || password.isEmpty() || lastName.isEmpty() || firstName.isEmpty()) {
            System.out.println("Vui lòng điền đầy đủ thông tin.");
        } else {
            System.out.println("Đang đăng ký user: " + firstName + " " + lastName);
        }
    }

    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            URL loginLocation = getClass().getResource("/io/auctionsystem/logindemo.fxml");

            if (loginLocation == null) {
                System.err.println("Lỗi: Không tìm thấy logindemo.fxml tại /io.auctionsystem/logindemo.fxml");
                return;
            }

            Parent root = FXMLLoader.load(loginLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Lỗi khi load màn hình Login!");
            e.printStackTrace();
        }
    }
}