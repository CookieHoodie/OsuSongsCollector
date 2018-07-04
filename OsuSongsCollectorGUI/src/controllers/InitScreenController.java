package controllers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.Main;
import application.OsuDbParser;
import application.SqliteDatabase;
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
						int folderCount = metadataRs.getInt(songsDb.Data.Metadata.FOLDER_COUNT);
						int numberOfBeatmaps = metadataRs.getInt(songsDb.Data.Metadata.NUMBER_OF_BEATMAPS);
						ResultSet configRs = songsDb.selectConfig();
						if (configRs.next()) {
							pathToOsuDb = configRs.getString(songsDb.Data.Config.PATH_TO_OSU_DB);
							pathToSongsFolder = configRs.getString(songsDb.Data.Config.PATH_TO_SONGS_FOLDER);
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
						this.loadSongsDisplayView();
					} catch (IOException e1) {
						e1.printStackTrace();
						Alert alert = new Alert(AlertType.ERROR, "Failed to load displaying screen", ButtonType.OK);
						alert.showAndWait();
					} catch (SQLException e1) {
						// this exception comes from initTableData method for populating the tableView
						e1.printStackTrace();
						Alert alert = new Alert(AlertType.ERROR, "Failed to retrieve table data from songs.db", ButtonType.OK);
						alert.showAndWait();
					}
				}
				else {
					try {
						this.loadUpdateDataView();
					} catch (IOException e1) {
						e1.printStackTrace();
						Alert alert = new Alert(AlertType.ERROR, "Failed to load update screen", ButtonType.OK);
						alert.showAndWait();
					}
				}
			});
			
			checkAllSetService.setOnFailed(e -> {
				checkAllSetService.getException().printStackTrace();
				Alert alert = new Alert(AlertType.ERROR, "Corrupted or interrupted when checking for songs added or deleted", ButtonType.OK);
				alert.showAndWait();
			});
			checkAllSetService.start();
		}
		// if not, prompt user for initialization
		else {
			try {
				this.loadSetSongsFolderPathView();
			}
			catch (IOException e) {
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR, "Failed to load initialization screen", ButtonType.OK);
				alert.showAndWait();
			}
		}
		
	}
	
	private void loadSongsDisplayView() throws IOException, SQLException {
		Stage songsDisplayStage = new Stage();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/SongsDisplayView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		Stage primaryStage = (Stage) this.welcomeLabel.getScene().getWindow();
		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
		
//		primaryStage.setScene(scene);
		songsDisplayStage.setTitle(primaryStage.getTitle());
		songsDisplayStage.setScene(scene);
		ctr.initData(songsDisplayStage, this.songsDb);
		songsDisplayStage.show();
		primaryStage.hide();
	}
	
	private void loadSetSongsFolderPathView() throws IOException {
		Stage setSongsFolderStage = new Stage();
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
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/UpdateDataView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		Stage primaryStage = (Stage) this.welcomeLabel.getScene().getWindow();
		UpdateDataController ctr = loader.<UpdateDataController>getController();
		
		updateDataStage.setTitle("Update Songs Data");
		updateDataStage.setScene(scene);
		// the last two paths must have already initialized to come to here
		ctr.initDataAndStart(updateDataStage, this.songsDb, this.pathToOsuDb, this.pathToSongsFolder);
		updateDataStage.setScene(scene);
		updateDataStage.show();
		primaryStage.hide();
	}
}
