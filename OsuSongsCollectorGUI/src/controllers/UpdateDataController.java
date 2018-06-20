package controllers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import application.OsuDbParser;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class UpdateDataController {
	private String fullPathToOsuDb;
	private String pathToSongsFolder;
	private ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t ;
    });
	
	public void initDataAndStart(Stage currentStage, String fullPathToOsuDb, String pathToSongsFolder) {
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
//		this.loadOsuDb();
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
	
//	private void loadOsuDb() {
//		this.testStateLabel.setText("1/2: Loading osu!.db");
//		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask();
//		this.testProgressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
//		loadOsuDbTask.stateProperty().addListener((obs, oldValue, newValue) -> {
//        	switch (newValue) {
//        	case FAILED:
//        		Throwable e = loadOsuDbTask.getException();
//        		this.testStateLabel.setText(e.getMessage());
//        		break;
//        	case SUCCEEDED:
//        		OsuDbParser osuDb = loadOsuDbTask.getValue();
//        		this.createSongsDb(osuDb);
//        		break;
//			default:
//				break;
//        	}
//        });
//		this.exec.submit(loadOsuDbTask);
//	}
	
	private void updateSongsDb() {
		
	}

}
