package com.github.osusongscollector.application;
	
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.github.osusongscollector.controllers.InitScreenController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Main extends Application {
	
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static FileHandler fh = null;

	@Override
	public void start(Stage primaryStage) {
		this.setupLogger();
		logger.log(Level.INFO, "Launching program");
		
        try {
			primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/com/github/osusongscollector/img/osc-logo.png")));
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/com/github/osusongscollector/fxml/InitScreenView.fxml"));
//			BorderPane root = loader.load();
			StackPane root = loader.load();
			Scene scene = new Scene(root);
			primaryStage.initStyle(StageStyle.TRANSPARENT);
			scene.setFill(Color.TRANSPARENT);
			primaryStage.setScene(scene);
			primaryStage.show();
			// show the screen 1st, then start the checking progress
			// and handle subsequent processes in the com.github.osusongscollector.controllers
			InitScreenController initScreenController = loader.<InitScreenController>getController();
			initScreenController.setHostServices(getHostServices());
			initScreenController.startChecking();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to launch the program", e);
			Alert alert = new Alert(AlertType.ERROR, "Failed to launch the program!", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
		
	}
	
	private void setupLogger() {
		try {
			// it's not null if this program is restarted. So if log file already exist, don't create another one
			if (fh == null) {
                String fileLocation = Paths.get(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() , "runtime.log").toString();
				fh = new FileHandler(fileLocation, false);
				fh.setFormatter(new SimpleFormatter());
				logger.addHandler(fh);
				logger.setLevel(Level.INFO);
			}
		}
		catch (SecurityException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
    }
	
	public static void main(String[] args) {
        launch(args);
	}
}
