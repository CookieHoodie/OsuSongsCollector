package controllers;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import application.Comparators;
import application.SqliteDatabase;
import application.ViewLoader;
import controllers.FilterDialogController.SimplifiedTableViewData;
import controllers.SongsDisplayController.TableViewData;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SaveToOptionController {
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private HostServices hostServices;
	public void setHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}
	
	public enum ComboBoxChoice {
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
	@FXML private Button duplicatedSongsCheckButton;
	@FXML private Label warningLabel;
	private Tooltip collisionTooltip;
	
	private Stage currentStage;
	private SqliteDatabase songsDb;
	private Map<String, List<TableViewData>> selectedSongsMap;
	private boolean isPathSet = false;
	private boolean isOptionsSet = false;
	private boolean useArtistNameUnicode;
	private boolean useSongTitleUnicode;
	
	// all initially from db, should not be changed
	private int configID;
	private String pathToSongsFolder = "";
	private String saveFolder = "";
	
	
	@FXML private void initialize() {
		ObservableList<ComboBoxChoice> prefixSuffixComboBoxObsList = FXCollections.observableArrayList(ComboBoxChoice.values());
		this.prefixComboBox.setItems(prefixSuffixComboBoxObsList);
		this.suffixComboBox.setItems(prefixSuffixComboBoxObsList);
		// invisible by default
		this.warningLabel.setText("Filename with only one attribute can result in numerous duplicates!");
		this.collisionTooltip = new Tooltip("When Source is empty, Artist(Unicode) will be used instead and finally Artist if Unicode is also empty."
				+ " This can result in filename with something like: 'ArtistName - ArtistName'.");
		this.collisionTooltip.getStyleClass().add("tooltip-design");
	}
	
	public void initData(Stage currentStage, SqliteDatabase songsDb, Map<String, List<TableViewData>> selectedSongsMap
			, boolean useArtistNameUnicode, boolean useSongTitleUnicode) throws SQLException {
		// assigning to member all data passed in
		this.songsDb = songsDb;
		this.currentStage = currentStage;
		this.selectedSongsMap = selectedSongsMap;
		// both are for dialog view naming purpose (if any)
		this.useArtistNameUnicode = useArtistNameUnicode;
		this.useSongTitleUnicode = useSongTitleUnicode;
		// get metadata from database to initialize some needed variables
		ResultSet rs = this.songsDb.selectConfig();
		if (rs.next()) {
			this.configID = rs.getInt(SqliteDatabase.TableData.Config.CONFIG_ID);
			this.saveFolder = rs.getString(SqliteDatabase.TableData.Config.SAVE_FOLDER);
			this.pathToSongsFolder = rs.getString(SqliteDatabase.TableData.Config.PATH_TO_SONGS_FOLDER);
			String comboBoxPrefix = rs.getString(SqliteDatabase.TableData.Config.COMBO_BOX_PREFIX);
			String comboBoxSuffix = rs.getString(SqliteDatabase.TableData.Config.COMBO_BOX_SUFFIX);
			if (!this.saveFolder.isEmpty()) {
				this.chosenPathTextField.setText(this.saveFolder);
				this.rememberPathCheckBox.setSelected(true);
				this.isPathSet = true;
				this.setStartButtonDisability();
			}
			
			boolean isPrefixSet = false;
			boolean isSuffixSet = false;
			// select the values
			for (ComboBoxChoice cbc : ComboBoxChoice.values()) {
				if (cbc.toString().equals(comboBoxPrefix)) {
					this.prefixComboBox.getSelectionModel().select(cbc);
					isPrefixSet = true;
					if (isPrefixSet && isSuffixSet) {
						break;
					}
				}
				
				if (cbc.toString().equals(comboBoxSuffix)) {
					this.suffixComboBox.getSelectionModel().select(cbc);
					isSuffixSet = true;
					if (isPrefixSet && isSuffixSet) {
						break;
					}
				}
			}
			// unlikely but juz in case set them to none
			if (!isPrefixSet || !isSuffixSet) {
				this.prefixComboBox.getSelectionModel().select(ComboBoxChoice.NONE);
				this.suffixComboBox.getSelectionModel().select(ComboBoxChoice.NONE);
			}
			// render the sample in textField and to enable the start button if already all set
			this.renderSample();
			
		}
		// shouldn't be the case
		else {
			throw new SQLException("No config available?");
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
	@FXML private void renderSample() {
		ComboBoxChoice prefix = this.prefixComboBox.getSelectionModel().getSelectedItem();
		ComboBoxChoice suffix = this.suffixComboBox.getSelectionModel().getSelectedItem();
		
		this.sampleTextField.setTooltip(null); // remove tooptip if any
		this.isOptionsSet = false;
		boolean setWarningLabelVisible = false;
		if (prefix == suffix) {
			if (prefix != ComboBoxChoice.NONE) {
				this.sampleTextField.setText("Attributes can't be the same!");
			}
			// both are none
			else {
				this.sampleTextField.setText("Choose your filename format below:");
			}
		}
		else if (((prefix == ComboBoxChoice.ARTIST_NAME || prefix == ComboBoxChoice.ARTIST_NAME_UNICODE) && (suffix == ComboBoxChoice.ARTIST_NAME || suffix == ComboBoxChoice.ARTIST_NAME_UNICODE))
				|| ((prefix == ComboBoxChoice.SONG_TITLE || prefix == ComboBoxChoice.SONG_TITLE_UNICODE) && (suffix == ComboBoxChoice.SONG_TITLE || suffix == ComboBoxChoice.SONG_TITLE_UNICODE))) {
			this.sampleTextField.setText("Similar attributes is not allowed!");
		}
		else if ((prefix == ComboBoxChoice.SONG_SOURCE || suffix == ComboBoxChoice.SONG_SOURCE) &&
				 (prefix == ComboBoxChoice.ARTIST_NAME || prefix == ComboBoxChoice.ARTIST_NAME_UNICODE
				|| suffix == ComboBoxChoice.ARTIST_NAME || suffix == ComboBoxChoice.ARTIST_NAME_UNICODE)) {
			this.sampleTextField.setText("Possible attribute collision �");
			this.sampleTextField.setTooltip(this.collisionTooltip);
		}
		else {
			// either one is empty but not both
			if (prefix == ComboBoxChoice.NONE || suffix == ComboBoxChoice.NONE) {
				setWarningLabelVisible = true;
				this.sampleTextField.setText(prefix.getSample() + suffix.getSample());
			}
			else {
				this.sampleTextField.setText(prefix.getSample() + " - " + suffix.getSample());
			}
			this.isOptionsSet = true;
			
		}
		// this is only for when either one box is None
		this.warningLabel.setVisible(setWarningLabelVisible);
		
		this.setStartButtonDisability();
	}
	
	// start Button
	@FXML private void startCopying(ActionEvent event) {
		ComboBoxChoice prefix = this.prefixComboBox.getSelectionModel().getSelectedItem();
		ComboBoxChoice suffix = this.suffixComboBox.getSelectionModel().getSelectedItem();
		
		try {
			// save saveFolder if specified
			if ((this.rememberPathCheckBox.isSelected() && !this.chosenPathTextField.getText().equals(this.saveFolder))) {
				String[] items = {SqliteDatabase.TableData.Config.SAVE_FOLDER, SqliteDatabase.TableData.Config.COMBO_BOX_PREFIX
						, SqliteDatabase.TableData.Config.COMBO_BOX_SUFFIX};
				String[] results = {this.chosenPathTextField.getText(), prefix.toString(), suffix.toString()};
				this.songsDb.updateConfigString(this.configID, items, results);
			}
			// otherwise, store the comboBoxChoice anyway
			else {
				String[] items = {SqliteDatabase.TableData.Config.COMBO_BOX_PREFIX, SqliteDatabase.TableData.Config.COMBO_BOX_SUFFIX};
				String[] results = {prefix.toString(), suffix.toString()};
				this.songsDb.updateConfigString(this.configID, items, results);
			}
		}
		// runtimeException & SQLException should show similar result to user
		catch (Exception e) {
			logger.log(Level.WARNING, "Failed to save chosen path to songs.db", e);
			Alert alert = new Alert(AlertType.ERROR, "Failed to remember chosen path", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.show();
		}
		
		
		List<TableViewData> selectedSongsList = this.selectedSongsMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
		
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/fxml/CopySongsView.fxml"));
			StackPane root = loader.load();
			Scene scene = new Scene(root);
			CopySongsController ctr = loader.<CopySongsController>getController();
			ctr.setHostServices(this.hostServices);
			ctr.initDataAndStart(this.currentStage, this.songsDb, selectedSongsList, this.pathToSongsFolder, this.chosenPathTextField.getText(), prefix, suffix);
			this.currentStage.setTitle("Collect songs");
			this.currentStage.setScene(scene);
			this.currentStage.setResizable(true);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to load CopySongsView", e);
			Alert alert = new Alert(AlertType.ERROR, "Failed to load collect songs window", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
	}
	
	// TODO: might consider adding option for user to choose all duplicated Name instead of examining the totalTIme
	// and option for user to choose longest, shortest, etc.
	@FXML private void checkForDuplicatedSongs(ActionEvent event) {
		// add data into map with Song as key
		// if similar song, the value (list) size will be > 1
		Map<Song, List<TableViewData>> duplicatedMap = new HashMap<>();
		for (List<TableViewData> rowList : this.selectedSongsMap.values()) {
			if (rowList.size() == 1) {
				TableViewData row = rowList.get(0);
				Song s = new Song(row.artistNameProperty().get().toLowerCase(), row.songTitleProperty().get().toLowerCase());
				List<TableViewData> duplicatedRows = duplicatedMap.get(s);
				// if the key doesn't exist
				if (duplicatedRows == null) {
					duplicatedRows = new ArrayList<TableViewData>();
					duplicatedRows.add(row);
					duplicatedMap.put(s, duplicatedRows);
				}
				else {
					duplicatedRows.add(row);
				}
			}
		}
		Comparator<TableViewData> totalTimeComparator = new Comparators.TotalTimeComparator();
		// from dialogView
		ObservableList<SimplifiedTableViewData> dialogObsList = FXCollections.observableArrayList(SimplifiedTableViewData.extractor());
		final int range = 15000;  // in range of 15 seconds
		for (List<TableViewData> r : duplicatedMap.values()) {
			if (r.size() > 1) {
				// sort for comparison later
				r.sort(totalTimeComparator);
				List<TableViewData> l = new ArrayList<TableViewData>();
				for (int i = 1; i < r.size(); i++) {
					TableViewData previousRow = r.get(i - 1);
					TableViewData currentRow = r.get(i);
					
					int currentTime = currentRow.totalTimeProperty().get();
					int previousTime = previousRow.totalTimeProperty().get();
					if (Math.abs(currentTime - previousTime) < range) {
						// if the streak breaks, we need to add both rows if duplicated
						if (l.isEmpty()) {
							l.add(previousRow);
						}
						// separate if same name but two groups of different length (ie. 1:30, 1:30 --- 4:30, 4:30)
						else if (!previousRow.equals(l.get(l.size() - 1))) {
							TableViewData lastRow = l.get(l.size() - 1);
							if (Math.abs(previousTime - lastRow.totalTimeProperty().get()) >= range) {
								l.add(null);
							}
							l.add(previousRow);
						}
						l.add(currentRow);
					}
				}
				if (!l.isEmpty()) {
					for (TableViewData row : l) {
						if (row == null) {
							// add empty row
							dialogObsList.add(new SimplifiedTableViewData());
							continue;
						}
						String name;
						String artistName = this.useArtistNameUnicode && !row.artistNameUnicodeProperty().get().isEmpty() ? row.artistNameUnicodeProperty().get() : row.artistNameProperty().get();
						String songTitle = this.useSongTitleUnicode && !row.songTitleUnicodeProperty().get().isEmpty() ? row.songTitleUnicodeProperty().get() : row.songTitleProperty().get();
						name = artistName + " - " + songTitle;
						SimplifiedTableViewData data = new SimplifiedTableViewData(name, TableViewData.totalTimeToString(row.totalTimeProperty().get()), row.isSelectedProperty().get(), row.folderNameProperty().get());
						dialogObsList.add(data);
					}
					
					dialogObsList.add(new SimplifiedTableViewData());
				}
			}
		}
		
		if (dialogObsList.isEmpty()) {
			Alert alert = new Alert(AlertType.INFORMATION, "No duplicate is found!", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
		else {
			// remove last empty row added
			dialogObsList.remove(dialogObsList.size() - 1, dialogObsList.size());
			try {
				this.loadFilterDialogView(dialogObsList);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to load FilterDialogView", e);
				Alert alert = new Alert(AlertType.ERROR, "Failed to load duplicate check window", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
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
	
	private void loadFilterDialogView(ObservableList<SimplifiedTableViewData> dialogObsList) throws IOException {
		Stage dialogStage = new Stage();
		dialogStage.setTitle("Duplicate check");
		dialogStage.initModality(Modality.WINDOW_MODAL);
		dialogStage.initOwner(this.currentStage);
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/FilterDialogView.fxml"));
		StackPane root = loader.load();
		Scene scene = new Scene(root);
		FilterDialogController ctr = loader.<FilterDialogController>getController();
		ctr.initData(this.selectedSongsMap, dialogObsList);
		dialogStage.setScene(scene);
		dialogStage.show();
	}
	
	
	private class Song {
		private final String artistName;
		private final String songTitle;
		
		public Song(String artistName, String songTitle) {
			this.artistName = artistName;
			this.songTitle = songTitle;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((artistName == null) ? 0 : artistName.hashCode());
			result = prime * result + ((songTitle == null) ? 0 : songTitle.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Song other = (Song) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (artistName == null) {
				if (other.artistName != null)
					return false;
			} else if (!artistName.equals(other.artistName))
				return false;
			if (songTitle == null) {
				if (other.songTitle != null)
					return false;
			} else if (!songTitle.equals(other.songTitle))
				return false;
			return true;
		}

		private SaveToOptionController getOuterType() {
			return SaveToOptionController.this;
		}
	}

}
