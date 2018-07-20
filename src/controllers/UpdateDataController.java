package controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import application.Beatmap;
import application.OsuDbParser;
import application.SqliteDatabase;
import application.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.util.Duration;


// TODO: update details inherit this class after this class has added updateDetails functionality
// remember to override getOsuDbTask to get map
// in updateDetails class, override initDataAndStart to give different warning when exiting (ie. changes might have been made
// but not yet reflected, restart to take effect)
// and also, in that class, override finish method to not close stage but gv alert whether to restart now or not
// if restart, move the restart function in SongsDisplay to new public method and then after closing the popup, invoke that method
public class UpdateDataController extends LoadingDialogParentController {
	
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
			closeAlert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					this.exec.shutdownNow();
					try {
						this.exec.awaitTermination(8, TimeUnit.SECONDS);
					} 
					catch (InterruptedException e1) {
						e1.printStackTrace();
						// TODO: show more specific instructions when this happen
						Alert alert = new Alert(AlertType.ERROR, "Program is interrupted while updating data without cleaning up. Relevant files might be corrupted. Consider Reset All to repair.", ButtonType.OK);
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
				e1.printStackTrace();
	//			Alert alert = new Alert(AlertType.ERROR, "Failed to load osu!.db", ButtonType.OK);
	//			alert.showAndWait();
				this.onFailedProceedAlert("Failed to load osu!.db. Proceed anyway?");
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
//		this.instructionLabel.textProperty().bind(updateSongsDbTask.messageProperty());
		this.instructionLabel.setText("Updating songs data");
		
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
				e1.printStackTrace();
				this.onFailedProceedAlert("Failed to update songs data. Proceed anyway?");
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
//		this.instructionLabel.textProperty().bind(updateBeatmapDetailsTask.messageProperty());
		this.instructionLabel.setText("Updating beatmaps details");
		
		this.setUpdateBeatmapDetailsTaskOnHandlers(updateBeatmapDetailsTask);
		this.exec.submit(updateBeatmapDetailsTask);
	}
	
	protected void setUpdateBeatmapDetailsTaskOnHandlers(Task<Boolean> updateBeatmapDetailsTask) {
		updateBeatmapDetailsTask.setOnSucceeded(e -> {
			this.instructionLabel.setText("Finish updating. Loading table data...");
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
				e1.printStackTrace();
				this.onFailedProceedAlert("Failed to update beatmaps details. Proceed anyway?");
			}
		});
	}
	
	
	
	// for reuse
	private void loadSongDisplayViewWrapperForTaskEvent(SqliteDatabase songsDb) {
		try {
//			this.loadSongsDisplayView(songsDb);
			Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
			ViewLoader.loadNewSongsDisplayView(currentStage, songsDb);
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
	
	private void onFailedProceedAlert(String message) {
		Alert alert = new Alert(AlertType.ERROR, message, ButtonType.YES, ButtonType.NO);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
			}
		});
	}
	
}

















