package controllers;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import application.Main;
import application.OsuDbParser;
import application.SqliteDatabase;
import application.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.util.Duration;


public class LoadAndCreateDatabaseController extends LoadingDialogParentController {
	private String fullPathToOsuDb;
	private String pathToSongsFolder;
	
	
	public void initDataAndStart(Stage currentStage, String fullPathToOsuDb, String pathToSongsFolder) {
		this.fullPathToOsuDb = fullPathToOsuDb;
		this.pathToSongsFolder = pathToSongsFolder;
		currentStage.setOnCloseRequest(e -> {
			// show alert to user to reconfirm exit
			Alert alert = new Alert(AlertType.WARNING, "Unexpected results can happen if the window is closed now. Close anyway?", ButtonType.YES, ButtonType.NO);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					this.exec.shutdownNow();
					try {
						this.exec.awaitTermination(8, TimeUnit.SECONDS);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						Alert corruptionAlert = new Alert(AlertType.ERROR, "Program is interrupted without cleaning up while initializing. Relevant files might have corrupted.", ButtonType.OK);
						ViewLoader.addStyleToAlert(corruptionAlert);
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
				Alert alert = new Alert(AlertType.ERROR, "osu!.db is not found. Please make sure the osu!.exe path chosen is correct!", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
			else if (!(e instanceof InterruptedException)) {
				Alert alert = new Alert(AlertType.ERROR, "Error loading osu!.db", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
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
		this.instructionLabel.setText("(2/2): Storing data. This might take a while...");
		Task<SqliteDatabase> createSongsDbTask = this.getCreateSongsDbTask(osuDb);
		this.progressBar.progressProperty().bind(createSongsDbTask.progressProperty());
		createSongsDbTask.setOnFailed(event -> {
			Throwable e = createSongsDbTask.getException();
			e.printStackTrace();
			if (!(e instanceof InterruptedException)) {
				Alert alert = new Alert(AlertType.ERROR, "Failed to store data from osu!.db", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
		});
		
		createSongsDbTask.setOnSucceeded(event -> {
			this.instructionLabel.setText("Finish storing data. Loading display window...");
			// pause because otherwise label setText is not updated in UI
			PauseTransition pause = new PauseTransition(Duration.millis(10));
        	pause.setOnFinished(e -> {
        		SqliteDatabase songsDb = createSongsDbTask.getValue();
        		try {
        			Stage currentStage = (Stage) this.instructionLabel.getScene().getWindow();
        			ViewLoader.loadNewSongsDisplayView(currentStage, songsDb);
    			}
        		catch (SQLException e1) {
    				e1.printStackTrace();
    				Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve data from songs.db", ButtonType.OK);
    				ViewLoader.addStyleToAlert(alert);
    				alert.showAndWait();
    			}
        		catch (Exception e1) {
    				e1.printStackTrace();
    				Alert alert = new Alert(AlertType.ERROR, "Failed to load display window", ButtonType.OK);
    				ViewLoader.addStyleToAlert(alert);
    				alert.showAndWait();
    			} 
        	});
        	pause.play();
		});
        this.exec.submit(createSongsDbTask);
	}
}
