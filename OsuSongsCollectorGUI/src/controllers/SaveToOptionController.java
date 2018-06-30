package controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

import application.Main;
import application.OsuDbParser;
import application.SqliteDatabase;
import controllers.SongsDisplayController.TableViewData;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SaveToOptionController {
	private enum ComboBoxChoice {
		NONE("None", ""),
		SONG_SOURCE("Source", "やはり俺の青春ラブコメはまちがっている。続"),
		ARTIST_NAME("Artist", "yanaginagi"),
		ARTIST_NAME_UNICODE("Artist (Unicode)", "やなぎなぎ"),
		SONG_TITLE("Song Title", "Haru Modoki"),
		SONG_TITLE_UNICODE("Song Title (Unicode)", "春擬き");
		
		private final String text;
		private final String sample;
		private ComboBoxChoice(String text, String sample) {
			this.text = text;
			this.sample = sample;
		}
		
		@Override public String toString() {
			return this.text;
		}
		
		public String getSample() {
			return this.sample;
		}
	}
	
	@FXML private ComboBox<ComboBoxChoice> prefixComboBox;
	@FXML private ComboBox<ComboBoxChoice> suffixComboBox;
	
	@FXML private Label instructionLabel;
	@FXML private TextField chosenPathTextField;
	@FXML private Button choosePathButton;
	@FXML private CheckBox rememberPathCheckBox;
	@FXML private Button startButton;
	@FXML private TextField sampleTextField;
//	@FXML private ProgressBar downloadProgressBar;
//	@FXML private Button cancelButton;
//	@FXML private TextArea taskDetailsTextArea;
	
	private Stage currentStage;
	private SqliteDatabase songsDb;
	private List<TableViewData> selectedSongsList;
	private boolean isPathSet = false;
	private boolean isOptionsSet = true;
	
	// all initially from db, should not be changed
	private int configID;
	private String pathToSongsFolder = "";
	private String saveFolder = "";
	
//	private Task<Void> task;
	
	// TODO: unselect all checked checkbox after closing stage
	// TODO: might want to move the progressBar and textField into new scene
	// TODO: add option to remove or hide duplicated by examining artist, songTitle, totalTime, and audioName
	// TODO: add option to not download downloaded songs, otherwise, warning
	
	
	@FXML private void initialize() {
		// TODO: might want to save to db as well
		// in that case, remember to change the isOptionsSet also
		ObservableList<ComboBoxChoice> prefixSuffixComboBoxObsList = FXCollections.observableArrayList(ComboBoxChoice.values());
		this.prefixComboBox.setItems(prefixSuffixComboBoxObsList);
		this.prefixComboBox.getSelectionModel().select(ComboBoxChoice.ARTIST_NAME);
		this.suffixComboBox.setItems(prefixSuffixComboBoxObsList);
		this.suffixComboBox.getSelectionModel().select(ComboBoxChoice.SONG_TITLE);
//		this.taskDetailsTextArea.setText(this.prefixComboBox.getSelectionModel().getSelectedItem().getSample() + " - " + this.suffixComboBox.getSelectionModel().getSelectedItem().getSample());
		this.sampleTextField.setText(this.prefixComboBox.getSelectionModel().getSelectedItem().getSample() + " - " + this.suffixComboBox.getSelectionModel().getSelectedItem().getSample());
	}
	
	public void initData(Stage currentStage, SqliteDatabase songsDb, List<TableViewData> selectedSongsList) throws SQLException {
		// assigning to member all data passed in
		this.songsDb = songsDb;
		this.currentStage = currentStage;
		this.selectedSongsList = selectedSongsList;
		// get metadata from database to initialize some needed variables
		ResultSet rs = this.songsDb.selectConfig();
		if (rs.next()) {
			this.configID = rs.getInt(this.songsDb.Data.Config.CONFIG_ID);
			this.saveFolder = rs.getString(this.songsDb.Data.Config.SAVE_FOLDER);
			this.pathToSongsFolder = rs.getString(this.songsDb.Data.Config.PATH_TO_SONGS_FOLDER);
			if (!this.saveFolder.isEmpty()) {
				this.chosenPathTextField.setText(this.saveFolder);
				this.rememberPathCheckBox.setSelected(true);
				this.isPathSet = true;
				this.setStartButtonDisability();
			}
		}
		// shouldn't be the case
		else {
			throw new SQLException("No metadata available?");
		}
		
		
	}
	
	// choosePath Button
	@FXML private void promptToChoosePath(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(this.currentStage);
          
        if (selectedDirectory == null) {
    		this.chosenPathTextField.setText("No path selected");
    		this.isPathSet = false;
        }
        else {
    		this.chosenPathTextField.setText(selectedDirectory.getAbsolutePath());
    		this.isPathSet = true;
        }
        
        this.setStartButtonDisability();
	}
	
	// comboBoxOnSelected
	@FXML private void renderSample(ActionEvent event) {
		ComboBoxChoice prefix = this.prefixComboBox.getSelectionModel().getSelectedItem();
		ComboBoxChoice suffix = this.suffixComboBox.getSelectionModel().getSelectedItem();
		
		this.isOptionsSet = false;
		if (prefix == suffix) {
			if (prefix != ComboBoxChoice.NONE) {
				this.sampleTextField.setText("Attributes can't be the same!");
			}
			else {
				this.sampleTextField.setText("No attribute is chosen!");
			}
		}
		else if (((prefix == ComboBoxChoice.ARTIST_NAME || prefix == ComboBoxChoice.ARTIST_NAME_UNICODE) && (suffix == ComboBoxChoice.ARTIST_NAME || suffix == ComboBoxChoice.ARTIST_NAME_UNICODE))
				|| ((prefix == ComboBoxChoice.SONG_TITLE || prefix == ComboBoxChoice.SONG_TITLE_UNICODE) && (suffix == ComboBoxChoice.SONG_TITLE || suffix == ComboBoxChoice.SONG_TITLE_UNICODE))) {
			this.sampleTextField.setText("Similar attributes is not allowed!");
		}
		else {
			if (prefix == ComboBoxChoice.NONE || suffix == ComboBoxChoice.NONE) {
				this.sampleTextField.setText(prefix.getSample() + suffix.getSample());
			}
			else {
				this.sampleTextField.setText(prefix.getSample() + " - " + suffix.getSample());
			}
			this.isOptionsSet = true;
		}
		
		this.setStartButtonDisability();
	}
	
	// start Button
	@FXML private void startCopying(ActionEvent event) {
		if (this.rememberPathCheckBox.isSelected() && !this.chosenPathTextField.getText().equals(this.saveFolder)) {
			String[] items = {this.songsDb.Data.Config.SAVE_FOLDER};
			String[] results = {this.chosenPathTextField.getText()};
			try {
				this.songsDb.updateConfigString(this.configID, items, results);
			}
			// runtimeException & SQLException should show similar result to user
			catch (Exception e) {
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR, "Failed to remember chosen path", ButtonType.OK);
				alert.show();
			}
		}
		
		ComboBoxChoice prefix = this.prefixComboBox.getSelectionModel().getSelectedItem();
		ComboBoxChoice suffix = this.suffixComboBox.getSelectionModel().getSelectedItem();
		Task<Void> copySongsTask = new CopySongsTask(this.selectedSongsList, this.pathToSongsFolder, this.chosenPathTextField.getText(), prefix, suffix); 
		
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/fxml/CopySongsView.fxml"));
			BorderPane root = loader.load();
			Scene scene = new Scene(root);
			CopySongsController ctr = loader.<CopySongsController>getController();
			ctr.initDataAndStart(copySongsTask);
			this.currentStage.setScene(scene);
		}
		catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to load copy songs screen", ButtonType.OK);
			alert.showAndWait();
		}
	}
	
	private void setStartButtonDisability() {
		if (this.isPathSet && this.isOptionsSet) {
			this.startButton.setDisable(false);
		}
		else {
			this.startButton.setDisable(true);
			this.startButton.requestFocus();
		}
	}
	
	private class CopySongsTask extends Task<Void> {
		private final String pathToSongsFolderInTask;
		private final String destinationFolder;
		private final ComboBoxChoice prefix;
		private final ComboBoxChoice suffix;
		private final List<TableViewData> selectedSongsListInTask;
		
		public CopySongsTask(List<TableViewData> selectedSongsList, String pathToSongsFolder, String destinationFolder, ComboBoxChoice prefix, ComboBoxChoice suffix) {
			this.selectedSongsListInTask = selectedSongsList;
			this.pathToSongsFolderInTask = pathToSongsFolder;
			this.destinationFolder = destinationFolder;
			this.prefix = prefix;
			this.suffix = suffix;
		}
		
		private String formatFileName(String prefix, String suffix, String audioName, int occurance) {
			String separator = prefix == "" || suffix == "" ? "" : " - ";
			if (occurance == 0) {
				return prefix.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + separator + suffix.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + audioName.substring(audioName.lastIndexOf('.'));
			}
			else {
				return prefix.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + separator + suffix.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + " (" + occurance + ")" + audioName.substring(audioName.lastIndexOf('.'));
			}
		}
		
		private String getFileNamePrefix(TableViewData row) {
			String fileNamePrefix;
			// if Source is empty, cascade to artistNameUnicode and finally artistName if still empty
			// if songTitleUnicode is empty, cascade to songTitle only
			switch (this.prefix) {
				case NONE: {
					fileNamePrefix = "";
					break;
				}
				case SONG_SOURCE: {
					fileNamePrefix = row.songSourceProperty().get();
					if (!fileNamePrefix.isEmpty()) {
						break;
					}
				}
				case ARTIST_NAME_UNICODE: {
					fileNamePrefix = row.artistNameUnicodeProperty().get();
					if (!fileNamePrefix.isEmpty()) {
						break;
					}
				}
				case ARTIST_NAME: {
					fileNamePrefix = row.artistNameProperty().get();
					break;
				}
				case SONG_TITLE_UNICODE: {
					fileNamePrefix = row.songTitleProperty().get();
					if (!fileNamePrefix.isEmpty()) {
						break;
					}
				}
				case SONG_TITLE: {
					fileNamePrefix = row.songTitleProperty().get();
					break;
				}
				
				default: throw new RuntimeException("Invalid options");
			}
			return fileNamePrefix;
		}
		
		private String getFileNameSuffix(TableViewData row) {
			String fileNameSuffix;
			switch (this.suffix) {
				case NONE: {
					fileNameSuffix = "";
					break;
				}
				case SONG_SOURCE: {
					fileNameSuffix = row.songSourceProperty().get();
					if (!fileNameSuffix.isEmpty()) {
						break;
					}
				}
				case ARTIST_NAME_UNICODE: {
					fileNameSuffix = row.artistNameUnicodeProperty().get();
					if (!fileNameSuffix.isEmpty()) {
						break;
					}
				}
				case ARTIST_NAME: {
					fileNameSuffix = row.artistNameProperty().get();
					break;
				}
				case SONG_TITLE_UNICODE: {
					fileNameSuffix = row.songTitleProperty().get();
					if (!fileNameSuffix.isEmpty()) {
						break;
					}
				}
				case SONG_TITLE: {
					fileNameSuffix = row.songTitleProperty().get();
					break;
				}
				default: throw new RuntimeException("Invalid filename options");
			}
			return fileNameSuffix;
		}
		
		@Override
        protected Void call() throws Exception {
//			int currentProgress = 0;
//			boolean isCompleted = true;
			updateProgress(0, 0);
			int totalProgress = this.selectedSongsListInTask.size();
			String[] items = {songsDb.Data.BeatmapSet.IS_DOWNLOADED};
			Boolean[] results = {true};
			try {
				songsDb.getConn().setAutoCommit(false);
				PreparedStatement updateBeatmapSetBooleanPStatement = songsDb.getUpdateBeatmapSetBooleanPreparedStatement(items);
				for (int i = 0; i < totalProgress; i++) {
					if (!isCancelled()) {
						TableViewData row = this.selectedSongsListInTask.get(i);
						int beatmapSetAutoID = row.beatmapSetAutoIDProperty().get();
						Path oriPath = Paths.get(this.pathToSongsFolderInTask, row.folderNameProperty().get(), row.audioNameProperty().get());
						// TODO: if unicode, use english if empty
						// warn user if they change the order of the filename as old files does not recognize the previous one
						int occurance = 0;
						String fileNamePrefix = this.getFileNamePrefix(row);
						String fileNameSuffix = this.getFileNameSuffix(row);
						String fileName = this.formatFileName(fileNamePrefix, fileNameSuffix, row.audioNameProperty().get(), occurance);
						
						Path cpPath = Paths.get(this.destinationFolder, fileName);
						File cpFile = cpPath.toFile();
						if (cpFile.exists()) {
							fileNameSuffix += " (" + TableViewData.totalTimeToString(row.totalTimeProperty().get()).replace(":", "'") + ")";
							fileName = this.formatFileName(fileNamePrefix, fileNameSuffix, row.audioNameProperty().get(), occurance);
							cpPath = Paths.get(this.destinationFolder, fileName);
							cpFile = cpPath.toFile();
							while (cpFile.exists()) {
								occurance++;
								fileName = this.formatFileName(fileNamePrefix, fileNameSuffix, row.audioNameProperty().get(), occurance);
								cpPath = Paths.get(this.destinationFolder, fileName);
								cpFile = cpPath.toFile();
							}
						}
						
						try {
							Files.copy(oriPath, cpPath);
						}
						catch (UnsupportedOperationException | IOException | SecurityException e) {
							updateBeatmapSetBooleanPStatement.executeBatch();
							songsDb.getConn().commit();
							throw e;
						}
						songsDb.addUpdateBeatmapSetBatch(updateBeatmapSetBooleanPStatement, beatmapSetAutoID, results);
						Platform.runLater(() -> {
							row.isDownloadedProperty().set(true);
							row.isSelectedProperty().set(false);
						}); 
						updateProgress(i + 1, totalProgress);
						updateMessage("(" + i + 1 + "/" + totalProgress + ") " + fileName);
					}
					else {
						updateMessage((i+1) + " songs are copied, " + (totalProgress-i-1) + " are cancelled.");
						break;
					}
				}
				updateBeatmapSetBooleanPStatement.executeBatch();
				songsDb.getConn().commit();
			}
			finally {
				songsDb.getConn().setAutoCommit(true);
			}
			return null;
        }
    }
	
}
