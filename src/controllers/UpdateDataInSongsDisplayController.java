package controllers;

import application.OsuDbParser;
import application.SqliteDatabase;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.util.Duration;


public class UpdateDataInSongsDisplayController extends UpdateDataController {

	private boolean isSongsUpdated = false;
	private boolean isBeatmapsDetailsUpdated = false;
	private SongsDisplayController parentController;
	private Stage currentStage;
	
	// !! must use this instead of inherited initDataAndStart otherwise currentStage and parentContoller will be null
	public void newInitDataAndStart(SongsDisplayController parentController, Stage currentStage, SqliteDatabase songsDb, String fullPathToOsuDb, String pathToSongsFolder) {
		this.parentController = parentController;
		this.currentStage = currentStage;
		this.initDataAndStart(currentStage, songsDb, fullPathToOsuDb, pathToSongsFolder);
	}
	
	@Override protected void setLoadOsuDbTaskOnHandlers(Task<OsuDbParser> loadOsuDbTask) {
		loadOsuDbTask.setOnSucceeded(e -> {
			this.updateSongsDb(loadOsuDbTask.getValue());
		});
		
		loadOsuDbTask.setOnFailed(e -> {
			loadOsuDbTask.getException().printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to load osu!.db", ButtonType.OK);
			alert.showAndWait();
		});
	}
	
	@Override protected void setUpdateSongsDbTaskOnHandlers(Task<Boolean> updateSongsDbTask) {
		updateSongsDbTask.setOnSucceeded(e -> {
			this.isSongsUpdated = updateSongsDbTask.getValue();
			this.updateBeatmapDetails(this.osuDbFolders, this.songsDb);
		});
		
		updateSongsDbTask.setOnFailed(e -> {
			Throwable e1 = updateSongsDbTask.getException();
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to update songs data.", ButtonType.OK);
			alert.showAndWait();
		});
	}
	
	@Override protected void setUpdateBeatmapDetailsTaskOnHandlers(Task<Boolean> updateBeatmapDetailsTask) {
		updateBeatmapDetailsTask.setOnSucceeded(e -> {
			this.instructionLabel.setText("Finish updating.");
			this.isBeatmapsDetailsUpdated = updateBeatmapDetailsTask.getValue();
    		this.promptForUserAction();
		});
		
		updateBeatmapDetailsTask.setOnFailed(e -> {
			Throwable e1 = updateBeatmapDetailsTask.getException();
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to update beatmaps details.", ButtonType.OK);
			alert.showAndWait();
		});
	}
	
	private void promptForUserAction() {
		if (this.isBeatmapsDetailsUpdated || this.isSongsUpdated) {
			String message = "Data is successfully updated. Restarting the program is required to take effect. Restart now?"
					+ " (data may be inconsistent if you proceed without restarting)";
			Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.YES, ButtonType.NO);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					this.currentStage.hide();
					this.parentController.restartProgram(false);
				}
				else {
					this.currentStage.hide();
				}
			});
		}
		else {
			Alert alert = new Alert(AlertType.INFORMATION, "Data is already up-to-date!", ButtonType.OK);
			alert.showAndWait();
			this.currentStage.hide();
		}
	}
}
