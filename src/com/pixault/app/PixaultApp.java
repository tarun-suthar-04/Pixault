package com.pixault.app;

import com.pixault.crypto.CryptoEngine;
import com.pixault.db.DatabaseManager;
import com.pixault.stego.StegoEngine;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Pixault - Secure Digital Image Steganography and Encrypted Data Vault
 * * Architecture:
 * 1. Pixault (Main/UI)
 * 2. DatabaseManager (Auth/Logs)
 * 3. CryptoEngine (AES)
 * 4. StegoEngine (LSB)
 */
public class PixaultApp extends Application {

    // Global User State
    private static int currentUserId = -1;
    private static String currentUsername = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pixault - Secure Steganography Vault");

        // Initialize Database
        if (!DatabaseManager.testConnection()) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Cannot connect to MySQL. Check configuration.");
            return;
        }

        showLoginScreen(primaryStage);
    }

    // ==========================================
    // MODULE 1: UI & NAVIGATION (JavaFX)
    // ==========================================

    private void showLoginScreen(Stage stage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #f4f4f4;");

        Label title = new Label("Pixault Login");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        TextField userField = new TextField();
        userField.setPromptText("Username");
        userField.setMaxWidth(300);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setMaxWidth(300);

        Button loginBtn = new Button("Login");
        loginBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        loginBtn.setMinWidth(100);

        Button registerBtn = new Button("Register New Account");
        registerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3;");

        loginBtn.setOnAction(e -> {
            String u = userField.getText();
            String p = passField.getText();
            int userId = DatabaseManager.authenticate(u, p);
            if (userId != -1) {
                currentUserId = userId;
                currentUsername = u;
                DatabaseManager.logAction(currentUserId, "LOGIN_SUCCESS");
                showDashboard(stage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid credentials.");
            }
        });

        registerBtn.setOnAction(e -> showRegisterScreen(stage));

        root.getChildren().addAll(title, userField, passField, loginBtn, registerBtn);
        stage.setScene(new Scene(root, 400, 500));
        stage.show();
    }

    private void showRegisterScreen(Stage stage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        Label title = new Label("Create Account");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        TextField userField = new TextField();
        userField.setPromptText("Username");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");

        Button regBtn = new Button("Register");
        Button backBtn = new Button("Back");

        regBtn.setOnAction(e -> {
            if (DatabaseManager.registerUser(userField.getText(), passField.getText())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account created. Please login.");
                showLoginScreen(stage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Username already exists or database error.");
            }
        });

        backBtn.setOnAction(e -> showLoginScreen(stage));

        root.getChildren().addAll(title, userField, passField, regBtn, backBtn);
        stage.setScene(new Scene(root, 400, 500));
    }

    private void showDashboard(Stage stage) {
        BorderPane root = new BorderPane();

        // Header
        HBox header = new HBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #333;");
        Label welcome = new Label("Welcome, " + currentUsername);
        welcome.setTextFill(Color.WHITE);
        welcome.setFont(Font.font(16));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> {
            DatabaseManager.logAction(currentUserId, "LOGOUT");
            currentUserId = -1;
            currentUsername = "";
            showLoginScreen(stage);
        });
        header.getChildren().addAll(welcome, spacer, logoutBtn);
        root.setTop(header);

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab hideTab = new Tab("Hide Data", createHideTab(stage));
        Tab extractTab = new Tab("Extract Data", createExtractTab(stage));

        tabPane.getTabs().addAll(hideTab, extractTab);
        root.setCenter(tabPane);

        stage.setScene(new Scene(root, 900, 700));
        stage.centerOnScreen();
    }

    private VBox createHideTab(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Image Selection
        HBox imgBox = new HBox(10);
        Button selectImgBtn = new Button("Select Cover Image (PNG/BMP)");
        Label imgPathLabel = new Label("No image selected");
        ImageView preview = new ImageView();
        preview.setFitHeight(200);
        preview.setPreserveRatio(true);
        imgBox.getChildren().addAll(selectImgBtn, imgPathLabel);

        // Data Input (Text or File)
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Enter secret text here...");
        messageArea.setPrefRowCount(5);

        CheckBox useFileCheck = new CheckBox("Embed File instead of Text");
        Button selectFileBtn = new Button("Select File to Hide");
        Label filePathLabel = new Label("");
        selectFileBtn.setDisable(true);

        useFileCheck.selectedProperty().addListener((obs, old, val) -> {
            messageArea.setDisable(val);
            selectFileBtn.setDisable(!val);
        });

        // Security
        PasswordField encryptPass = new PasswordField();
        encryptPass.setPromptText("Encryption Password (Required to extract later)");

        Button actionBtn = new Button("Encrypt & Hide Data");
        actionBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");

        // State variables (effectively final wrappers)
        final File[] selectedImageFile = {null};
        final File[] selectedDataFile = {null};

        selectImgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.bmp"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                selectedImageFile[0] = f;
                imgPathLabel.setText(f.getName());
                preview.setImage(new Image(f.toURI().toString()));
            }
        });

        selectFileBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                selectedDataFile[0] = f;
                filePathLabel.setText(f.getName());
            }
        });

        actionBtn.setOnAction(e -> {
            if (selectedImageFile[0] == null || encryptPass.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Info", "Select an image and set a password.");
                return;
            }

            try {
                byte[] dataToHide;
                if (useFileCheck.isSelected()) {
                    if (selectedDataFile[0] == null) return;
                    dataToHide = java.nio.file.Files.readAllBytes(selectedDataFile[0].toPath());
                } else {
                    if (messageArea.getText().isEmpty()) return;
                    dataToHide = messageArea.getText().getBytes(StandardCharsets.UTF_8);
                }

                // 1. Encrypt
                byte[] encryptedData = CryptoEngine.encrypt(dataToHide, encryptPass.getText());

                // 2. Steganography
                BufferedImage original = ImageIO.read(selectedImageFile[0]);
                BufferedImage stegoImg = StegoEngine.embed(original, encryptedData);

                if (stegoImg == null) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Image too small to hold this data.");
                    return;
                }

                // 3. Save
                FileChooser fc = new FileChooser();
                fc.setInitialFileName("encrypted_image.png");
                File dest = fc.showSaveDialog(stage);
                if (dest != null) {
                    ImageIO.write(stegoImg, "png", dest);
                    DatabaseManager.logAction(currentUserId, "ENCRYPT_HIDE");
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Data hidden and saved successfully!");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        layout.getChildren().addAll(
            new Label("1. Select Cover Image"), imgBox, preview,
            new Label("2. Data to Hide"), useFileCheck, selectFileBtn, filePathLabel, messageArea,
            new Label("3. Security"), encryptPass, actionBtn
        );
        return layout;
    }

    private VBox createExtractTab(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Button selectImgBtn = new Button("Select Stego-Image");
        Label imgLabel = new Label("No image selected");
        ImageView preview = new ImageView();
        preview.setFitHeight(200);
        preview.setPreserveRatio(true);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Decryption Password");

        Button extractBtn = new Button("Extract & Decrypt");
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Decrypted text will appear here...");

        Button saveFileBtn = new Button("Save Extracted Data as File");
        saveFileBtn.setDisable(true);

        final File[] selectedFile = {null};
        final byte[][] extractedBytes = {null};

        selectImgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.bmp"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                selectedFile[0] = f;
                imgLabel.setText(f.getName());
                preview.setImage(new Image(f.toURI().toString()));
            }
        });

        extractBtn.setOnAction(e -> {
            if (selectedFile[0] == null || passField.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Select image and enter password.");
                return;
            }

            try {
                BufferedImage img = ImageIO.read(selectedFile[0]);

                // 1. Extract Raw Encrypted Bytes
                byte[] rawEncrypted = StegoEngine.extract(img);
                if (rawEncrypted == null || rawEncrypted.length == 0) {
                    showAlert(Alert.AlertType.ERROR, "Error", "No hidden data found or image corrupted.");
                    return;
                }

                // 2. Decrypt
                extractedBytes[0] = CryptoEngine.decrypt(rawEncrypted, passField.getText());

                // Attempt to show as string, enable save
                String resultText = new String(extractedBytes[0], StandardCharsets.UTF_8);
                // Heuristic: If it looks like text, show it. Otherwise show binary message.
                if (isText(extractedBytes[0])) {
                    outputArea.setText(resultText);
                } else {
                    outputArea.setText("[Binary Data Extracted - Please Save as File]");
                }

                saveFileBtn.setDisable(false);
                DatabaseManager.logAction(currentUserId, "DECRYPT_EXTRACT");

            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Decryption Failed", "Wrong password or corrupted data.");
            }
        });

        saveFileBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File dest = fc.showSaveDialog(stage);
            if (dest != null && extractedBytes[0] != null) {
                try {
                    java.nio.file.Files.write(dest.toPath(), extractedBytes[0]);
                    showAlert(Alert.AlertType.INFORMATION, "Saved", "File saved successfully.");
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not save file.");
                }
            }
        });

        layout.getChildren().addAll(
            new Label("1. Select Image"), selectImgBtn, imgLabel, preview,
            new Label("2. Decryption"), passField, extractBtn,
            new Label("3. Output"), outputArea, saveFileBtn
        );
        return layout;
    }

    private boolean isText(byte[] data) {
        // Simple heuristic to check if data is likely text
        for (int i = 0; i < Math.min(data.length, 50); i++) {
            if (data[i] < 9 && data[i] != 0) return false; // Control chars
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
