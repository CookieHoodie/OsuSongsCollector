package controllers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import application.OsuDbParser;
import application.SqliteDatabase;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class UpdateDataController {
	@FXML private Label instructionLabel;
	
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
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		this.updateSongsDb();
	}
	
	private Task<Void> getUpdateSongsDbTask() {
		return new Task<Void>() {
			@Override
	        protected Void call() throws Exception {
				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				osuDb.startParsing();
				songsDb.updateData(osuDb);
	            return null;
	        }
		};
    }
	
	
	private void updateSongsDb() {
		Task<Void> updateSongsDbTask = this.getUpdateSongsDbTask(); 
		updateSongsDbTask.setOnSucceeded(e -> {
			this.instructionLabel.setText("Success");
		});
		
		updateSongsDbTask.setOnFailed(e -> {
			this.instructionLabel.setText(updateSongsDbTask.getException().getMessage());
		});
		this.exec.submit(updateSongsDbTask);
	}

}
