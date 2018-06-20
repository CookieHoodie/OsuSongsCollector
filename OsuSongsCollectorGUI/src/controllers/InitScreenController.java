package controllers;

import java.io.IOException;
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
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class InitScreenController {
	@FXML private Label welcomeLabel;
	
	private SqliteDatabase songsDb;
	
	private class CheckOsuDbUpdateService extends Service<Boolean> {
		@Override
	    protected Task<Boolean> createTask() {
			return new Task<Boolean>() {
				@Override
	            protected Boolean call() throws Exception {
					// To come to here, songsDb must hv existed
					songsDb.connect();
					ResultSet rs = songsDb.selectMetadata();
					if (rs.next()) {
						int folderCount = rs.getInt("FolderCount");
						int numberOfBeatmaps = rs.getInt("NumberOfBeatmaps");
						String pathToOsuDb = rs.getString("PathToOsuDb");
						String pathToSongsFolder = rs.getString("PathToSongsFolder");
						OsuDbParser osuDb = new OsuDbParser(pathToOsuDb, pathToSongsFolder);
						osuDb.startParsingMetadataOnly();
						if (osuDb.getFolderCount() != folderCount || osuDb.getNumberOfBeatmaps() != numberOfBeatmaps) {
							return false;
						}
						return true;
					}
					else {
						throw new SQLException("Failed to retrieve metadata");
					}
	            }
	        };
	    }
	}
	
	public void startChecking() throws IOException {
		// 1st quick check if songs db is ald created (not created means 1st time use)
		this.songsDb = new SqliteDatabase(Main.DB_NAME);
		// if db exists, check for any new songs or deleted songs
		if (songsDb.isDbExist()) {
			CheckOsuDbUpdateService checkAllSetService = new CheckOsuDbUpdateService();
			// TODO: account for on failed and etc.
			checkAllSetService.setOnSucceeded(e -> {
				boolean isUpToDate = checkAllSetService.getValue();
				if (isUpToDate) {
					try {
						this.loadSongsDisplayView();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				else {
					FXMLLoader loader = new FXMLLoader();
					loader.setLocation(getClass().getResource("/fxml/UpdateDataView.fxml"));
					BorderPane root;
					try {
						root = loader.load();
						Scene scene = new Scene(root);
						Stage currentStage = (Stage) this.welcomeLabel.getScene().getWindow();
						currentStage.setScene(scene);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
			checkAllSetService.start();
		}
		// if not, prompt user for initialization
		else {
			this.loadSetSongsFolderPathView();
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
}
