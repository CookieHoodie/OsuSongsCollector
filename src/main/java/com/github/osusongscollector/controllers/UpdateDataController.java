package com.github.osusongscollector.controllers;

import com.github.osusongscollector.application.Beatmap;
import com.github.osusongscollector.application.OsuDbParser;
import com.github.osusongscollector.application.SqliteDatabase;
import com.github.osusongscollector.application.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UpdateDataController extends LoadingDialogParentController {
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	protected SqliteDatabase songsDb;
	protected String fullPathToOsuDb;
	protected String pathToSongsFolder;
	protected Map<String, List<Beatmap>> osuDbFolders;
	
	
	public void initDataAndStart(Stage currentStage, SqliteDatabase songsDb, String fullPathToOsuDb, String pathToSongsFolder) {
		this.songsDb = songsDb;
		this.fullPathToOsuDb = fullPathToOsuDb;
		this.pathToSongsFolder = pathToSongsFolder;
		currentStage.setOnCloseRequest(e -> {
			Alert closeAlert = new Alert(AlertType.WARNING, "Closing now may cause inconsistency in songs data. Close anyway?", ButtonType.YES, ButtonType.NO);
			ViewLoader.addStyleToAlert(closeAlert);
			closeAlert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					logger.logp(Level.INFO, this.getClass().getName(), "initDataAndStart", "Closing while task is running");
					this.exec.shutdownNow();
					try {
						this.exec.awaitTermination(8, TimeUnit.SECONDS);
					} 
					catch (InterruptedException e1) {
						logger.logp(Level.SEVERE, this.getClass().getName(), "initDataAndStart", "Failed to wait for tasks to finish", e1);
						Alert alert = new Alert(AlertType.ERROR, "Program is interrupted while updating data without cleaning up. Relevant files might have corrupted.", ButtonType.OK);
						ViewLoader.addStyleToAlert(alert);
						alert.show();
					}
				}
				else {
					e.consume();
				}
			});
		});
		this.loadOsuDb();
	}
	
	@Override protected Task<OsuDbParser> getLoadOsuDbTask(final String fullPathToOsuDb, final String pathToSongsFolder) {
		return new Task<OsuDbParser>() {
			@Override protected OsuDbParser call() throws Exception {
				updateProgress(0, 1);
				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				// assign osuDbFolders to member var
				osuDbFolders = osuDb.startParsing();
				return osuDb;
			}
		};
	} 
	
	protected void loadOsuDb() {
		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask(this.fullPathToOsuDb, this.pathToSongsFolder);
		this.progressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
		this.instructionLabel.setText("Loading osu!.db");
		
		this.setLoadOsuDbTaskOnHandlers(loadOsuDbTask);
		
		this.exec.submit(loadOsuDbTask);
	}
	
	// setHandlers are separated so that they can be overriden more easily
	protected void setLoadOsuDbTaskOnHandlers(Task<OsuDbParser> loadOsuDbTask) {
		loadOsuDbTask.setOnSucceeded(e -> {
			this.updateSongsDb(loadOsuDbTask.getValue());
		});
		
		loadOsuDbTask.setOnFailed(e -> {
			Throwable e1 = loadOsuDbTask.getException();
			if (!(e1 instanceof InterruptedException)) {
				logger.logp(Level.SEVERE, this.getClass().getName(), "setLoadOsuDbTaskOnHandlers", "Failed to load osu!.db", e1);
				this.onFailedProceedAlert("Failed to load osu!.db. Proceed anyway?");
			}
			else {
				logger.logp(Level.WARNING, this.getClass().getName(), "setLoadOsuDbTaskOnHandlers", "loadOsuDbTask is interrupted", e1);
			}
		});
	}
	
	
	protected Task<Boolean> getUpdateSongsDbTask(OsuDbParser osuDb, SqliteDatabase songsDb) {
		return new Task<Boolean>() {
			@Override protected Boolean call() throws Exception {
				updateProgress(0, 1);
				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				return songsDb.updateData(osuDb);
	        }
		};
    }
	
	protected void updateSongsDb(OsuDbParser loadedOsuDb) {
		Task<Boolean> updateSongsDbTask = this.getUpdateSongsDbTask(loadedOsuDb, this.songsDb); 
		this.progressBar.progressProperty().bind(updateSongsDbTask.progressProperty());
		this.instructionLabel.setText("Updating songs list");
		
		this.setUpdateSongsDbTaskOnHandlers(updateSongsDbTask);
		this.exec.submit(updateSongsDbTask);
	}
	
	protected void setUpdateSongsDbTaskOnHandlers(Task<Boolean> updateSongsDbTask) {
		updateSongsDbTask.setOnSucceeded(e -> {
			this.updateBeatmapDetails(this.osuDbFolders, this.songsDb);
		});
		
		updateSongsDbTask.setOnFailed(e -> {
			Throwable e1 = updateSongsDbTask.getException();
			if (!(e1 instanceof InterruptedException)) {
				logger.logp(Level.SEVERE, this.getClass().getName(), "setUpdateSongsDbTaskOnHandlers", "Failed to update data in songs.db", e1);
				this.onFailedProceedAlert("Failed to update songs list. Proceed anyway?");
			}
			else {
				logger.logp(Level.WARNING, this.getClass().getName(), "setUpdateSongsDbTaskOnHandlers", "updateSongsDbTask is interrupted", e1);
			}
		});
	}
	
	protected Task<Boolean> getUpdateBeatmapDetailsTask(Map<String, List<Beatmap>> osuDbFolders, SqliteDatabase songsDb) {
		return new Task<Boolean>() {
			@Override 
			protected Boolean call() throws Exception {
				updateProgress(0, 1);
				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				return songsDb.updateDetails(osuDbFolders);
			}
		};
	}
	
	protected void updateBeatmapDetails(Map<String, List<Beatmap>> osuDbFolders, SqliteDatabase songsDb) {
		Task<Boolean> updateBeatmapDetailsTask = this.getUpdateBeatmapDetailsTask(osuDbFolders, songsDb);
		this.progressBar.progressProperty().bind(updateBeatmapDetailsTask.progressProperty());
		this.instructionLabel.setText("Updating beatmaps details");
		
		this.setUpdateBeatmapDetailsTaskOnHandlers(updateBeatmapDetailsTask);
		this.exec.submit(updateBeatmapDetailsTask);
	}
	
	protected void setUpdateBeatmapDetailsTaskOnHandlers(Task<Boolean> updateBeatmapDetailsTask) {
		updateBeatmapDetailsTask.setOnSucceeded(e -> {
			this.instructionLabel.setText("Finish updating. Loading display window...");
			// use pause so that the label text can be properly shown
			PauseTransition pause = new PauseTransition(Duration.millis(10));
        	pause.setOnFinished(e1 -> {
        		this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
        	});
        	pause.play();
		});
		
		updateBeatmapDetailsTask.setOnFailed(e -> {
			Throwable e1 = updateBeatmapDetailsTask.getException();
			if (!(e1 instanceof InterruptedException)) {
				logger.logp(Level.SEVERE, this.getClass().getName(), "setUpdateBeatmapDetailsTaskOnHandlers", "Failed to update details in songs.db", e1);
				this.onFailedProceedAlert("Failed to update beatmaps details. Proceed anyway?");
			}
			else {
				logger.logp(Level.WARNING, this.getClass().getName(), "setUpdateBeatmapDetailsTaskOnHandlers", "updateBeatmapDetailsTask is interrupted", e1);
			}
		});
	}
	
	
	
	// for reuse
	private void loadSongDisplayViewWrapperForTaskEvent(SqliteDatabase songsDb) {
		try {
			logger.log(Level.INFO, "Loading SongsDisplayView");
			Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
			ViewLoader.loadNewSongsDisplayView(currentStage, songsDb, this.hostServices);
		} catch (IOException e1) {
			logger.log(Level.SEVERE, "Failed to load SongsDisplayView", e1);
			Alert alert = new Alert(AlertType.ERROR, "Failed to load display window", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		} catch (SQLException e1) {
			logger.log(Level.SEVERE, "Failed to get table data during loading songsDisplayView", e1);
			Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve table data from songs.db", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
	}
	
	private void onFailedProceedAlert(String message) {
		Alert alert = new Alert(AlertType.ERROR, message, ButtonType.YES, ButtonType.NO);
		ViewLoader.addStyleToAlert(alert);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				logger.log(Level.WARNING, "Continue after failing to update songs.db");
				this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
			}
		});
	}
	
}
