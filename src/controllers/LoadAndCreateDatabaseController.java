package controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import application.Main;
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


// TODO: best practice: pass parameter into the tasks and assign them as final
public class LoadAndCreateDatabaseController extends LoadingDialogParentController {
	private String fullPathToOsuDb;
	private String pathToSongsFolder;
	
	
	public void initDataAndStart(Stage currentStage, String fullPathToOsuDb, String pathToSongsFolder) {
		this.fullPathToOsuDb = fullPathToOsuDb;
		this.pathToSongsFolder = pathToSongsFolder;
		currentStage.setOnCloseRequest(e -> {
			// show alert to user to reconfirm exit
			Alert alert = new Alert(AlertType.WARNING, "Unexpected results can happen if the window is closed now. Close anyway?", ButtonType.YES, ButtonType.NO);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					this.exec.shutdownNow();
					try {
						this.exec.awaitTermination(8, TimeUnit.SECONDS);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						// TODO: show more specific instructions when this happen
						Alert corruptionAlert = new Alert(AlertType.ERROR, "Program is interrupted without cleaning up while initializing. Relevant files might be corrupted. Consider Reset All to repair.", ButtonType.OK);
						corruptionAlert.show();
					}
				}
				// if not, continue the process
				else {
					e.consume();
				}
			});
		});
		this.loadOsuDb();
	}
	
	
	private Task<SqliteDatabase> getCreateSongsDbTask(OsuDbParser osuDb) {
		return new Task<SqliteDatabase>() {
			@Override
			protected SqliteDatabase call() throws Exception {
				updateProgress(0, 1);
				SqliteDatabase songsDb = new SqliteDatabase(Main.DB_NAME);
				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				songsDb.createDatabase();
				songsDb.createTables();
				if (Thread.currentThread().isInterrupted()) {
					songsDb.cleanUpThread(true);
					throw new InterruptedException("Interrupted while creating songs.db");
				}
				songsDb.insertAllData(osuDb);
				return songsDb;
			}
		};
	}
	
	private void loadOsuDb() {
		this.instructionLabel.setText("(1/2): Loading osu!.db");
		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask(this.fullPathToOsuDb, this.pathToSongsFolder);
		this.progressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
		
		loadOsuDbTask.setOnFailed(event -> {
			Throwable e = loadOsuDbTask.getException();
			e.printStackTrace();
			if (e instanceof FileNotFoundException) {
				Alert alert = new Alert(AlertType.ERROR, "osu!.db is not found. Please make sure the folder chosen is correct!", ButtonType.OK);
				alert.showAndWait();
			}
			else {
				Alert alert = new Alert(AlertType.ERROR, "Error loading osu!.db or interrupted", ButtonType.OK);
				alert.show();
			}
		});
		
		loadOsuDbTask.setOnSucceeded(e -> {
			OsuDbParser osuDb = loadOsuDbTask.getValue();
    		this.createSongsDb(osuDb);
		});
		
		this.exec.submit(loadOsuDbTask);
	}
	
	
	private void createSongsDb(OsuDbParser osuDb) {
		this.instructionLabel.setText("(2/2): Creating songs.db. This might take a while...");
		Task<SqliteDatabase> createSongsDbTask = this.getCreateSongsDbTask(osuDb);
		this.progressBar.progressProperty().bind(createSongsDbTask.progressProperty());
		createSongsDbTask.setOnFailed(event -> {
			createSongsDbTask.getException().printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to store data from osu!.db", ButtonType.OK);
			alert.showAndWait();
		});
		
		createSongsDbTask.setOnSucceeded(event -> {
			this.instructionLabel.setText("Done. Loading songs data...");
			// pause because otherwise label setText is not updated in UI
			PauseTransition pause = new PauseTransition(Duration.millis(10));
        	pause.setOnFinished(e -> {
        		SqliteDatabase songsDb = createSongsDbTask.getValue();
        		try {
//    				this.loadSongsDisplayStage(songsDb);
        			Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
        			ViewLoader.loadNewSongsDisplayView(currentStage, songsDb);
    			}
        		catch (SQLException e1) {
    				e1.printStackTrace();
    				Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve table data from songs.db", ButtonType.OK);
    				alert.showAndWait();
    			}
        		catch (IOException e1) {
    				e1.printStackTrace();
    				Alert alert = new Alert(AlertType.ERROR, "Failed to load displaying screen", ButtonType.OK);
    				alert.showAndWait();
    			} 
        	});
        	pause.play();
		});
        this.exec.submit(createSongsDbTask);
	}
}


























