package navigation;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class NavigationManager {
    private static NavigationManager instance;
    private Stage mainStage;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    public void navigateTo(String fxmlPath) {
        try {
            if (mainStage == null) {
                System.err.println("Lỗi: Chưa gọi setMainStage() cho NavigationManager.");
                return;
            }

            //getClass().getResource(fxmlPath) sẽ tìm file trong thư mục resources
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            //double currentWidth = mainStage.getWidth();
            //double currentHeight = mainStage.getHeight();
            Scene currentScene = mainStage.getScene();

            Scene scene;
            //Kiểm tra nếu cửa sổ đã có Scene trước đó
            if (currentScene != null) {
                double currentWidth = currentScene.getWidth();
                double currentHeight = currentScene.getHeight();
                scene = new Scene(root, currentWidth, currentHeight);
            } else {
                scene = new Scene(root);
            }

            mainStage.setScene(scene);
            mainStage.setResizable(true);
            mainStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi điều hướng: Không tìm thấy file " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void goToSellerHome() {
        navigateTo("/fxml/SellerHome.fxml");
    }

    public void goToHome() {
        navigateTo("/fxml/Home.fxml");
    }
}
