package controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

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
			this.copyDetailsTextArea.appendText(copySongsTask.getException().getMessage());
		});
		new Thread(copySongsTask).start();
	}
	
	// cancelButton
	@FXML private void stopCopying(ActionEvent event) {
		this.copySongsTask.cancel();
	}
}