//
//
//package controllers;
//
//import java.io.IOException;
//import java.sql.SQLException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import application.OsuDbParser;
//import application.SqliteDatabase;
//import application.ViewLoader;
//import javafx.animation.PauseTransition;
//import javafx.concurrent.Task;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.scene.control.Alert;
//import javafx.scene.control.ButtonType;
//import javafx.scene.control.Label;
//import javafx.scene.control.ProgressBar;
//import javafx.scene.control.Alert.AlertType;
//import javafx.scene.layout.BorderPane;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//
//// TODO: since this is so similar to createDb view, might consider migrating this to that controller or extending it
//public class UpdateDataController {
//	@FXML private Label instructionLabel;
//	@FXML private ProgressBar progressBar;
//	
//	private SqliteDatabase songsDb;
//	private String fullPathToOsuDb;
//	private String pathToSongsFolder;
//	private ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
//        Thread t = new Thread(r);
//        t.setDaemon(true); // allows app to exit if tasks are running
//        return t ;
//    });
//	
//	public void initDataAndStart(Stage currentStage, SqliteDatabase songsDb, String fullPathToOsuDb, String pathToSongsFolder) {
//		this.songsDb = songsDb;
//		this.fullPathToOsuDb = fullPathToOsuDb;
//		this.pathToSongsFolder = pathToSongsFolder;
//		currentStage.setOnCloseRequest(e -> {
//			this.exec.shutdownNow();
//			try {
//				this.exec.awaitTermination(8, TimeUnit.SECONDS);
//			} 
//			catch (InterruptedException e1) {
//				e1.printStackTrace();
//				// TODO: show more specific instructions when this happen
//				Alert alert = new Alert(AlertType.ERROR, "Program is interrupted without cleaning up while updating data. Relevant files might be corrupted. Consider Reset All to repair.", ButtonType.OK);
//				alert.show();
//			}
//		});
//		this.loadOsuDb();
//	}
//	
//	private Task<OsuDbParser> getLoadOsuDbTask() {
//		return new Task<OsuDbParser>() {
//			@Override 
//			protected OsuDbParser call() throws Exception {
//				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
//				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
//				osuDb.startParsing();
//				return osuDb;
//			}
//		};
//	}
//	
//	private Task<Void> getUpdateSongsDbTask(OsuDbParser osuDb) {
//		return new Task<Void>() {
//			@Override
//	        protected Void call() throws Exception {
//				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
//				songsDb.updateData(osuDb);
//	            return null;
//	        }
//		};
//    }
//	
//	private void loadOsuDb() {
//		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask();
//		this.progressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
//		this.instructionLabel.setText("Loading osu!.db");
//		
//		loadOsuDbTask.setOnSucceeded(e -> {
//			this.updateSongsDb(loadOsuDbTask.getValue());
//		});
//		
//		loadOsuDbTask.setOnFailed(e -> {
//			loadOsuDbTask.getException().printStackTrace();
//			Alert alert = new Alert(AlertType.ERROR, "Failed to load osu!.db", ButtonType.OK);
//			alert.showAndWait();
//		});
//		
//		this.exec.submit(loadOsuDbTask);
//	}
//	
//	private void updateSongsDb(OsuDbParser loadedOsuDb) {
//		Task<Void> updateSongsDbTask = this.getUpdateSongsDbTask(loadedOsuDb); 
//		this.progressBar.progressProperty().bind(updateSongsDbTask.progressProperty());
//		this.instructionLabel.setText("Updating songs data");
//		
//		updateSongsDbTask.setOnSucceeded(e -> {
//			this.instructionLabel.setText("Done updating. Loading songs data...");
//			PauseTransition pause = new PauseTransition(Duration.millis(10));
//        	pause.setOnFinished(event -> {
//        		this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
//        	});
//        	pause.play();
//			
//		});
//		
//		updateSongsDbTask.setOnFailed(e -> {
//			Throwable e1 = updateSongsDbTask.getException();
//			e1.printStackTrace();
//			Alert alert = new Alert(AlertType.ERROR, "Failed to update songs data. Proceed anyway?", ButtonType.YES, ButtonType.NO);
//			alert.showAndWait().ifPresent(response -> {
//				if (response == ButtonType.YES) {
//					this.loadSongDisplayViewWrapperForTaskEvent(this.songsDb);
//				}
//			});
//		});
//		this.exec.submit(updateSongsDbTask);
//	}
//	
////	private void loadSongsDisplayView(SqliteDatabase songsDb) throws SQLException, IOException {
////		Stage songsDisplayStage = new Stage();
////		FXMLLoader loader = new FXMLLoader();
////		loader.setLocation(getClass().getResource("/fxml/SongsDisplayView.fxml"));
////		BorderPane root = loader.load();
////		Scene scene = new Scene(root);
////		Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
////		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
////		
////		songsDisplayStage.setTitle(currentStage.getTitle());
////		songsDisplayStage.setScene(scene);
////		ctr.initData(songsDisplayStage, songsDb);
////		songsDisplayStage.show();
////		currentStage.hide();
////	}
//	
//	// for reuse
//	private void loadSongDisplayViewWrapperForTaskEvent(SqliteDatabase songsDb) {
//		try {
////			this.loadSongsDisplayView(songsDb);
//			Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
//			ViewLoader.loadNewSongsDisplayView(currentStage, songsDb);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			Alert alert = new Alert(AlertType.ERROR, "Failed to load displaying screen", ButtonType.OK);
//			alert.showAndWait();
//		} catch (SQLException e1) {
//			e1.printStackTrace();
//			Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve table data from songs.db", ButtonType.OK);
//			alert.showAndWait();
//		}
//	}
//
//}
