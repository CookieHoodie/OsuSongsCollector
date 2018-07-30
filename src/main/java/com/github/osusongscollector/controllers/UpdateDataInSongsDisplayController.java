package com.github.osusongscollector.controllers;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.osusongscollector.application.OsuDbParser;
import com.github.osusongscollector.application.SqliteDatabase;
import com.github.osusongscollector.application.ViewLoader;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;


public class UpdateDataInSongsDisplayController extends UpdateDataController {
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
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
			Throwable e1 = loadOsuDbTask.getException();
			if (!(e1 instanceof InterruptedException)) {
				logger.logp(Level.SEVERE, this.getClass().getName(), "setLoadOsuDbTaskOnHandlers", "Failed to load osu!.db", e1);
				Alert alert = new Alert(AlertType.ERROR, "Failed to load osu!.db", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
			else {
				logger.logp(Level.WARNING, this.getClass().getName(), "setLoadOsuDbTaskOnHandlers", "loadOsuDbTask is interrupted", e1);
			}
		});
	}
	
	@Override protected void setUpdateSongsDbTaskOnHandlers(Task<Boolean> updateSongsDbTask) {
		updateSongsDbTask.setOnSucceeded(e -> {
			this.isSongsUpdated = updateSongsDbTask.getValue();
			this.updateBeatmapDetails(this.osuDbFolders, this.songsDb);
		});
		
		updateSongsDbTask.setOnFailed(e -> {
			Throwable e1 = updateSongsDbTask.getException();
			if (!(e1 instanceof InterruptedException)) {
				logger.logp(Level.SEVERE, this.getClass().getName(), "setUpdateSongsDbTaskOnHandlers", "Failed to update data in songs.db", e1);
				Alert alert = new Alert(AlertType.ERROR, "Failed to update songs list.", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
			else {
				logger.logp(Level.WARNING, this.getClass().getName(), "setUpdateSongsDbTaskOnHandlers", "updateSongsDbTask is interrupted", e1);
			}
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
			if (!(e1 instanceof InterruptedException)) {
				logger.logp(Level.SEVERE, this.getClass().getName(), "setUpdateBeatmapDetailsTaskOnHandlers", "Failed to update details in songs.db", e1);
				Alert alert = new Alert(AlertType.ERROR, "Failed to update beatmaps details.", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
			else {
				logger.logp(Level.WARNING, this.getClass().getName(), "setUpdateBeatmapDetailsTaskOnHandlers", "updateBeatmapDetailsTask is interrupted", e1);
			}
		});
	}
	
	private void promptForUserAction() {
		if (this.isBeatmapsDetailsUpdated || this.isSongsUpdated) {
			String message = "Data is successfully updated. Restarting the program is required to take effect. Restart now?"
					+ " (data may be inconsistent if you proceed without restarting)";
			Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.YES, ButtonType.NO);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					logger.logp(Level.INFO, this.getClass().getName(), "promptForUserAction", "Restarting program");
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
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
			this.currentStage.hide();
		}
	}
}
