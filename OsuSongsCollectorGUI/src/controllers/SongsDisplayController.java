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
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import application.Comparators;
import application.Comparators.SongTitleComparator;
import application.Main;
import application.SqliteDatabase;
import controllers.SongsDisplayController.TableViewData;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
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
	private Stage currentStage;
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
	private SimpleIntegerProperty selectedCounterProperty = new SimpleIntegerProperty(0);
	
	
	// TODO: save those (and maybe saveFolder) in a different table Configuration
	// and check the corresponding items in controller instead of fxml
	// TODO: set help text on approx length
	// TODO: add rotating screen while changing view, searching, etc.
	// TODO: allow user to select and copy words but not edit
	
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
        
//        Comparator<TableViewData> lastModificationTimeComparator = new Comparators.LastModificationTimeComparator();
//        ObservableList<Comparator<TableViewData>> orderByComparatorsObsList = FXCollections.observableArrayList(
//        		new Comparators.SongTitleComparator(),
//        		new Comparators.ArtistNameComparator(),
//        		new Comparators.CreatorNameComparator(),
//        		new Comparators.TotalTimeComparator(),
//        		lastModificationTimeComparator
//        );
//        // TODO: set to user preference and sort using that after setting
//        this.orderByComboBox.setItems(orderByComparatorsObsList);
//        this.orderByComboBox.getSelectionModel().select(lastModificationTimeComparator);
        
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
        this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + this.selectedCounterProperty.get());
        
        this.selectedCounterProperty.addListener((obs, oldValue, newValue) -> {
        	this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + newValue);
        });
        
        // selectAll
