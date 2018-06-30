package controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;

public class CopySongsController {
	@FXML private ProgressBar copyProgressBar;
	@FXML private TextArea copyDetailsTextArea;
	@FXML private Button cancelButton;
	
	Task<Void> copySongsTask;
	
	public void initDataAndStart(Task<Void> copySongsTask) {
		this.copySongsTask = copySongsTask;
		this.startCopying();
	}
	
	private void startCopying() {
		this.copyProgressBar.progressProperty().bind(this.copySongsTask.progressProperty());
		copySongsTask.messageProperty().addListener((obs, oldValue, newValue) -> {
			this.copyDetailsTextArea.appendText(newValue + "\n");
		});
		
		copySongsTask.setOnCancelled(e -> {
			this.copyDetailsTextArea.appendText("Cancelling. Waiting for the last song to finish...");
		});
		copySongsTask.setOnSucceeded(e -> {
			this.copyDetailsTextArea.appendText("All songs are successfully copied!");
		});
		copySongsTask.setOnFailed(e -> {
			copySongsTask.getException().printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Error occured while copying songs: " + copySongsTask.getException().getMessage(), ButtonType.OK);
			alert.showAndWait();
		});
		new Thread(copySongsTask).start();
	}
	
	// cancelButton
	@FXML private void stopCopying(ActionEvent event) {
		Alert alert = new Alert(AlertType.WARNING, "Are you sure you want to cancel copying?", ButtonType.YES, ButtonType.NO);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				this.copySongsTask.cancel();
			}
		});
	}
}
