package controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import application.Comparators;
import application.Main;
import application.OsuDbParser;
import application.OsuSongsCollector;
import application.SqliteDatabase;
import application.Beatmap;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


// TODO: best practice: pass parameter into the tasks and assign them as final
public class LoadAndCreateDatabaseController {
	private String fullPathToOsuDb;
	private String pathToSongsFolder;
	private ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t ;
    });
	
	@FXML private ProgressBar testProgressBar;
	@FXML private Label testStateLabel;
	
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
	
	private Task<SqliteDatabase> getCreateSongsDbTask(OsuDbParser osuDb) {
		return new Task<SqliteDatabase>() {
			@Override
			protected SqliteDatabase call() throws Exception {
				SqliteDatabase songsDb = new SqliteDatabase(Main.DB_NAME);
				updateProgress(0, 0);
				songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
				songsDb.createDatabase();
				songsDb.createTables();
				if (Thread.currentThread().isInterrupted()) {
					songsDb.cancelThread();
				}
				songsDb.insertAllData(osuDb);
				return songsDb;
			}
		};
	}
	
	private void loadOsuDb() {
		this.testStateLabel.setText("1/2: Loading osu!.db");
		Task<OsuDbParser> loadOsuDbTask = this.getLoadOsuDbTask();
		this.testProgressBar.progressProperty().bind(loadOsuDbTask.progressProperty());
		loadOsuDbTask.stateProperty().addListener((obs, oldValue, newValue) -> {
        	switch (newValue) {
        	case FAILED:
        		Throwable e = loadOsuDbTask.getException();
        		this.testStateLabel.setText(e.getMessage());
        		break;
        	case SUCCEEDED:
        		OsuDbParser osuDb = loadOsuDbTask.getValue();
        		this.createSongsDb(osuDb);
        		break;
			default:
				break;
        	}
        });
		this.exec.submit(loadOsuDbTask);
	}
	
	
	private void createSongsDb(OsuDbParser osuDb) {
		this.testStateLabel.setText("2/2 Creating database");
		Task<SqliteDatabase> createSongsDbTask = this.getCreateSongsDbTask(osuDb);
		this.testProgressBar.progressProperty().bind(createSongsDbTask.progressProperty());
		createSongsDbTask.stateProperty().addListener((obs, oldValue, newValue) -> {
        	switch (newValue) {
        	case FAILED:
        		Throwable e = createSongsDbTask.getException();
        		this.testStateLabel.setText(e.getMessage());
        		break;
        	case SUCCEEDED:
        		this.testStateLabel.setText("Done. Loading required data...");
        		SqliteDatabase songsDb = createSongsDbTask.getValue();
        		try {
					this.loadSongsDisplayStage(songsDb);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
        		break;
			default:
				break;
        	}
        });
        this.exec.submit(createSongsDbTask);
	}
	
	
	
	
	
	
//	private class LoadOsuDbService extends Service<OsuDbParser> {
//		@Override
//        protected Task<OsuDbParser> createTask() {
//			return new Task<OsuDbParser>() {
//				@Override
//                protected OsuDbParser call() throws Exception {
//					OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
//					osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork), this);
//					osuDb.startParsing();
//                    return osuDb;
//                }
//            };
//        }
//	}
//	
//	private class CreateSongsDbService extends Service<SqliteDatabase> {
//		OsuDbParser osuDb;
//		
//		private CreateSongsDbService(OsuDbParser osuDb) {
//			this.osuDb = osuDb;
//		}
//		
//		@Override
//		protected Task<SqliteDatabase> createTask() {
//			return new Task<SqliteDatabase>() {
//				@Override
//				protected SqliteDatabase call() throws Exception {
//					SqliteDatabase songsDb = new SqliteDatabase(Main.DB_NAME);
//					updateProgress(0, 0);
//					songsDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork), this);
//					songsDb.createDatabase();
//					songsDb.createTables();
//					if (this.isCancelled()) {
//						songsDb.cancelThread();
//					}
//					songsDb.insertAllData(osuDb);
//					return songsDb;
//				}
//			};
//		}
//	}
//	
//	private void loadOsuDb() {
//		this.testStateLabel.setText("1/2: Loading osu!.db");
//		this.osuDbService = new LoadOsuDbService();
//        this.testProgressBar.progressProperty().bind(this.osuDbService.progressProperty());
//        this.osuDbService.stateProperty().addListener((obs, oldValue, newValue) -> {
//        	switch (newValue) {
//        	case FAILED:
//        		Throwable e = this.osuDbService.getException();
//        		this.testStateLabel.setText(e.getMessage());
//        		break;
//        	case SUCCEEDED:
//        		OsuDbParser osuDb = this.osuDbService.getValue();
//				this.createSongsDb(osuDb);
//        		break;
//			default:
//				break;
//        	}
//        });
//        this.osuDbService.start();
//	}
//	
//	private void createSongsDb(OsuDbParser osuDb) {
//		this.testStateLabel.setText("2/2 Creating database");
//		this.songsDbService = new CreateSongsDbService(osuDb);
//		this.testProgressBar.progressProperty().bind(this.songsDbService.progressProperty());
//        this.songsDbService.stateProperty().addListener((obs, oldValue, newValue) -> {
//        	switch (newValue) {
//        	case FAILED:
//        		Throwable e = this.osuDbService.getException();
//        		this.testStateLabel.setText(e.getMessage());
//        		break;
//        	case SUCCEEDED:
//        		this.testStateLabel.setText("All done");
//        		SqliteDatabase songsDb = this.songsDbService.getValue();
//        		try {
//					this.loadSongsDisplayStage(songsDb);
//				} catch (IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				} catch (SQLException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//        		break;
//			default:
//				break;
//        	}
//        });
//        this.songsDbService.start();
//	}
//	
	private void loadSongsDisplayStage(SqliteDatabase songsDb) throws IOException, SQLException {
		Stage songsDisplayStage = new Stage();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/SongsDisplayView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		Stage currentStage = (Stage) this.testStateLabel.getScene().getWindow();
		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
		
//		primaryStage.setScene(scene);
		songsDisplayStage.setTitle(currentStage.getTitle());
		songsDisplayStage.setScene(scene);
		ctr.initData(songsDisplayStage, songsDb);
		songsDisplayStage.show();
		currentStage.hide();
	}

}



// TODO: exit thread safely