package controllers;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import application.Comparators;
import application.Comparators.SongTitleComparator;
import application.SqliteDatabase;
import controllers.SongsDisplayController.TableViewData;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

public class SongsDisplayController {
	private SqliteDatabase songsDb;
	private ObservableList<TableViewData> initSongsObsList;
	private FilteredList<TableViewData> initSongsFilteredList;
	private SortedList<TableViewData> initSongsSortedList;
	
	@FXML private TableView<TableViewData> testTable;
	@FXML private TableColumn<TableViewData, String> songSourceCol;
	@FXML private TableColumn<TableViewData, String> artistNameCol;
	@FXML private TableColumn<TableViewData, String> artistNameUnicodeCol;
	@FXML private TableColumn<TableViewData, String> songTitleCol;
	@FXML private TableColumn<TableViewData, String> songTitleUnicodeCol;
	@FXML private TableColumn<TableViewData, String> creatorNameCol;
	@FXML private TableColumn<TableViewData, Integer> totalTimeCol;
	@FXML private TableColumn<TableViewData, Boolean> checkBoxCol;
	
	@FXML private TextField testSearchText;
	@FXML private Button testCopySongButton;
	@FXML private Button hideUnhideButton;
	@FXML private CheckBox selectAllCheckBoxInCheckBoxCol;
	@FXML private Label numOfSelectedSongsLabel;
	@FXML private ComboBox<Comparator<TableViewData>> orderByComboBox;
	
	@FXML private Menu fileMenu;
	@FXML private MenuItem checkNewSongsFileMenuItem;
	@FXML private MenuItem resetAllFileMenuItem;
	@FXML private MenuItem exitFileMenuItem;
	
	@FXML private Menu viewMenu;
	@FXML private Menu showMenuInViewMenu;
	@FXML private CheckMenuItem songSourceShowCheckMenuItem;
	@FXML private CheckMenuItem artistNameShowCheckMenuItem;
	@FXML private CheckMenuItem artistNameUnicodeShowCheckMenuItem;
	@FXML private CheckMenuItem songTitleShowCheckMenuItem;
	@FXML private CheckMenuItem songTitleUnicodeShowCheckMenuItem;
	@FXML private CheckMenuItem creatorNameShowCheckMenuItem;
	@FXML private CheckMenuItem totalTimeShowCheckMenuItem;
	@FXML private CheckMenuItem isDownloadedShowCheckMenuItem;
	@FXML private Menu displayMenuInViewMenu;
	@FXML private ToggleGroup displayToggleGroup;
	@FXML private RadioMenuItem unhiddenSongsRadioMenuItemInDisplayMenu;
	@FXML private RadioMenuItem hiddenSongsRadioMenuItemInDisplayMenu;
	@FXML private RadioMenuItem downloadedSongsRadioMenuItemInDisplayMenu;
	
	@FXML private Menu helpMenu;
	@FXML private MenuItem aboutHelpMenuItem;
	
	private final String numOfSelectedSongsLabelText = "Selected: ";
	private int isSelectedCounter = 0;
	
	// TODO: save those (and maybe saveFolder) in a different table Configuration
	// and check the corresponding items in controller instead of fxml
	// TODO: set help text on approx length
	// TODO: add rotating screen while changing view, searching, etc.
	// TODO: allow user to select and copy words but not edit
	// TODO: search length < and > in search bar
	
	@FXML private void initialize() {
		this.songSourceCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("songSource"));
        this.artistNameCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("artistName"));
        this.artistNameUnicodeCol.setCellValueFactory(value -> {
        	if (value.getValue().artistNameUnicodeProperty().get().isEmpty() && !this.artistNameCol.isVisible()) {
        		return value.getValue().artistNameProperty();
        	}
        	return value.getValue().artistNameUnicodeProperty();
        });
        this.songTitleCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("songTitle"));
        this.songTitleUnicodeCol.setCellValueFactory(value -> {
        	if (value.getValue().songTitleUnicodeProperty().get().isEmpty() && !this.songTitleCol.isVisible()) {
        		return value.getValue().songTitleProperty();
        	}
        	return value.getValue().songTitleUnicodeProperty();
        });
        this.creatorNameCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("creatorName"));
        this.totalTimeCol.setCellValueFactory(new PropertyValueFactory<TableViewData, Integer>("totalTime"));
        this.totalTimeCol.setCellFactory(value -> {
        	return new TableCell<TableViewData, Integer>() {
        		@Override protected void updateItem(Integer totalTime, boolean empty) {
        			super.updateItem(totalTime, empty);
					setText(totalTime == null ? "" : TableViewData.totalTimeToString(totalTime));
        		}
        	};
        });
        this.checkBoxCol.setCellValueFactory(new PropertyValueFactory<TableViewData, Boolean>("isSelected"));
        this.checkBoxCol.setCellFactory(tc -> new CheckBoxTableCell<>());
