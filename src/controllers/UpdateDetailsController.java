package controllers;

import java.util.List;
import java.util.Map;

import application.Beatmap;
import application.OsuDbParser;
import application.SqliteDatabase;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Duration;

// TODO: move or add all updateProgress(0,0) to start of the call method in task 

public class UpdateDetailsController extends UpdateDataController {
	private Map<String, List<Beatmap>> osuDbFolders;
	
	@Override protected Task<OsuDbParser> getLoadOsuDbTask(final String fullPathToOsuDb, final String pathToSongsFolder) {
		return new Task<OsuDbParser>() {
			@Override 
			protected OsuDbParser call() throws Exception {
				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				osuDbFolders = osuDb.startParsing();
				return osuDb;
			}
		};
	} 
	
	@Override protected void setUpdateSongsDbTaskOnHandlers(Task<Void> updateSongsDbTask) {
		updateSongsDbTask.setOnSucceeded(e -> {
			this.updateBeatmapDetails();
		});
		
		updateSongsDbTask.setOnFailed(e -> {
			Throwable e1 = updateSongsDbTask.getException();
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to update songs data.", ButtonType.OK);
			alert.showAndWait();
		});
	}
	
	private void updateBeatmapDetails() {
		Task<Void> updateBeatmapDetailsTask = this.getUpdateBeatmapDetailsTask(this.osuDbFolders, this.songsDb);
		updateBeatmapDetailsTask.setOnSucceeded(e -> {
			// TODO: reload program
		});
		
		updateBeatmapDetailsTask.setOnFailed(e -> {
			Throwable e1 = updateBeatmapDetailsTask.getException();
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to update beatmaps details.", ButtonType.OK);
			alert.showAndWait();
		});
		
		this.exec.submit(updateBeatmapDetailsTask);
	}
	
	private Task<Void> getUpdateBeatmapDetailsTask(Map<String, List<Beatmap>> osuDbFolders, SqliteDatabase songsDb) {
		return new Task<Void>() {
			@Override 
			protected Void call() throws Exception {
//				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				songsDb.updateDetails(osuDbFolders);
				// TODO: return needRestart boolean to indicate whether to restart or not then do accordingly
				return null;
			}
		};
	}
	
}
