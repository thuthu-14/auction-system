package navigation;

import client.controller.DashboardController;
import client.controller.SellerHomeController;
import client.controller.WalletController;
import client.network.ClientSocket;
import client.util.ResponsiveSceneUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.KeyCombination;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.model.User;

import java.io.IOException;

public class NavigationManager {

    private static NavigationManager instance;
    private Stage mainStage;

    private User currentUser;
    private ClientSocket clientSocket;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    // ===================== SETUP =====================
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setClientSocket(ClientSocket socket) {
        this.clientSocket = socket;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public ClientSocket getClientSocket() {
        return clientSocket;
    }

    // ===================== CORE NAV =====================
    /**
     * Hàm điều hướng chính, tự động truyền dữ liệu User và Socket cho Controller tương ứng
     */
    public void navigateTo(String fxmlPath) {
        try {
            if (mainStage == null) {
                System.err.println("❌ NavigationManager: mainStage is NULL!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();

            // 🔥 FIX CỐT LÕI: Truyền dữ liệu cho Dashboard (Trang chủ) để load hàng
            if (controller instanceof DashboardController dc) {
                dc.setUserData(currentUser, clientSocket);
                System.out.println("✓ Navigation: Data passed to DashboardController");
            }

            // 🔥 FIX: Truyền dữ liệu cho Wallet (Ví tiền)
            else if (controller instanceof WalletController wc) {
                wc.setUserData(currentUser, clientSocket);
                System.out.println("✓ Navigation: Data passed to WalletController");
            }

            // 🔥 FIX: Truyền dữ liệu cho Seller Home (Trang người bán)
            else if (controller instanceof SellerHomeController shc) {
                shc.setUserData(currentUser, clientSocket);
                System.out.println("✓ Navigation: Data passed to SellerHomeController");
            }

            Scene currentScene = mainStage.getScene();
            Scene scene = (currentScene != null)
                    ? ResponsiveSceneUtil.createScaledScene(root, currentScene.getWidth(), currentScene.getHeight())
                    : ResponsiveSceneUtil.createScaledScene(root);

            mainStage.setScene(scene);
            mainStage.setMaximized(true);
            mainStage.setFullScreenExitHint("");
            mainStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            mainStage.setFullScreen(true);
            mainStage.show();

        } catch (IOException e) {
            System.err.println("❌ Navigation Error [" + fxmlPath + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== CÁC HÀM ĐIỀU HƯỚNG NHANH =====================

    /**
     * Quay về trang chủ đấu giá (Danh sách sản phẩm)
     */
    public void goToHome() {
        // Đảm bảo đường dẫn này khớp với tên file FXML của bạn
        navigateTo("/fxml/home.fxml");
    }

    /**
     * Chuyển sang giao diện quản lý của người bán
     */
    public void goToSellerHome() {
        navigateTo("/fxml/SellerHome.fxml");
    }

    /**
     * Chuyển sang giao diện ví tiền
     */
    public void goToWallet() {
        navigateTo("/fxml/Wallet.fxml");
    }
}
