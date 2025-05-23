package org.apiary.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apiary.model.Beekeeper;
import org.apiary.model.Client;
import org.apiary.model.User;
import org.apiary.service.ServiceFactory;
import org.apiary.service.interfaces.UserService;
import org.apiary.utils.StringUtils;
import org.apiary.utils.ValidationUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private VBox loginForm;
    @FXML private VBox signupForm;

    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;

    @FXML private TextField signupUsername;
    @FXML private PasswordField signupPassword;
    @FXML private PasswordField signupConfirmPassword;
    @FXML private TextField signupFullName;
    @FXML private TextField signupEmail;
    @FXML private TextField signupPhone;
    @FXML private TextArea signupAddress;
    @FXML private RadioButton clientRadioButton;
    @FXML private RadioButton beekeeperRadioButton;
    @FXML private Label yearsExperienceLabel;
    @FXML private Spinner<Integer> yearsExperienceSpinner;

    @FXML private Button loginButton;
    @FXML private Button signupButton;

    private UserService userService;

    @FXML
    private void initialize() {
        // Initialize the service
        userService = ServiceFactory.getUserService();

        // Set up button event handlers
        loginButton.setOnAction(event -> handleLogin());
        signupButton.setOnAction(event -> handleSignup());

        // Hide/show beekeeper fields based on selection
        beekeeperRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            yearsExperienceLabel.setVisible(newValue);
            yearsExperienceSpinner.setVisible(newValue);
        });

        // Default to Client radio button selected
        clientRadioButton.setSelected(true);
    }

    @FXML
    private void toggleForms() {
        // Toggle between login and signup forms
        boolean loginVisible = loginForm.isVisible();
        loginForm.setVisible(!loginVisible);
        loginForm.setManaged(!loginVisible);
        signupForm.setVisible(loginVisible);
        signupForm.setManaged(loginVisible);

        // Clear input fields
        if (loginVisible) {
            clearSignupFields();
        } else {
            clearLoginFields();
        }
    }

    private void clearLoginFields() {
        loginUsername.clear();
        loginPassword.clear();
    }

    private void clearSignupFields() {
        signupUsername.clear();
        signupPassword.clear();
        signupConfirmPassword.clear();
        signupFullName.clear();
        signupEmail.clear();
        signupPhone.clear();
        signupAddress.clear();
        clientRadioButton.setSelected(true);
        yearsExperienceSpinner.getValueFactory().setValue(0);
    }

    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        // Validate input
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            showAlert(Alert.AlertType.ERROR, "Login Error",
                    "Please enter both username and password.");
            return;
        }

        try {
            // Attempt authentication
            if (userService.authenticate(username, password)) {
                // Get the user
                Optional<User> userOpt = userService.findByUsername(username);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // Determine type and open appropriate dashboard
                    if (user instanceof Beekeeper) {
                        openBeekeeperDashboard((Beekeeper) user);
                    } else if (user instanceof Client) {
                        openClientDashboard((Client) user);
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                        "Invalid username or password. Please try again.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during login", e);
            showAlert(Alert.AlertType.ERROR, "Login Error",
                    "An error occurred during login: " + e.getMessage());
        }
    }

    private void handleSignup() {
        // Get form data
        String username = signupUsername.getText().trim();
        String password = signupPassword.getText().trim();
        String confirmPassword = signupConfirmPassword.getText().trim();
        String fullName = signupFullName.getText().trim();
        String email = signupEmail.getText().trim();
        String phone = signupPhone.getText().trim();
        String address = signupAddress.getText().trim();
        boolean isBeekeeper = beekeeperRadioButton.isSelected();
        Integer yearsExperience = isBeekeeper ? yearsExperienceSpinner.getValue() : null;

        // Validate input
        if (StringUtils.isBlank(username)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Username is required.");
            signupUsername.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error",
                    "Username must be 3-20 characters and contain only letters, numbers, underscores, and hyphens.");
            signupUsername.requestFocus();
            return;
        }

        if (StringUtils.isBlank(password)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Password is required.");
            signupPassword.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error",
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.");
            signupPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error",
                    "Passwords do not match. Please try again.");
            signupConfirmPassword.requestFocus();
            return;
        }

        if (StringUtils.isBlank(fullName)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Full name is required.");
            signupFullName.requestFocus();
            return;
        }

        if (!StringUtils.isBlank(email) && !ValidationUtils.isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Please enter a valid email address.");
            signupEmail.requestFocus();
            return;
        }

        if (!StringUtils.isBlank(phone) && !ValidationUtils.isValidPhone(phone)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Please enter a valid phone number.");
            signupPhone.requestFocus();
            return;
        }

        try {
            // Register user based on account type
            if (isBeekeeper) {
                Beekeeper beekeeper = userService.registerBeekeeper(username, password, phone, address, yearsExperience);

                if (beekeeper != null) {
                    showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                            "Beekeeper account created successfully! You can now log in.");
                    toggleForms(); // Switch back to login form
                }
            } else {
                Client client = userService.registerClient(username, password, fullName, email, address, phone);

                if (client != null) {
                    showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                            "Client account created successfully! You can now log in.");
                    toggleForms(); // Switch back to login form
                }
            }
        } catch (IllegalArgumentException e) {
            // Validation errors from service
            showAlert(Alert.AlertType.ERROR, "Registration Error", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during registration", e);
            showAlert(Alert.AlertType.ERROR, "Registration Error",
                    "An error occurred during registration: " + e.getMessage());
        }
    }

    private void openClientDashboard(Client client) {
        try {
            // Load the client dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/clientDashboard.fxml"));
            Parent root = loader.load();

            // Get controller and set client
            ClientDashboardController controller = loader.getController();
            controller.setClient(client);

            // Show dashboard
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Dashboard - Apiary Management System");
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening client dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open client dashboard: " + e.getMessage());
        }
    }

    private void openBeekeeperDashboard(Beekeeper beekeeper) {
        try {
            // Load the beekeeper dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/beekeeperDashboard.fxml"));
            Parent root = loader.load();

            // Get controller and set beekeeper
            BeekeeperDashboardController controller = loader.getController();
            controller.setBeekeeper(beekeeper);

            // Show dashboard
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Beekeeper Dashboard - Apiary Management System");
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening beekeeper dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open beekeeper dashboard: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}