//package controllers;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.sql.SQLException;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//
//import application.Comparators;
//import application.Main;
//import application.OsuDbParser;
//import application.SqliteDatabase;
//import application.ViewLoader;
//import application.Beatmap;
//import javafx.animation.PauseTransition;
//import javafx.application.Platform;
//import javafx.beans.binding.Bindings;
//import javafx.concurrent.Service;
//import javafx.concurrent.Task;
//import javafx.concurrent.Worker;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.scene.control.Alert;
//import javafx.scene.control.Button;
//import javafx.scene.control.ButtonType;
//import javafx.scene.control.Label;
//import javafx.scene.control.ProgressBar;
//import javafx.scene.control.Alert.AlertType;
//import javafx.scene.layout.BorderPane;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//
//// TODO: best practice: pass parameter into the tasks and assign them as final
//public class LoadAndCreateDatabaseController {
//	private String fullPathToOsuDb;
//	private String pathToSongsFolder;
//	private ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
//        Thread t = new Thread(r);
//        t.setDaemon(true); // allows app to exit if tasks are running
//        return t ;
//    });
//	
//	@FXML private ProgressBar testProgressBar;
//	@FXML private Label testStateLabel;
//	
//	public void initDataAndStart(Stage currentStage, String fullPathToOsuDb, String pathToSongsFolder) {
//		this.fullPathToOsuDb = fullPathToOsuDb;
//		this.pathToSongsFolder = pathToSongsFolder;
//		currentStage.setOnCloseRequest(e -> {
//			// show alert to user to reconfirm exit
//			Alert alert = new Alert(AlertType.WARNING, "Unexpected results can happen if the window is closed now. Close anyway?", ButtonType.YES, ButtonType.NO);
//			alert.showAndWait().ifPresent(response -> {
//				if (response == ButtonType.YES) {
//					this.exec.shutdownNow();
//					try {
//						this.exec.awaitTermination(8, TimeUnit.SECONDS);
//					} catch (InterruptedException e1) {
//						e1.printStackTrace();
//						// TODO: show more specific instructions when this happen
//						Alert corruptionAlert = new Alert(AlertType.ERROR, "Program is interrupted without cleaning up while initializing. Relevant files might be corrupted. Consider Reset All to repair.", ButtonType.OK);
//						corruptionAlert.show();
//					}
//				}
//				// if not, continue the process
//				else {
//					e.consume();
//				}
//			});
//		});
//		this.loadOsuDb();
//	}
//	
//	private Task<OsuDbParser> getLoadOsuDbTask() {
//		return new Task<OsuDbParser>() {
//			@Override
//	        protected OsuDbParser call() throws Exception {
//				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
//				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
//				osuDb.startParsing();
//	            return osuDb;
//	        }
//		};
//    }
//	
//	private Task<SqliteDatabase> getCreateSongsDbTask(OsuDbParser osuDb) {
//		return new Task<SqliteDatabase>() {
//			@Override
//			protected SqliteDatabase call() throws Exception {
//				SqliteDatabase songsDb = new SqliteDatabase(Main.DB_NAME);
//				updateProgress(0, 0);
//				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
//				songsDb.createDatabase();
//				songsDb.createTables();
//				if (Thread.currentThread().isInterrupted()) {
//					songsDb.cleanUpThread(true);;
//				}
//				songsDb.insertAllData(osuDb);
//				return songsDb;
//			}
//		};
//	}
//	
//	private void loadOsuDb() {
//		this.testStateLabel.setText("(1/2): Loading osu!.db");
//		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask();
//		this.testProgressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
//		
//		loadOsuDbTask.setOnFailed(event -> {
//			Throwable e = loadOsuDbTask.getException();
//			e.printStackTrace();
//			if (e instanceof FileNotFoundException) {
//				Alert alert = new Alert(AlertType.ERROR, "osu!.db is not found. Please make sure the folder chosen is correct!", ButtonType.OK);
//				alert.showAndWait();
//			}
//			else {
//				Alert alert = new Alert(AlertType.ERROR, "Error loading osu!.db or interrupted", ButtonType.OK);
//				alert.show();
//			}
//		});
//		
//		loadOsuDbTask.setOnSucceeded(e -> {
//			OsuDbParser osuDb = loadOsuDbTask.getValue();
//    		this.createSongsDb(osuDb);
//		});
//		
//		this.exec.submit(loadOsuDbTask);
//	}
//	
//	
//	private void createSongsDb(OsuDbParser osuDb) {
//		this.testStateLabel.setText("(2/2): Creating songs.db. This might take a while...");
//		Task<SqliteDatabase> createSongsDbTask = this.getCreateSongsDbTask(osuDb);
//		this.testProgressBar.progressProperty().bind(createSongsDbTask.progressProperty());
//		createSongsDbTask.setOnFailed(event -> {
//			createSongsDbTask.getException().printStackTrace();
//			Alert alert = new Alert(AlertType.ERROR, "Failed to store data from osu!.db", ButtonType.OK);
//			alert.showAndWait();
//		});
//		
//		createSongsDbTask.setOnSucceeded(event -> {
//			this.testStateLabel.setText("Done. Loading songs data...");
//			// pause because otherwise label setText is not updated in UI
//			PauseTransition pause = new PauseTransition(Duration.millis(10));
//        	pause.setOnFinished(e -> {
//        		SqliteDatabase songsDb = createSongsDbTask.getValue();
//        		try {
////    				this.loadSongsDisplayStage(songsDb);
//        			Stage currentStage = (Stage) this.testStateLabel.getScene().getWindow();
//        			ViewLoader.loadNewSongsDisplayView(currentStage, songsDb);
//    			}
//        		catch (SQLException e1) {
//    				e1.printStackTrace();
//    				Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve table data from songs.db", ButtonType.OK);
//    				alert.showAndWait();
//    			}
//        		catch (IOException e1) {
//    				e1.printStackTrace();
//    				Alert alert = new Alert(AlertType.ERROR, "Failed to load displaying screen", ButtonType.OK);
//    				alert.showAndWait();
//    			} 
//        	});
//        	pause.play();
//		});
//        this.exec.submit(createSongsDbTask);
//	}
//	
//	
////	private void loadSongsDisplayStage(SqliteDatabase songsDb) throws IOException, SQLException {
////		Stage songsDisplayStage = new Stage();
////		FXMLLoader loader = new FXMLLoader();
////		loader.setLocation(getClass().getResource("/fxml/SongsDisplayView.fxml"));
////		BorderPane root = loader.load();
////		Scene scene = new Scene(root);
////		Stage currentStage = (Stage) this.testStateLabel.getScene().getWindow();
////		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
////		
////		songsDisplayStage.setTitle(currentStage.getTitle());
////		songsDisplayStage.setScene(scene);
////		ctr.initData(songsDisplayStage, songsDb);
////		songsDisplayStage.show();
////		currentStage.hide();
////	}
//
//}