//        this.checkBoxCol.setCellFactory(tc -> new CustomCheckBoxCell());
//        this.testTable.getSelectionModel().selectedItemProperty().addListener((obs, wasSelected, isSelected) -> {
//        	System.out.println(obs);
//        });
        this.testTable.setRowFactory(value -> {
        	return new TableRow<TableViewData>() {
        		@Override protected void updateItem(TableViewData item, boolean empty) {
        			super.updateItem(item, empty);
    				if (item != null && item.isDownloadedProperty().get()) {
        				setStyle("-fx-background-color:#c0bcf2");
        			}
        			else {
        				setStyle("");
        			}
        		}
        		
        	};
        });
        
        Comparator<TableViewData> lastModificationTimeComparator = new Comparators.LastModificationTimeComparator();
        ObservableList<Comparator<TableViewData>> orderByComparatorsObsList = FXCollections.observableArrayList(
        		new Comparators.SongTitleComparator(),
        		new Comparators.ArtistNameComparator(),
        		new Comparators.CreatorNameComparator(),
        		new Comparators.TotalTimeComparator(),
        		lastModificationTimeComparator
        );
        this.orderByComboBox.setItems(orderByComparatorsObsList);
        this.orderByComboBox.getSelectionModel().select(lastModificationTimeComparator);
        
        this.songSourceShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.songSourceCol.setVisible(true);
        	}
        	else {
        		this.songSourceCol.setVisible(false);
        	}
        });
        
        this.artistNameShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.artistNameCol.setVisible(true);
        	}
        	else {
        		this.artistNameCol.setVisible(false);
        	}
        });
        
        this.artistNameUnicodeShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.artistNameUnicodeCol.setVisible(true);
        	}
        	else {
        		this.artistNameUnicodeCol.setVisible(false);
        	}
        });
        
        this.songTitleShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.songTitleCol.setVisible(true);
        	}
        	else {
        		this.songTitleCol.setVisible(false);
        	}
        });
        
        this.songTitleUnicodeShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.songTitleUnicodeCol.setVisible(true);
        	}
        	else {
        		this.songTitleUnicodeCol.setVisible(false);
        	}
        });
        
        this.creatorNameShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.creatorNameCol.setVisible(true);
        	}
        	else {
        		this.creatorNameCol.setVisible(false);
        	}
        });
        
        this.totalTimeShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.totalTimeCol.setVisible(true);
        	}
        	else {
        		this.totalTimeCol.setVisible(false);
        	}
        });
        
        this.isDownloadedShowCheckMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	// throw error if filteredList is not initialized as the list is needed to perform operation here
        	if (this.initSongsFilteredList == null) {
        		throw new RuntimeException("MenuItem is modified before GUI is shown");
        	}
        	
        	this.initSongsFilteredList.setPredicate(new CustomPredicate(this.testSearchText.getText()));
        });
        
        // set selected items to 0
        this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + this.isSelectedCounter);
        
        // selectAll
        this.selectAllCheckBoxInCheckBoxCol.selectedProperty().addListener((obs, oldValue, newValue) -> {
        	if (newValue) {
        		this.testTable.getItems().forEach(row -> {
    				if (!row.isSelectedProperty().get()) {
    					row.isSelectedProperty().set(true);
    				}
    			});
        	}
        	else {
        		this.testTable.getItems().forEach(row -> {
        			if (row.isSelectedProperty().get()) {
        				row.isSelectedProperty().set(false);
        			}
        		});
        	}
        });
	}
	
	public void initData(Stage currentStage, SqliteDatabase connectedSongsDb) throws SQLException {
		this.songsDb = connectedSongsDb;
		currentStage.setOnCloseRequest(e -> {
			try {
				this.songsDb.closeConnection();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		});
		this.initTableView();
	}
	
	
	private void initTableView() throws SQLException {
        ResultSet tableInitDataRs = this.songsDb.getTableInitData();
       
        ObservableList<TableViewData> initSongsObsList = FXCollections.observableArrayList(TableViewData.extractor());
        while (tableInitDataRs.next()) {
//        	data.add(new TableViewData(
//        			tableInitDataRs.getInt(this.songsDb.Data.BeatmapSet.BEATMAP_SET_AUTO_ID)
//					, tableInitDataRs.getString(this.songsDb.Data.Song.SONG_SOURCE)
//        			, tableInitDataRs.getString(this.songsDb.Data.Artist.ARTIST_NAME)
//        			, tableInitDataRs.getString(this.songsDb.Data.Artist.ARTIST_NAME_UNICODE)
//        			, tableInitDataRs.getString(this.songsDb.Data.Song.SONG_TITLE)
//        			, tableInitDataRs.getString(this.songsDb.Data.Song.SONG_TITLE_UNICODE)
//        			, tableInitDataRs.getInt(this.songsDb.Data.Beatmap.TOTAL_TIME)
//        			, tableInitDataRs.getLong(this.songsDb.Data.Beatmap.LAST_MODIFICATION_TIME)
//        			, tableInitDataRs.getBoolean(this.songsDb.Data.BeatmapSet.IS_DOWNLOADED)
//        			, tableInitDataRs.getBoolean(this.songsDb.Data.BeatmapSet.IS_HIDDEN)
//        			, false
//        			, tableInitDataRs.getString(this.songsDb.Data.BeatmapSet.FOLDER_NAME)
//        			, tableInitDataRs.getString(this.songsDb.Data.BeatmapSet.AUDIO_NAME)
//        			, tableInitDataRs.getString(this.songsDb.Data.SongTag.SONG_TAG_NAME)
//        			));
        	TableViewData t = new TableViewData(
        			tableInitDataRs.getInt(this.songsDb.Data.BeatmapSet.BEATMAP_SET_AUTO_ID)
					, tableInitDataRs.getString(this.songsDb.Data.Song.SONG_SOURCE)
        			, tableInitDataRs.getString(this.songsDb.Data.Artist.ARTIST_NAME)
        			, tableInitDataRs.getString(this.songsDb.Data.Artist.ARTIST_NAME_UNICODE)
        			, tableInitDataRs.getString(this.songsDb.Data.Song.SONG_TITLE)
        			, tableInitDataRs.getString(this.songsDb.Data.Song.SONG_TITLE_UNICODE)
        			, tableInitDataRs.getInt(this.songsDb.Data.Beatmap.TOTAL_TIME)
        			, tableInitDataRs.getLong(this.songsDb.Data.Beatmap.LAST_MODIFICATION_TIME)
        			, tableInitDataRs.getBoolean(this.songsDb.Data.BeatmapSet.IS_DOWNLOADED)
        			, tableInitDataRs.getBoolean(this.songsDb.Data.BeatmapSet.IS_HIDDEN)
        			, false
        			, tableInitDataRs.getString(this.songsDb.Data.BeatmapSet.FOLDER_NAME)
        			, tableInitDataRs.getString(this.songsDb.Data.BeatmapSet.AUDIO_NAME)
        			, tableInitDataRs.getString(this.songsDb.Data.SongTag.SONG_TAG_NAME).replaceAll(",", " ")
        			, tableInitDataRs.getString(this.songsDb.Data.BeatmapSet.CREATOR_NAME)
        			);
        	// TODO: this is flawful! change this implementation alltogether
        	t.isSelectedProperty().addListener((obs, oldValue, newValue) -> {
        		if (newValue) {
        			this.isSelectedCounter++;
        		}
        		else {
        			this.isSelectedCounter--;
        		}
        		this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + this.isSelectedCounter);
        	});
        	initSongsObsList.add(t);

        }
        FilteredList<TableViewData> initSongsFilteredList = new FilteredList<TableViewData>(initSongsObsList, new CustomPredicate(""));
        PauseTransition pause = new PauseTransition(Duration.millis(180));
        this.testSearchText.textProperty().addListener((obs, oldValue, newValue) -> {
        	pause.setOnFinished(event -> {
        		initSongsFilteredList.setPredicate(new CustomPredicate(newValue));
        	});
            pause.playFromStart();
        });
        
        SortedList<TableViewData> initSongsSortedList = new SortedList<TableViewData>(initSongsFilteredList);
        
        
        this.initSongsObsList = initSongsObsList;
        this.initSongsFilteredList = initSongsFilteredList;
        this.initSongsSortedList = initSongsSortedList;
        this.testTable.setItems(initSongsSortedList);
	}
	
	@FXML private void testSort(ActionEvent event) {
		this.initSongsSortedList.setComparator(this.orderByComboBox.getSelectionModel().getSelectedItem());
	}
	
	// testCopySongButton
	@FXML private void copySong(ActionEvent event) {
		List<TableViewData> selectedSongsList = new ArrayList<TableViewData>();
		for (TableViewData row : this.testTable.getItems()) {
			if (row.isSelectedProperty().get()) {
				selectedSongsList.add(row);
			}
		}
		if (selectedSongsList.size() == 0) {
			// TODO: change to reflect in GUI
			System.out.println("No row is chosen");
		}
		else {
			try {
				Stage saveToOptionStage = new Stage();
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(getClass().getResource("/fxml/SaveToOptionView.fxml"));
				BorderPane root = loader.load();
				Scene scene = new Scene(root);
				SaveToOptionController ctr = loader.<SaveToOptionController>getController();
				saveToOptionStage.initModality(Modality.WINDOW_MODAL);
				saveToOptionStage.initOwner(this.testTable.getScene().getWindow());
				saveToOptionStage.setTitle("Configuration");
				saveToOptionStage.setScene(scene);
				ctr.initData(saveToOptionStage, this.songsDb, selectedSongsList);
				saveToOptionStage.show();
			}
			catch (IOException e) {
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR, "Failed to load copy option screen", ButtonType.OK);
				alert.showAndWait();
			}
			catch (SQLException e) {
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR, "Error getting data from songs.db", ButtonType.OK);
				alert.showAndWait();
			}
		}
	}
	
	// hideUnhideButton
	@FXML private void hideUnhideSelectedSongs(ActionEvent event) throws SQLException {
		// TODO: account for selectAll button also (probably wanna change the whole implementation)
		try {
			String[] items = {this.songsDb.Data.BeatmapSet.IS_HIDDEN};
			this.songsDb.getConn().setAutoCommit(false);
			PreparedStatement updateBeatmapSetBooleanPStatement = this.songsDb.getUpdateBeatmapSetBooleanPreparedStatement(items);
			if (this.unhiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
				Boolean[] results = {true};
				// !! iterate over obsList instead of filteredList 
				// otherwise because of the predicate in displaySongs method, index out of bound error and stuff will occur.
				for (TableViewData row : this.initSongsObsList) {
					if (row.isSelectedProperty().get()) {
						row.isSelectedProperty().set(false);
						row.isHiddenProperty().set(true);
						this.songsDb.addUpdateBeatmapSetBatch(updateBeatmapSetBooleanPStatement, row.beatmapSetAutoIDProperty().get(), results);
					}
				}
			}
			else if (this.hiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
				Boolean[] results = {false};
				for (TableViewData row : this.initSongsObsList) {
					if (row.isSelectedProperty().get()) {
						row.isSelectedProperty().set(false);
						row.isHiddenProperty().set(false);
						this.songsDb.addUpdateBeatmapSetBatch(updateBeatmapSetBooleanPStatement, row.beatmapSetAutoIDProperty().get(), results);
					}
				}
			}
			updateBeatmapSetBooleanPStatement.executeBatch();
		}
		catch (SQLException e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to update hidden preference in songs.db", ButtonType.OK);
			alert.showAndWait();
		}
		finally {
			this.songsDb.getConn().setAutoCommit(true);
		}
	}
	
	@FXML private void displaySongs(ActionEvent event) {
		this.testSearchText.clear();
		if (this.unhiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.hideUnhideButton.setText("Hide");
			this.isDownloadedShowCheckMenuItem.setVisible(true);
			this.hideUnhideButton.setVisible(true);
			this.checkBoxCol.setVisible(true);
		}
		else if (this.hiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.hideUnhideButton.setText("Unhide");
			this.isDownloadedShowCheckMenuItem.setVisible(false);
			this.hideUnhideButton.setVisible(true);
			this.checkBoxCol.setVisible(true);
		}
		// TODO: label showing number of downloaded songs
		else if (this.downloadedSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.isDownloadedShowCheckMenuItem.setVisible(false);
			this.hideUnhideButton.setVisible(false);
			this.checkBoxCol.setVisible(false);
		}
		
		this.initSongsFilteredList.setPredicate(new CustomPredicate(""));
	}
	
	private class CustomPredicate implements Predicate<TableViewData> { 
		private final String searchedText;
		
		public CustomPredicate(String searchedText) {
			this.searchedText = searchedText;
		}
		
		@Override public boolean test(TableViewData row) {
			boolean displayCondition = true;
			if (unhiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
				// account for showing downloaded or not here as it involves rows. Other related MenuItems are set in initialize method
				if (isDownloadedShowCheckMenuItem.isSelected()) {
					displayCondition = row.isHiddenProperty().get() ? false : true;
				}
				else {
					displayCondition = row.isHiddenProperty().get() || row.isDownloadedProperty().get() ? false : true;
				}
			}
			else if (hiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
				displayCondition = row.isHiddenProperty().get() ? true : false;
			}
			else if (downloadedSongsRadioMenuItemInDisplayMenu.isSelected()) {
				displayCondition = row.isDownloadedProperty().get() ? true : false;
			}
			
			// TODO: allows comparator < and > for length related or date?
			if (this.searchedText.isEmpty()) {
				return displayCondition && true;
			}
			String[] items = {row.songSourceProperty().get(), row.artistNameProperty().get()
    				, row.artistNameUnicodeProperty().get(), row.songTitleProperty().get()
    				, row.songTitleUnicodeProperty().get(), row.songTagNamesProperty().get()
    				, row.creatorNameProperty().get()};
    		String itemsStr = String.join(" ", items).toLowerCase();
    		String[] words = searchedText.toLowerCase().split("\\s+");
    		if (Arrays.stream(words).parallel().allMatch(itemsStr::contains)) {
    			return displayCondition && true;
    		}
    		return displayCondition && false;
		}
	}
	
