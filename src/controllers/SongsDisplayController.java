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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import application.Comparators;
import application.Comparators.SongTitleComparator;
import application.Main;
import application.SqliteDatabase;
import controllers.FilterDialogController.SimplifiedTableViewData;
import controllers.SongsDisplayController.TableViewData;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
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
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
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
//	@FXML private MenuItem checkNewSongsFileMenuItem;
	@FXML private MenuItem fullBeatmapsUpdateFileMenuItem;
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
	
	@FXML private Label mediaPlayerTitleLabel;
	@FXML private Button mediaPlayerPlayButton;
	@FXML private Button mediaPlayerPauseButton;
	@FXML private Button mediaPlayerStopButton;
	@FXML private Button mediaPlayerPreviousButton;
	@FXML private Button mediaPlayerNextButton;
	@FXML private Label mediaPlayerSpeakerLabel;
	@FXML private Slider mediaPlayerVolumeSlider;
	@FXML private Slider mediaPlayerTimeSlider;
	@FXML private ToggleButton mediaPlayerRepeatToggleButton;
	@FXML private ToggleButton mediaPlayerShuffleToggleButton;
	
	private final String numOfSelectedSongsLabelText = "Selected: ";
	private SimpleIntegerProperty selectedCounterProperty = new SimpleIntegerProperty(0);
	
	private MediaPlayer mediaPlayer;
	private TableViewData currentlyPlayedSong;
	private String pathToSongsFolder;
	private String pathToOsuDb;
	private Double soundVolume;
	private boolean userChangedTimeSlider = true; // var for detecting user or comp changed slider value

//	private final String playButtonPlayText = "▶";
//	private final String playButtonPauseText = "⏸";
//	private final String shuffleButtonShuffleText = "🔀";
//	private final String shuffleButtonRepeatText = "∞";
	private final String speakerUTFIcon = "🔊";
	private final String speakerMuteUTFIcon = "🔇";

	
	// and check the corresponding items in controller instead of fxml
	// TODO: add rotating screen while changing view, searching, etc.
	// TODO: allow user to select and copy words but not edit
	// TODO: add action to checkForNewSongs menuitem with popup windows
	
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
        // for the sake of tooltip
        Label totalTimeLabel = new Label("Length �");
        totalTimeLabel.setTooltip(new Tooltip("This length corresponds to the length of beatmap in osu!. Real mp3 length may be longer than this."));
        this.totalTimeCol.setGraphic(totalTimeLabel);
        this.checkBoxCol.setCellValueFactory(new PropertyValueFactory<TableViewData, Boolean>("isSelected"));
        this.checkBoxCol.setCellFactory(tc -> new CheckBoxTableCell<>());
