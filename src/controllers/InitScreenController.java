package controllers;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.Main;
import application.OsuDbParser;
import application.SqliteDatabase;
import application.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

// TODO: at the end, ensure only one app can be opened at the same time. Otherwise racing condition can happen in SQL
// TODO: change all the printStacks to logging
// TODO: change all fxml api to get rid of warnings
// TODO: refine all stage titles, description and variable names (including @FXML) before releasing

public class InitScreenController {
	@FXML private Label welcomeLabel;
	
	private SqliteDatabase songsDb;
	private String pathToOsuDb;
	private String pathToSongsFolder;
	
	private class CheckOsuDbUpdateService extends Service<Boolean> {
		@Override
	    protected Task<Boolean> createTask() {
			return new Task<Boolean>() {
				@Override
	            protected Boolean call() throws Exception {
					// To come to here, songsDb must hv existed
					songsDb.connect();
					ResultSet metadataRs = songsDb.selectMetadata();
					if (metadataRs.next()) {
						int folderCount = metadataRs.getInt(SqliteDatabase.TableData.Metadata.FOLDER_COUNT);
						int numberOfBeatmaps = metadataRs.getInt(SqliteDatabase.TableData.Metadata.NUMBER_OF_BEATMAPS);
						ResultSet configRs = songsDb.selectConfig();
						if (configRs.next()) {
							pathToOsuDb = configRs.getString(SqliteDatabase.TableData.Config.PATH_TO_OSU_DB);
							pathToSongsFolder = configRs.getString(SqliteDatabase.TableData.Config.PATH_TO_SONGS_FOLDER);
							OsuDbParser osuDbMeta = new OsuDbParser(pathToOsuDb, pathToSongsFolder);
							osuDbMeta.startParsingMetadataOnly();
							
							if (osuDbMeta.getFolderCount() != folderCount || osuDbMeta.getNumberOfBeatmaps() != numberOfBeatmaps) {
								return false;
							}
							
							return true;
						}
						else {
							throw new SQLException("Failed to retrieve config");
						}
					}
					else {
						throw new SQLException("Failed to retrieve metadata");
					}
	            }
	        };
	    }
	}
	
	public void startChecking() {
		// 1st quick check if songs db is ald created (not created means 1st time use)
		this.songsDb = new SqliteDatabase(Main.DB_NAME);
		// if db exists, check for any new songs or deleted songs
		if (songsDb.isDbExist()) {
			CheckOsuDbUpdateService checkAllSetService = new CheckOsuDbUpdateService();
			checkAllSetService.setOnSucceeded(e -> {
				boolean isUpToDate = checkAllSetService.getValue();
				if (isUpToDate) {
					try {
						Stage currentStage = (Stage) this.welcomeLabel.getScene().getWindow();
						ViewLoader.loadNewSongsDisplayView(currentStage, this.songsDb);
					}
					catch (SQLException e1) {
						// this exception comes from initTableData method for populating the tableView
						e1.printStackTrace();
						this.displayAlertAndExit("Failed to retrieve data from songs.db");
					}
					catch (Exception e1) {
						e1.printStackTrace();
						this.displayAlertAndExit("Failed to load display window");
					}
				}
				else {
					try {
						this.loadUpdateDataView();
					} 
					catch (Exception e1) {
						e1.printStackTrace();
						this.displayAlertAndExit("Failed to load update window");
					}
				}
			});
			
			checkAllSetService.setOnFailed(e -> {
				checkAllSetService.getException().printStackTrace();
				this.displayAlertAndExit("Corrupted or interrupted when checking for songs added or deleted");
			});
			checkAllSetService.start();
		}
		// if not, prompt user for initialization
		else {
			// wait for at least one second for first time load
			PauseTransition pause = new PauseTransition(Duration.millis(1000));
	    	pause.setOnFinished(e1 -> {
	    		try {
	    			this.loadSetSongsFolderPathView();
	    		}
				catch (Exception e) {
					e.printStackTrace();
					this.displayAlertAndExit("Failed to load initialization screen");
				}
	    	});
	    	pause.play();
		}
		
	}
	
	private void displayAlertAndExit(String message) {
		Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
		ViewLoader.addStyleToAlert(alert);
		alert.showAndWait();
		Stage currentStage = (Stage) this.welcomeLabel.getScene().getWindow();
		currentStage.hide();
	}
	
	private void loadSetSongsFolderPathView() throws IOException {
		Stage setSongsFolderStage = new Stage();
		setSongsFolderStage.setTitle("Configuration");
		setSongsFolderStage.setResizable(false);
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/SetSongsFolderPathView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		Stage primaryStage = (Stage) this.welcomeLabel.getScene().getWindow();
		setSongsFolderStage.setTitle(primaryStage.getTitle());
		setSongsFolderStage.setScene(scene);
		setSongsFolderStage.show();
		primaryStage.hide();;
	}
	
	private void loadUpdateDataView() throws IOException {
		Stage updateDataStage = new Stage();
		updateDataStage.setTitle("Update Songs Data");
		updateDataStage.setResizable(false);
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/LoadingDialogParentView.fxml"));
		UpdateDataController ctr = new UpdateDataController();
		loader.setController(ctr);
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		Stage primaryStage = (Stage) this.welcomeLabel.getScene().getWindow();
		
		updateDataStage.setScene(scene);
		// the last two paths must have already initialized to come to here
		ctr.initDataAndStart(updateDataStage, this.songsDb, this.pathToOsuDb, this.pathToSongsFolder);
		updateDataStage.setScene(scene);
		updateDataStage.show();
		primaryStage.hide();
	}
}