//        this.selectAllCheckBoxInCheckBoxCol.selectedProperty().addListener((obs, oldValue, newValue) -> {
//        	if (newValue) {
//        		this.testTable.getItems().forEach(row -> {
//    				if (!row.isSelectedProperty().get()) {
//    					row.isSelectedProperty().set(true);
//    				}
//    			});
//        	}
//        	else {
//        		this.testTable.getItems().forEach(row -> {
//        			if (row.isSelectedProperty().get()) {
//        				row.isSelectedProperty().set(false);
//        			}
//        		});
//        	}
//        });
	}
	
	public void initData(Stage currentStage, SqliteDatabase connectedSongsDb) throws SQLException {
		this.currentStage = currentStage;
		this.songsDb = connectedSongsDb;
		currentStage.setOnCloseRequest(e -> {
			try {
				this.updatePreference();
				this.songsDb.closeConnection();	
			} 
			// exception instead of SQLException as other exceptions can happen that prevent window from closing
			catch (Exception e1) {
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
        			this.selectedCounterProperty.set(this.selectedCounterProperty.get() + 1);
        		}
        		else {
        			this.selectedCounterProperty.set(this.selectedCounterProperty.get() - 1);
        		}
        	});
        	initSongsObsList.add(t);

        }
        FilteredList<TableViewData> initSongsFilteredList = new FilteredList<TableViewData>(initSongsObsList, new CustomPredicate(""));
        PauseTransition pause = new PauseTransition(Duration.millis(200));
        this.testSearchText.textProperty().addListener((obs, oldValue, newValue) -> {
        	pause.setOnFinished(event -> {
        		initSongsFilteredList.setPredicate(new CustomPredicate(newValue));
        	});
            pause.playFromStart();
        });
        
        SortedList<TableViewData> initSongsSortedList = new SortedList<TableViewData>(initSongsFilteredList);
        
        // store the references first as the following operation can trigger listener which requires these references
        this.initSongsObsList = initSongsObsList;
        this.initSongsFilteredList = initSongsFilteredList;
        this.initSongsSortedList = initSongsSortedList;
        
        ResultSet configRs = this.songsDb.selectConfig();
        if (configRs.next()) {
        	boolean isSongSourceShown = configRs.getBoolean(this.songsDb.Data.Config.IS_SONG_SOURCE_SHOWN);
        	boolean isArtistNameShown = configRs.getBoolean(this.songsDb.Data.Config.IS_ARTIST_NAME_SHOWN);
        	boolean isArtistNameUnicodeShown = configRs.getBoolean(this.songsDb.Data.Config.IS_ARTIST_NAME_UNICODE_SHOWN);
        	boolean isSongTitleShown = configRs.getBoolean(this.songsDb.Data.Config.IS_SONG_TITLE_SHOWN);
        	boolean isSongTitleUnicodeShown = configRs.getBoolean(this.songsDb.Data.Config.IS_SONG_TITLE_UNICODE_SHOWN);
        	boolean isCreatorNameShown = configRs.getBoolean(this.songsDb.Data.Config.IS_CREATOR_NAME_SHOWN);
        	boolean isTotalTimeShown = configRs.getBoolean(this.songsDb.Data.Config.IS_TOTAL_TIME_SHOWN);
        	boolean isIsDownloadedShown = configRs.getBoolean(this.songsDb.Data.Config.IS_IS_DOWNLOADED_SHOWN);
        	String ordering = configRs.getString(this.songsDb.Data.Config.ORDERING);
        	
        	// ordering is empty only when it's the first time loading the app, 
        	// so if it's first time, dun overwrite the menuItem as the data from songsDb is defaulted to false
        	if (!ordering.isEmpty()) {
        		if (this.songSourceShowCheckMenuItem.isSelected() != isSongSourceShown) {
            		this.songSourceShowCheckMenuItem.setSelected(isSongSourceShown);
            	}
            	if (this.artistNameShowCheckMenuItem.isSelected() != isArtistNameShown) {
            		this.artistNameShowCheckMenuItem.setSelected(isArtistNameShown);
            	}
            	if (this.artistNameUnicodeShowCheckMenuItem.isSelected() != isArtistNameUnicodeShown) {
            		this.artistNameUnicodeShowCheckMenuItem.setSelected(isArtistNameUnicodeShown);
            	}
            	if (this.songTitleShowCheckMenuItem.isSelected() != isSongTitleShown) {
            		this.songTitleShowCheckMenuItem.setSelected(isSongTitleShown);
            	}
            	if (this.songTitleUnicodeShowCheckMenuItem.isSelected() != isSongTitleUnicodeShown) {
            		this.songTitleUnicodeShowCheckMenuItem.setSelected(isSongTitleUnicodeShown);
            	}
            	if (this.creatorNameShowCheckMenuItem.isSelected() != isCreatorNameShown) {
            		this.creatorNameShowCheckMenuItem.setSelected(isCreatorNameShown);
            	}
            	if (this.totalTimeShowCheckMenuItem.isSelected() != isTotalTimeShown) {
            		this.totalTimeShowCheckMenuItem.setSelected(isTotalTimeShown);
            	}
            	if (this.isDownloadedShowCheckMenuItem.isSelected() != isIsDownloadedShown) {
            		this.isDownloadedShowCheckMenuItem.setSelected(isIsDownloadedShown);
            	}
        	}
        	
        	// default comparator
        	Comparator<TableViewData> lastModificationTimeComparator = new Comparators.LastModificationTimeComparator();
			@SuppressWarnings("unchecked")
			ObservableList<Comparator<TableViewData>> orderByComparatorsObsList = FXCollections.observableArrayList(
			 	new Comparators.SongTitleComparator(),
			 	new Comparators.ArtistNameComparator(),
			 	new Comparators.CreatorNameComparator(),
			 	new Comparators.TotalTimeComparator(),
			 	lastModificationTimeComparator
			 );
			 
			this.orderByComboBox.setItems(orderByComparatorsObsList);
			if (!ordering.isEmpty()) {
				for (Comparator<TableViewData> comparator : orderByComparatorsObsList) {
			    	if (comparator.toString().equals(ordering)) {
			    		this.orderByComboBox.getSelectionModel().select(comparator);
			    		initSongsSortedList.setComparator(comparator);
			    		break;
			    	}
			    }
			}
			// for first time loading app
			else {
				this.orderByComboBox.getSelectionModel().select(lastModificationTimeComparator);
			}
        }
        else {
        	throw new SQLException("Failed to get config data");
        }
       
        this.testTable.setItems(initSongsSortedList);
	}
	
	private void updatePreference() throws SQLException {
		ResultSet configRs = this.songsDb.selectConfig();
		if (configRs.next()) {
			int configID = configRs.getInt(this.songsDb.Data.Config.CONFIG_ID);
			String pathToOsuDb = configRs.getString(this.songsDb.Data.Config.PATH_TO_OSU_DB);
			String pathToSongsFolder = configRs.getString(this.songsDb.Data.Config.PATH_TO_SONGS_FOLDER);
			String saveFolder = configRs.getString(this.songsDb.Data.Config.SAVE_FOLDER);
			String ordering = this.orderByComboBox.getSelectionModel().getSelectedItem().toString();
			this.songsDb.updateConfigFull(configID, pathToOsuDb, pathToSongsFolder, saveFolder
					, songSourceShowCheckMenuItem.isSelected(), artistNameShowCheckMenuItem.isSelected(), artistNameUnicodeShowCheckMenuItem.isSelected()
					, songTitleShowCheckMenuItem.isSelected(), songTitleUnicodeShowCheckMenuItem.isSelected(), creatorNameShowCheckMenuItem.isSelected()
					, totalTimeShowCheckMenuItem.isSelected(), isDownloadedShowCheckMenuItem.isSelected(), ordering);
		}
	}
	
	@FXML private void testSort(ActionEvent event) {
		this.initSongsSortedList.setComparator(this.orderByComboBox.getSelectionModel().getSelectedItem());
	}
	
	// testCopySongButton
	// TODO: option to exclude possible duplicated songs 
	@FXML private void copySong(ActionEvent event) {
		List<TableViewData> selectedSongsList = new ArrayList<TableViewData>();
//		boolean dismissWarning = false;
//		ButtonType ignoreButton = new ButtonType("Ignore", ButtonData.NO);
		boolean containCopiedSongs = false;
		for (TableViewData row : this.testTable.getItems()) {
			if (row.isSelectedProperty().get()) {
//				if (row.isDownloadedProperty().get() && !dismissWarning) {
//					String warning = "testing";
////					Alert customAlert = this.alertWithCheckBox(AlertType.WARNING, warning, isSelected -> {if (isSelected) {dismissWarning = true;}}, ButtonType.YES, ButtonType.NO);
////					Alert alert = new Alert(AlertType.WARNING, "", ButtonType.YES, ButtonType.NO);
//				}
				selectedSongsList.add(row);
				if (row.isDownloadedProperty().get()) {
					containCopiedSongs = true;
				}
			}
		}
		
//		Map<String, List<TableViewData>> t = this.testTable.getItems().stream()
//				.filter(row -> row.isSelectedProperty().get())
//				.collect(Collectors.groupingBy(row -> row.folderNameProperty().get()));
		
		if (selectedSongsList.size() == 0) {
			// TODO: change to reflect in GUI
			System.out.println("No row is chosen");
		}
		else {
			boolean proceed = true;
			if (containCopiedSongs) {
				String warningText = "One or more copied songs are found in your copy list. Are you sure you want to proceed to copy those songs again? (This will result in duplicated songs in the same folder)";
				Alert duplicatedAlert = new Alert(AlertType.WARNING, warningText, ButtonType.YES, ButtonType.NO);
//				duplicatedAlert.showAndWait().ifPresent(response -> {
//					if (response == ButtonType.NO) {
//						return;
//					}
//				});
				Optional<ButtonType> result = duplicatedAlert.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.NO) {
				    proceed = false;
				}
			}
			
			if (proceed) {
				try {
					this.loadSaveToOptionView(selectedSongsList);
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
	}
	
	// hideUnhideButton
	@FXML private void hideUnhideSelectedSongs(ActionEvent event) throws SQLException {
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
			this.selectAllCheckBoxInCheckBoxCol.setSelected(false);
			// TODO: wrap this in task
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
	
	@FXML private void selectUnselectAll(ActionEvent event) {
//		if (this.selectAllCheckBoxInCheckBoxCol.isSelected()) {
//    		this.testTable.getItems().forEach(row -> {
//				if (!row.isSelectedProperty().get()) {
//					row.isSelectedProperty().set(true);
//				}
//			});
//    	}
//    	else {
//    		this.testTable.getItems().forEach(row -> {
//    			if (row.isSelectedProperty().get()) {
//    				row.isSelectedProperty().set(false);
//    			}
//    		});
//    	}
		System.out.println("Start");
		boolean setValue = this.selectAllCheckBoxInCheckBoxCol.isSelected();
		ObservableList<TableViewData> obsList = this.testTable.getItems();
		for (TableViewData row : obsList) {
			row.isSelectedProperty().set(setValue);
		}
		System.out.println("End");
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
	
	@FXML private void resetAll(ActionEvent event) {
		String warning = "Are you sure you want to reset all data? All stored data such as copied songs, hidden songs, chosen path, and preferences"
				+ " will be lost! (Application will be restarted after reset)";
		Alert alert = new Alert(AlertType.WARNING, warning, ButtonType.YES, ButtonType.NO);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				try {
					this.songsDb.closeConnection();
					this.songsDb.deleteSongsDb();
					this.currentStage.hide();
					Main newApp = new Main();
					newApp.start(new Stage());
				} 
				catch (Exception e) {
					e.printStackTrace();
					Alert restartAlert = new Alert(AlertType.ERROR, "Failed to restart", ButtonType.OK);
					restartAlert.showAndWait();
				}
			}
		});
	}
	
	private void loadSaveToOptionView(List<TableViewData> selectedSongsList) throws SQLException, IOException {
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
	
//	private Alert alertWithCheckBox(AlertType type, String contentText, Consumer<Boolean> checkBoxAction, ButtonType... buttonTypes) {
//		Alert alert = new Alert(type);
//		// Need to force the alert to layout in order to grab the graphic,
//		 // as we are replacing the dialog pane with a custom pane
//		 alert.getDialogPane().applyCss();
//		 Node graphic = alert.getDialogPane().getGraphic();
//		 // Create a new dialog pane that has a checkbox instead of the hide/show details button
//		 // Use the supplied callback for the action of the checkbox
//		 alert.setDialogPane(new DialogPane() {
//		   @Override
//		   protected Node createDetailsButton() {
//		     CheckBox optOut = new CheckBox();
//		     optOut.setText("Do not ask again");
//		     optOut.setOnAction(e -> checkBoxAction.accept(optOut.isSelected()));
//		     return optOut;
//		   }
//		 });
//		 alert.getDialogPane().getButtonTypes().addAll(buttonTypes);
//		 alert.getDialogPane().setContentText(contentText);
//		 // Fool the dialog into thinking there is some expandable content
//		 // a Group won't take up any space if it has no children
//		 alert.getDialogPane().setExpandableContent(new Group());
//		 alert.getDialogPane().setExpanded(true);
//		 // Reset the dialog graphic using the default style
//		 alert.getDialogPane().setGraphic(graphic);
//		 return alert;
//	}
	
	
	private class CustomPredicate implements Predicate<TableViewData> { 
		private final String searchedText;
		
		private final String lengthFilter = "length"; 
		
		public CustomPredicate(String searchedText) {
			this.searchedText = searchedText;
		}
		
		private boolean matchLengthFilter(String[] words) {
			if (words.length != 1) {
				return false;
			}
			if (words[0].toLowerCase().startsWith(lengthFilter)) {
				// must have at least 2 chars after length (for <,> or <=,>=) to qualify 
				if (words[0].length() < lengthFilter.length() + 2) {
					return false;
				}
				else {
					String operator = words[0].substring(lengthFilter.length(), lengthFilter.length() + 2);
					// make sure there's number after the operators for filtering
					if ((!operator.startsWith("<") && !operator.startsWith(">")) || (operator.endsWith("=") && words[0].length() == lengthFilter.length() + 2)) {
						return false;
		 			}
					
					return true;
				}
			}
			else {
				return false;
			}
		}
		
		private boolean matchLengthCondition(String word, int totalTime) {
			int operatorStartIndex = lengthFilter.length();
			String searchedLengthStr;
			String operator = word.substring(operatorStartIndex, operatorStartIndex + 2);
			// operator is either < or >
			if (!operator.endsWith("=")) {
				// get substring after operator
				searchedLengthStr = word.substring(operatorStartIndex + 1);
				// rewrite operator as length of operator is only one
				operator = Character.toString(operator.charAt(0));
			}
			// operator is either <= or >=
			else {
				searchedLengthStr = word.substring(operatorStartIndex + 2);
			}
			
			// for searching seconds
			String[] searchedLengthArr = searchedLengthStr.split(":", 2);
			long searchedLength;
			// if search for minutes + seconds (etc. 4:30)
			if (searchedLengthArr.length > 1) {
				// check if they are digits
				for (String time : searchedLengthArr) {
					if (!time.matches("\\d+")) {
						return false;
					}
				}
				// false if length of seconds string is more than two (etc. 2:333) 
				if (searchedLengthArr[1].length() > 2) {
					return false;
				}
				// convert to seconds
				searchedLength = TimeUnit.MINUTES.toSeconds(Long.parseLong(searchedLengthArr[0])) + Long.parseLong(searchedLengthArr[1]);
			}
			else {
				if (!searchedLengthStr.matches("\\d+")) {
					return false;
				}
				
				searchedLength = TimeUnit.MINUTES.toSeconds(Long.parseLong(searchedLengthStr));
			}
			// convert totalTime to seconds as well for comparison as the user given string is not as precise
			long length = TimeUnit.MILLISECONDS.toSeconds(totalTime);
			switch (operator) {
				case "<":
					return length < searchedLength;
				case ">":
					return length > searchedLength;
				case "<=":
					return length <= searchedLength;
				case ">=":
					return length >= searchedLength;
			}
 			// shouldn't have come to here but return false in default
			return false;
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
			
			if (this.searchedText.isEmpty()) {
				return displayCondition && true;
			}
			
			String[] words = searchedText.toLowerCase().split("\\s+");
			
			// if search for length specifically
			if (this.matchLengthFilter(words)) {
				return this.matchLengthCondition(words[0], row.totalTimeProperty().get());
			}
			
			// TODO: can optimize this by using map of sorts instead of this linear search
			// normal search
			String[] items = {row.songSourceProperty().get(), row.artistNameProperty().get()
    				, row.artistNameUnicodeProperty().get(), row.songTitleProperty().get()
    				, row.songTitleUnicodeProperty().get(), row.songTagNamesProperty().get()
    				, row.creatorNameProperty().get()};
    		String itemsStr = String.join(" ", items).toLowerCase();
    		
    		if (Arrays.stream(words).allMatch(itemsStr::contains)) {
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
		   return (p) -> new Observable[]{p.isDownloadedProperty(), p.isHiddenProperty(), p.isSelectedProperty()};
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