//        this.checkBoxCol.setCellFactory(tc -> new CustomCheckBoxCell());
        
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
        
        this.initMenuItemsListener();
        
        // set selected items to 0
        this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + this.selectedCounterProperty.get());
        
        this.selectedCounterProperty.addListener((obs, oldValue, newValue) -> {
        	this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + newValue);
        });
	}
	
	private void initMenuItemsListener() {
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
        	// TODO: prettify this
        	this.testTable.setDisable(true);
        	pause.setOnFinished(event -> {
        		initSongsFilteredList.setPredicate(new CustomPredicate(newValue));
        		this.testTable.setDisable(false);
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
        	this.pathToOsuDb = configRs.getString(this.songsDb.Data.Config.PATH_TO_OSU_DB);
        	this.pathToSongsFolder = configRs.getString(this.songsDb.Data.Config.PATH_TO_SONGS_FOLDER);
        	this.soundVolume = configRs.getDouble(this.songsDb.Data.Config.SOUND_VOLUME);
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
       
        this.initMediaPlayerEssentials();
        
        this.testTable.setItems(initSongsSortedList);
        
	}
	
	
	// procedure:
	// startMusic (random) -> repeat Or new song base on repeatToggleButton 
	// if repeatToggleButton is pressed while playing, set mediaPlayer to repeat or not
	// shuffleToggleButton is only used when it's time to get new song
	// modelSelection prevails all
	
	private void initMediaPlayerEssentials() {
		if (this.soundVolume == null) {
			throw new RuntimeException("Sound volume is not yet selected from songs.db");
		}
		// this must be set after querying database so that mediaPlayer later can refer to this value when boot
		this.mediaPlayerVolumeSlider.setValue(this.soundVolume);
		// default is not mute icon
		if (Math.abs(this.soundVolume) < 0.01) {
			this.mediaPlayerSpeakerLabel.setText(this.speakerMuteUTFIcon);
		}
		
		
		this.testTable.getSelectionModel().selectedItemProperty().addListener((obs, wasSelected, isSelected) -> {
//			// when switching display, this is also fired but isSelected is null this time
//			// also check if mediaPlayer is initialized and set to stopped. If yes, then dun play anything
//			if (isSelected != null && this.mediaPlayer != null && this.mediaPlayer.getStatus() != Status.STOPPED) {
//				// when switching display or searching, sometimes this listener will be fired but with same isSelected
//				// so here check if selected is different only then play new song
//				if (this.currentlyPlayedSong == null || this.currentlyPlayedSong != isSelected) {
//					this.playNewSong(isSelected);
//				}
//				else {
//					// otherwise, update title only for hinting user
//					this.mediaPlayerTitleLabel.setText(this.getFormattedMediaPlayerSongTitle(isSelected));
//				}
//			}
			
			// when switching display, this is also fired but isSelected is null this time
			if (isSelected != null) {
				// also check if mediaPlayer is initialized,
				if (this.mediaPlayer != null) {
					// and set to stopped. If no, 
					if (this.mediaPlayer.getStatus() != Status.STOPPED) {
						// check if selected is different only then play new song
						if (this.currentlyPlayedSong == null || this.currentlyPlayedSong != isSelected) {
							this.playNewSong(isSelected);
						}
					}
					// otherwise,
					else {
						// update title only for visual hint
						this.mediaPlayerTitleLabel.setText(this.getFormattedMediaPlayerSongTitle(isSelected));
					}
				}
				// nothing is played and a song is selected (which should not be happenning cuz song is initialized when 1st rendering this view)
				else {
					this.playNewSong(isSelected);
				}
			}
        });
		
		// all these check for null first becuz if table is empty (for whatever reason) when booting, mediaPlayer will not be initialized
		this.mediaPlayerVolumeSlider.valueProperty().addListener(inL -> {
        	if (this.mediaPlayer != null) {
        		double volume = this.mediaPlayerVolumeSlider.getValue() / 100.0;
        		this.mediaPlayer.setVolume(volume);
        		if (Math.abs(volume) < 0.01) {
        			this.mediaPlayerSpeakerLabel.setText(this.speakerMuteUTFIcon);
        		}
        		else {
        			this.mediaPlayerSpeakerLabel.setText(this.speakerUTFIcon);
        		}
        	}
        });
		
		this.mediaPlayerTimeSlider.valueProperty().addListener(inL -> {
			if (this.mediaPlayer != null) {
				// only if user change the slider value
				if (this.userChangedTimeSlider) {
			        // multiply duration by percentage calculated by slider position
					this.mediaPlayer.seek(this.mediaPlayer.getMedia().getDuration().multiply(this.mediaPlayerTimeSlider.getValue() / 100.0));
				}
			}
		});
		
		this.mediaPlayerRepeatToggleButton.selectedProperty().addListener((obs, oldValue, newValue) -> {
			if (this.mediaPlayer != null) {
				// if repeat is selected
				if (newValue) {
					this.mediaPlayer.setOnEndOfMedia(null);
				}
				else {
					// reset the property as it might have been rewritten by other methods
					this.mediaPlayer.setOnEndOfMedia(() -> {
						this.playNewSong(this.getNextRowForMusic(false));
					}); 
				}
			}
		});
		
		this.mediaPlayerPlayButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
				// this is for situation where user press stop and select new song then press play
				// in this case, the song played should be the currently selected song instead of the cached song before stop
				// selectedItem will equal to null if nothing is selected when stop
				if (this.mediaPlayer.getStatus() == Status.STOPPED && this.testTable.getSelectionModel().getSelectedItem() != null
						&& !this.testTable.getSelectionModel().getSelectedItem().equals(this.currentlyPlayedSong)) {
					this.playNewSong(this.testTable.getSelectionModel().getSelectedItem());
				}
				else {
					if (this.mediaPlayer.getStatus() == Status.PAUSED || this.mediaPlayer.getStatus() == Status.STOPPED) {
						this.mediaPlayer.play();
					}
					// TODO: think carefully for play when stopped
					else if (this.mediaPlayer.getStatus() == Status.PLAYING) {
						this.mediaPlayer.seek(this.mediaPlayer.getStartTime());
					}
				}
			}
		});
		
		this.mediaPlayerPauseButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
				if (this.mediaPlayer.getStatus() == Status.PLAYING) {
					this.mediaPlayer.pause();
				}
				else if (this.mediaPlayer.getStatus() == Status.PAUSED) {
					this.mediaPlayer.play();
				}
			}
		});
		
		this.mediaPlayerStopButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
				this.mediaPlayer.stop();
			}
		});
		
		// next and previous doesn't respect the currently selected songs if status is stopped but doesn't matter much
		this.mediaPlayerNextButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
				this.playNewSong(this.getNextRowForMusic(false));
			}
		});
		
		this.mediaPlayerPreviousButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
				this.playNewSong(this.getNextRowForMusic(true));
			}
		});
		
