package controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import application.OsuDbParser;
import application.SqliteDatabase;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


// TODO: since this is so similar to createDb view, might consider migrating this to that controller or extending it
public class UpdateDataController {
	@FXML private Label instructionLabel;
	@FXML private ProgressBar progressBar;
	
	private SqliteDatabase songsDb;
	private String fullPathToOsuDb;
	private String pathToSongsFolder;
	private ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t ;
    });
	
	public void initDataAndStart(Stage currentStage, SqliteDatabase songsDb, String fullPathToOsuDb, String pathToSongsFolder) {
		this.songsDb = songsDb;
		this.fullPathToOsuDb = fullPathToOsuDb;
		this.pathToSongsFolder = pathToSongsFolder;
		currentStage.setOnCloseRequest(e -> {
			this.exec.shutdownNow();
			try {
				this.exec.awaitTermination(8, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				// TODO: show more specific instructions when this happen
				Alert alert = new Alert(AlertType.ERROR, "Program is interrupted without cleaning up while updating data. Relevant files might be corrupted. Consider Reset All to repair.", ButtonType.OK);
				alert.show();
			}
		});
		this.loadOsuDb();
	}
	
	private Task<OsuDbParser> getLoadOsuDbTask() {
		return new Task<OsuDbParser>() {
			@Override 
			protected OsuDbParser call() throws Exception {
				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				osuDb.startParsing();
				return osuDb;
			}
		};
	}
	
	private Task<Void> getUpdateSongsDbTask(OsuDbParser osuDb) {
		return new Task<Void>() {
			@Override
	        protected Void call() throws Exception {
				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				songsDb.updateData(osuDb);
	            return null;
	        }
		};
    }
	
	private void loadOsuDb() {
		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask();
		this.progressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
		this.instructionLabel.setText("Loading osu!.db");
		
		loadOsuDbTask.setOnSucceeded(e -> {
			this.updateSongsDb(loadOsuDbTask.getValue());
		});
		
		loadOsuDbTask.setOnFailed(e -> {
			loadOsuDbTask.getException().printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to load osu!.db", ButtonType.OK);
			alert.showAndWait();
		});
		
		this.exec.submit(loadOsuDbTask);
	}
	
	private void updateSongsDb(OsuDbParser loadedOsuDb) {
		Task<Void> updateSongsDbTask = this.getUpdateSongsDbTask(loadedOsuDb); 
		this.progressBar.progressProperty().bind(updateSongsDbTask.progressProperty());
		this.instructionLabel.setText("Updating songs data");
		
		updateSongsDbTask.setOnSucceeded(e -> {
			this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
		});
		
		updateSongsDbTask.setOnFailed(e -> {
			Throwable e1 = updateSongsDbTask.getException();
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to update songs data. Proceed anyway?", ButtonType.YES, ButtonType.NO);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
				}
			});
		});
		this.exec.submit(updateSongsDbTask);
	}
	
	private void loadSongsDisplayView(SqliteDatabase songsDb) throws SQLException, IOException {
		Stage songsDisplayStage = new Stage();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/SongsDisplayView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
		
		songsDisplayStage.setTitle(currentStage.getTitle());
		songsDisplayStage.setScene(scene);
		ctr.initData(songsDisplayStage, songsDb);
		songsDisplayStage.show();
		currentStage.hide();
	}
	
	// for reuse
	private void loadSongDisplayViewWrapperForTaskEvent(SqliteDatabase songsDb) {
		try {
			this.loadSongsDisplayView(songsDb);
		} catch (IOException e1) {
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to load displaying screen", ButtonType.OK);
			alert.showAndWait();
		} catch (SQLException e1) {
			e1.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve table data from songs.db", ButtonType.OK);
			alert.showAndWait();
		}
	}

}
