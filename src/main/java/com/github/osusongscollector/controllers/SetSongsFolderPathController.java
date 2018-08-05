package com.github.osusongscollector.controllers;

import com.github.osusongscollector.application.Constants;
import com.github.osusongscollector.application.ViewLoader;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetSongsFolderPathController {
	@FXML private TextField chosenPathTextField;
	@FXML private Button choosePathButton;
	@FXML private Label instructionLabel;
	@FXML private Button continueButton;
	
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private String fullPathToOsuDb;
	private String pathToSongsFolder;
	private HostServices hostServices;
	
	public void setHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}
	
	// choosePathButton pressed
	@FXML
	private void promptToChoosePath(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(this.choosePathButton.getScene().getWindow());
          
        if (selectedFile == null) {
        	this.chosenPathTextField.setText("No file selected");
        	if (!this.continueButton.isDisable()) {
        		this.continueButton.setDisable(true);
        	}
        }
        else {
        	if (fileIsValid(selectedFile.getName())) {
        		this.pathToSongsFolder = Paths.get(selectedFile.getParent(), Constants.SONGS_FOLDER).toString();
        		this.fullPathToOsuDb = Paths.get(selectedFile.getParent(), Constants.OSU_DB_NAME).toString();
        		this.chosenPathTextField.setText(selectedFile.getAbsolutePath());
        		this.continueButton.setDisable(false);
        		this.continueButton.requestFocus();
        	}
        	else {
        		this.chosenPathTextField.setText("Invalid file");
        	}
        }
	}
	
	// continueButton pressed
	@FXML
	private void setupDatabaseNewScene(ActionEvent event) {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/com/github/osusongscollector/fxml/LoadingDialogParentView.fxml"));
		try {
			logger.log(Level.INFO, "Loading LoadAndCreateDatabaseView");
			LoadAndCreateDatabaseController ctr = new LoadAndCreateDatabaseController();
			loader.setController(ctr);
			BorderPane root = loader.load();
			Scene scene = new Scene(root);
			Stage currentStage = (Stage) this.continueButton.getScene().getWindow();
			ctr.setHostServices(this.hostServices);
			ctr.initDataAndStart(currentStage, this.fullPathToOsuDb, this.pathToSongsFolder);
			currentStage.setScene(scene);
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to load LoadingDialogParentView", e);
			Alert alert = new Alert(AlertType.ERROR, "Failed to load setup screen", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
	}
	
	private boolean fileIsValid(String filename) {
		return filename.equals(Constants.OSU_EXE);
	}
}