//        Pane thumb = (Pane) this.mediaPlayerVolumeSlider.lookup(".thumb");
//        Label label = new Label();
//        label.textProperty().bind(this.mediaPlayerVolumeSlider.valueProperty().asString("%.0f"));
//        thumb.getChildren().add(label);
		
	}
	
	// TODO: might want to save the option (repeat, shuffle) into songs.db at the end
	// TODO: might want to move the exception handling to another or calling method
	private void playNewSong(TableViewData rowToBePlayed) {
		Path mp3Path = Paths.get(this.pathToSongsFolder, rowToBePlayed.folderNameProperty().get(), rowToBePlayed.audioNameProperty().get());
		
		if (mp3Path.toFile().exists()) {
			// cleanup last song
			if (this.mediaPlayer != null) {
	    		this.mediaPlayer.dispose();
	    		// reset timeSlider position
	    		this.mediaPlayerTimeSlider.setValue(0);
	    	}
			
			// load next song
			this.mediaPlayer = new MediaPlayer(new Media(mp3Path.toUri().toString()));
			// always set to repeat so that if shuffle is removed, it can repeat itself
			this.mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
			// if repeat is not chosen, play next song when current song ends
			if (!this.mediaPlayerRepeatToggleButton.isSelected()) {
				this.mediaPlayer.setOnEndOfMedia(() -> {
					this.playNewSong(this.getNextRowForMusic(false));
				});
			}
			// this listener is added here instead of in init method as only now the mediaPlayer is sure to exist
			this.mediaPlayer.currentTimeProperty().addListener(inL -> {
				// no need this as it is already handled in the init method
//				if (!this.mediaPlayerTimeSlider.isValueChanging()) {
					double currentTime = this.mediaPlayer.getCurrentTime().toMillis();
					// indicate that this slider change is not made by user,
					// so that the listener in init method won't seek unnecessarily
					this.userChangedTimeSlider = false;
					this.mediaPlayerTimeSlider.setValue(currentTime / this.mediaPlayer.getMedia().getDuration().toMillis() * 100.0);
					// set back to true to allow change by user
					this.userChangedTimeSlider = true;
//				}
			});
			this.mediaPlayer.setVolume(this.mediaPlayerVolumeSlider.getValue() / 100.0);
			this.currentlyPlayedSong = rowToBePlayed;
			this.mediaPlayerTitleLabel.setText(getFormattedMediaPlayerSongTitle(rowToBePlayed));
			this.mediaPlayer.play();
		}
		else {
			// TODO: after deciding the final name for the 'check for new songs', change this warning messsage!
			String errorMessage =  mp3Path.toString() + " is not found! Data is likely to be outdated. Try to check for new songs.";
			Alert alert = new Alert(AlertType.ERROR, errorMessage, ButtonType.OK);
			alert.showAndWait();
		}
	}
	
	private String getFormattedMediaPlayerSongTitle(TableViewData rowToBePlayed) {
		String artistName = "";
		String songTitle = "";
		
		// get unicode if col is shown and not empty else fallback to ascii
		if (this.artistNameUnicodeCol.isVisible()) {
			artistName = rowToBePlayed.artistNameUnicodeProperty().get();
		}
		if (artistName.isEmpty()) {
			artistName = rowToBePlayed.artistNameProperty().get();
		}
		
		if (this.songTitleUnicodeCol.isVisible()) {
			songTitle = rowToBePlayed.songTitleUnicodeProperty().get();
		}
		if (songTitle.isEmpty()) {
			songTitle = rowToBePlayed.songTitleProperty().get();
		}
		
		return artistName + " - " + songTitle;
	}
	