//	class CustomCheckBoxCell extends TableCell<TableViewData, Boolean>
//    {
//        private CheckBox checkBox = new CheckBox();
//        public CustomCheckBoxCell(){
//        	checkBox.setOnAction(new EventHandler<ActionEvent>() {
//                @Override
//                public void handle(ActionEvent actionEvent) {
//
//                    if(checkBox.isSelected()) {
//                    	TableViewData selectedRow = testTable.getItems().get(getTableRow().getIndex());
//                    	selectedRow.setIsSelectedProperty(new SimpleBooleanProperty(true));
//                    }
//                    else {
//                    	TableViewData selectedRow = testTable.getItems().get(getTableRow().getIndex());
//                    	selectedRow.setIsSelectedProperty(new SimpleBooleanProperty(false));
//                    }
//                }
//            });
//        }
//        
//        @Override
//        protected void updateItem(Boolean item, boolean empty)
//        {
//           super.updateItem(item, empty);
//           if(!empty && item != null) {
////        	   checkBox.setAlignment(Pos.CENTER);
//        	   checkBox.setSelected(item);
//               setAlignment(Pos.CENTER);
//               setGraphic(checkBox);
//           }
//        }
//    }
	
	public static class TableViewData {
		private final SimpleIntegerProperty beatmapSetAutoID;
		private final SimpleStringProperty songSource;
		private final SimpleStringProperty artistName;
		private final SimpleStringProperty artistNameUnicode;
		private final SimpleStringProperty songTitle;
		private final SimpleStringProperty songTitleUnicode;
//		private final SimpleIntegerProperty previewTime;
		private final SimpleIntegerProperty totalTime;
		private final SimpleLongProperty lastModificationTime;
		private SimpleBooleanProperty isDownloaded;
		private SimpleBooleanProperty isHidden;
		private SimpleBooleanProperty isSelected;
		private final SimpleStringProperty folderName;
		private final SimpleStringProperty audioName;
		private final SimpleStringProperty songTagNames;
		private final SimpleStringProperty creatorName;
		
		// TODO: add previewTime, ranked?, last play time
		public TableViewData(int beatmapSetAutoID, String songSource, String artistName, String artistNameUnicode, String songTitle, String songTitleUnicode, int totalTime
				, long lastModificationTime, boolean isDownloaded, boolean isHidden, boolean isSelected, String folderName, String audioName, String songTagNames, String creatorName) {
			this.beatmapSetAutoID = new SimpleIntegerProperty(beatmapSetAutoID);
			this.songSource = new SimpleStringProperty(songSource);
			this.artistName = new SimpleStringProperty(artistName);
			this.artistNameUnicode = new SimpleStringProperty(artistNameUnicode);
			this.songTitle = new SimpleStringProperty(songTitle);
			this.songTitleUnicode = new SimpleStringProperty(songTitleUnicode);
			this.totalTime = new SimpleIntegerProperty(totalTime);
			this.lastModificationTime = new SimpleLongProperty(lastModificationTime);
			this.isDownloaded = new SimpleBooleanProperty(isDownloaded);
			this.isHidden = new SimpleBooleanProperty(isHidden);
			this.isSelected = new SimpleBooleanProperty(isSelected);
			this.folderName = new SimpleStringProperty(folderName);
			this.audioName = new SimpleStringProperty(audioName);
			this.songTagNames = new SimpleStringProperty(songTagNames);
			this.creatorName = new SimpleStringProperty(creatorName);
		}
		
		public static Callback<TableViewData, Observable[]> extractor() {
		   return (TableViewData p) -> new Observable[]{p.isDownloadedProperty(), p.isHiddenProperty(), p.isSelectedProperty()};
		}
		
		public static String totalTimeToString(int totalTime) {
			return String.format("%02d:%02d", 
				    TimeUnit.MILLISECONDS.toMinutes(totalTime),
				    TimeUnit.MILLISECONDS.toSeconds(totalTime) - 
				    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTime))
				    );
		}

		public SimpleBooleanProperty isDownloadedProperty() {
			return isDownloaded;
		}

		public void setIsDownloadedProperty(SimpleBooleanProperty isDownloadedProperty) {
			this.isDownloaded = isDownloadedProperty;
		}

		public SimpleBooleanProperty isHiddenProperty() {
			return isHidden;
		}

		public void setIsHiddenProperty(SimpleBooleanProperty isHiddenProperty) {
			this.isHidden = isHiddenProperty;
		}

		public SimpleBooleanProperty isSelectedProperty() {
			return isSelected;
		}

		public void setIsSelectedProperty(SimpleBooleanProperty isSelectedProperty) {
			this.isSelected = isSelectedProperty;
		}
		
		public SimpleIntegerProperty beatmapSetAutoIDProperty() {
			return beatmapSetAutoID;
		}
		
		public SimpleStringProperty songSourceProperty() {
			return songSource;
		}

		public SimpleStringProperty artistNameProperty() {
			return artistName;
		}

		public SimpleStringProperty artistNameUnicodeProperty() {
			return artistNameUnicode;
		}

		public SimpleStringProperty songTitleProperty() {
			return songTitle;
		}

		public SimpleStringProperty songTitleUnicodeProperty() {
			return songTitleUnicode;
		}

		public SimpleIntegerProperty totalTimeProperty() {
			return totalTime;
		}

		public SimpleLongProperty lastModificationTimeProperty() {
			return lastModificationTime;
		}
		
		public SimpleStringProperty folderNameProperty() {
			return folderName;
		}

		public SimpleStringProperty audioNameProperty() {
			return audioName;
		}
		
		public SimpleStringProperty songTagNamesProperty() {
			return songTagNames;
		}
		
		public SimpleStringProperty creatorNameProperty() {
			return creatorName;
		}
		

//		public String getSongSource() {
//			return songSource.get();
//		}
//
//
//		public String getSongTitle() {
//			return songTitle.get();
//		}
//
//
//		public String getSongTitleUnicode() {
//			return songTitleUnicode.get();
//		}
//
//
//		public String getArtistName() {
//			return artistName.get();
//		}
//
//
//		public String getArtistNameUnicode() {
//			return artistNameUnicode.get();
//		}
//		
//		public String getTotalTime() {
//			return totalTime.get();
//		}
//		
//		public long getLastmodificationTime() {
//			return lastModificationTime.get();
//		}
//		
//		public boolean getIsDownloaded() {
//			return isDownloaded.get();
//		}
//		
//		public boolean getIsHidden() {
//			return isHidden.get();
//		}
//		
//		public boolean getIsSelected() {
//			return isSelected.get();
//		}
	}
}