//	@FXML private void playOrPause(ActionEvent event) {
//		if (this.mediaPlayer == null) return;
//		
//		if (this.mediaPlayer.getStatus() == Status.PAUSED) {
//			this.mediaPlayer.play();
//			this.mediaPlayerPlayButton.setText(this.playButtonPauseText);
//		}
//		else if (this.mediaPlayer.getStatus() == Status.PLAYING) {
//			this.mediaPlayer.pause();
//			this.mediaPlayerPlayButton.setText(this.playButtonPlayText);
//		}
//	}
	
	
	// TODO: slider for selecting music start point
	// This is only called when stage is shown (ie. every required initialization has been done)
	public void startMusic() {
		TableViewData randomSong = this.getRandomRow();
		if (randomSong != null) {
			this.playNewSong(randomSong);
		}
	}
	
	private TableViewData getRandomRow() {
		if (this.testTable.getItems().isEmpty()) 
			return null;
		
		Random rand = new Random();
		return this.testTable.getItems().get(rand.nextInt(this.testTable.getItems().size()));
	}
	
	// at this point, currentlyPlayedSong must be initialized
	private TableViewData getNextRowForMusic(boolean reverse) {
		// check if shuffle or sequential
		if (this.mediaPlayerShuffleToggleButton.isSelected()) {
			if (this.getRandomRow() != null) {
				return this.getRandomRow();
			}
			else {
				return this.currentlyPlayedSong;
			}
		}
		else {
			// TODO: implement binary search
			// use iterator becuz the list might be filtered at the same time this is working
			for (ListIterator<TableViewData> iter = this.testTable.getItems().listIterator(); iter.hasNext();) {
				TableViewData row = iter.next();
				// if ever found this song in current tableview (ie. user does not switch to hidden display (for etc))
				if (row.equals(this.currentlyPlayedSong)) {
					if (!reverse) {
						// get next song
						if (iter.hasNext()) {
							return iter.next();
						}
					}
					else {
						if (iter.hasPrevious()) {
							iter.previous(); // same as row
							if (iter.hasPrevious()) {
								return iter.previous(); // this is the real previous row
							}
						}
					}
					// if no next song (ie. current view is empty or at the end of list), break out for default return
					break;
				}
			}
			// return 1st row if not empty
			if (this.testTable.getItems().isEmpty()) {
				return this.currentlyPlayedSong;
			}
			else {
				// if at 1st position and reverse, play the last song in the list
				if (reverse) {
					return this.testTable.getItems().get(this.testTable.getItems().size() - 1);
				}
				else {
					return this.testTable.getItems().get(0);
				}
			}
//			return this.testTable.getItems().isEmpty() ? this.currentlyPlayedSong : this.testTable.getItems().get(0);
		}
	}
	
	
	@FXML private void testSort(ActionEvent event) {
		this.initSongsSortedList.setComparator(this.orderByComboBox.getSelectionModel().getSelectedItem());
	}
	
	// testCopySongButton
	// TODO: option to exclude possible duplicated songs 
	@FXML private void copySong(ActionEvent event) {
//		List<TableViewData> selectedSongsList = new ArrayList<TableViewData>();
//		boolean containCopiedSongs = false;
//		for (TableViewData row : this.testTable.getItems()) {
//			if (row.isSelectedProperty().get()) {
//				selectedSongsList.add(row);
//				if (row.isDownloadedProperty().get()) {
//					containCopiedSongs = true;
//				}
//			}
//		}
		Map<String, List<TableViewData>> selectedSongsMap = this.testTable.getItems().stream()
				.filter(row -> row.isSelectedProperty().get())
				.collect(Collectors.groupingBy(row -> row.folderNameProperty().get()));
		
		
		
		if (selectedSongsMap.size() == 0) {
			// TODO: change to reflect in GUI
			System.out.println("No row is chosen");
		}
		else {
//			Map<String, List<TableViewData>> selectedSongsMap = selectedSongsList.stream()
//					.collect(Collectors.groupingBy(row -> row.folderNameProperty().get()));
//			List<TableViewData> x = selectedSongsMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
			boolean containCopiedSongs = selectedSongsMap.values().stream().anyMatch(list -> list.stream().anyMatch(row -> row.isDownloadedProperty().get()));
			boolean proceed = true;
			if (containCopiedSongs) {
				String warningText = "One or more copied songs are found in your copy list. Are you sure you want to proceed to copy those songs again? (This will result in duplicated songs in the same folder)";
				Alert duplicatedAlert = new Alert(AlertType.WARNING, warningText, ButtonType.YES, ButtonType.NO);
				Optional<ButtonType> result = duplicatedAlert.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.NO) {
				    proceed = false;
				}
			}
			
			if (proceed) {
				try {
					this.loadSaveToOptionView(selectedSongsMap);
				}
				catch (SQLException e) {
					e.printStackTrace();
					Alert alert = new Alert(AlertType.ERROR, "Error getting data from songs.db", ButtonType.OK);
					alert.showAndWait();
				}
				catch (Exception e) {
					e.printStackTrace();
					Alert alert = new Alert(AlertType.ERROR, "Failed to load copy option screen", ButtonType.OK);
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
			PreparedStatement updateBeatmapSetBooleanPStatement = this.songsDb.getUpdateBeatmapSetBooleanPStatement(items);
			if (this.unhiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
				Boolean[] results = {true};
				// !! iterate over obsList instead of filteredList 
				// otherwise because of the nature of filteredList, index out of bound error and stuff will occur.
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
			// TODO: the following line can violate the user choice
			this.isDownloadedShowCheckMenuItem.setVisible(true);
			this.hideUnhideButton.setVisible(true);
//			this.checkBoxCol.setVisible(true);
			this.selectedCounterProperty.set(this.selectedCounterProperty.get());
		}
		else if (this.hiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.hideUnhideButton.setText("Unhide");
			this.isDownloadedShowCheckMenuItem.setVisible(false);
			this.hideUnhideButton.setVisible(true);
//			this.checkBoxCol.setVisible(true);
		}
		// TODO: might want to allow selecting in here also
		// TODO: add one more invisible label in VBox or HBox and show that label instead of sharing labels
		else if (this.downloadedSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.isDownloadedShowCheckMenuItem.setVisible(false);
			this.hideUnhideButton.setVisible(false);
//			this.checkBoxCol.setVisible(false);
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
					if (this.mediaPlayer != null) {
						this.mediaPlayer.dispose();
					}
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
	
	@FXML private void fullBeatmapsUpdate(ActionEvent event) {
//		this.songsDb.updateDetails(osuDbBeatmapsMap);
		try {
			this.loadUpdateDetailsView();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadUpdateDetailsView() throws IOException {
		// TODO: set modality
		Stage updateDetailsStage = new Stage();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/LoadingDialogParentView.fxml"));
		UpdateDetailsController ctr = new UpdateDetailsController();
		loader.setController(ctr);
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		
		updateDetailsStage.setTitle("Update Songs Details");
		updateDetailsStage.setScene(scene);
		// the last two paths must have already initialized to come to here
		ctr.initDataAndStart(updateDetailsStage, this.songsDb, this.pathToOsuDb, this.pathToSongsFolder);
		updateDetailsStage.setScene(scene);
		updateDetailsStage.show();
	}
	
	private void loadSaveToOptionView(Map<String, List<TableViewData>> selectedSongsMap) throws SQLException, IOException {
		Stage saveToOptionStage = new Stage();
		// pretty save to use this here as the new stage opened is not minimizable
		// if that changes later, this must be changed as well
		saveToOptionStage.showingProperty().addListener((obs, oldValue, newValue) -> {
			if (!newValue) { // when closed
				// so that copied songs can be reflected in the table 
				this.testTable.refresh();
			}
		});
		
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/SaveToOptionView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		SaveToOptionController ctr = loader.<SaveToOptionController>getController();
		saveToOptionStage.initModality(Modality.WINDOW_MODAL);
		saveToOptionStage.initOwner(this.testTable.getScene().getWindow());
		saveToOptionStage.setTitle("Configuration");
		saveToOptionStage.setScene(scene);
		ctr.initData(saveToOptionStage, this.songsDb, selectedSongsMap, this.artistNameUnicodeCol.isVisible(), this.songTitleUnicodeCol.isVisible());
		saveToOptionStage.show();
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
					, totalTimeShowCheckMenuItem.isSelected(), isDownloadedShowCheckMenuItem.isSelected(), ordering, this.mediaPlayerVolumeSlider.getValue());
		}
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
 			// shouldn't have come to here but return false by default
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
//			String[] items = {row.songSourceProperty().get(), row.artistNameProperty().get()
//    				, row.artistNameUnicodeProperty().get(), row.songTitleProperty().get()
//    				, row.songTitleUnicodeProperty().get(), row.songTagNamesProperty().get()
//    				, row.creatorNameProperty().get()};
			String[] items = {row.songTitleProperty().get()
					, row.artistNameProperty().get()
					, row.songTitleUnicodeProperty().get()
					, row.artistNameUnicodeProperty().get() 
					, row.songTagNamesProperty().get()
					, row.songSourceProperty().get()
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
		private final SimpleBooleanProperty isDownloaded;
		private final SimpleBooleanProperty isHidden;
		private final SimpleBooleanProperty isSelected;
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
		
		public SimpleBooleanProperty isHiddenProperty() {
			return isHidden;
		}

		public SimpleBooleanProperty isSelectedProperty() {
			return isSelected;
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
		
	}
}

