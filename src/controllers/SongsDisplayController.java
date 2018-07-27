package controllers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import application.Comparators;
import application.Constants;
import application.Main;
import application.SqliteDatabase;
import application.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.application.HostServices;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;

public class SongsDisplayController {
	private HostServices hostServices;
	public void setHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private SqliteDatabase songsDb;
	private Stage currentStage;
	private ObservableList<TableViewData> initSongsObsList;
	private FilteredList<TableViewData> initSongsFilteredList;
	private SortedList<TableViewData> initSongsSortedList;
	
	@FXML private TableView<TableViewData> songsTable;
	@FXML private TableColumn<TableViewData, String> songSourceCol;
	@FXML private TableColumn<TableViewData, String> artistNameCol;
	@FXML private TableColumn<TableViewData, String> artistNameUnicodeCol;
	@FXML private TableColumn<TableViewData, String> songTitleCol;
	@FXML private TableColumn<TableViewData, String> songTitleUnicodeCol;
	@FXML private TableColumn<TableViewData, String> creatorNameCol;
	@FXML private TableColumn<TableViewData, Integer> totalTimeCol;
	@FXML private TableColumn<TableViewData, Boolean> checkBoxCol;
	
	@FXML private Label userNameLabel;
	@FXML private Label totalSongsLabel;
	@FXML private Label currentlyVisibleLabel;
	@FXML private Label numOfSelectedSongsLabel;
	
	@FXML private TextField searchBar;
	@FXML private Button copySongButton;
	@FXML private Button hideUnhideButton;
	@FXML private CheckBox selectAllCheckBoxInCheckBoxCol;
	@FXML private ComboBox<Comparator<TableViewData>> orderByComboBox;
	
	@FXML private Menu fileMenu;
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
	@FXML private MenuItem donateMenuItemInHelpMenu;
	@FXML private MenuItem imageMenuItemInHelpMenu;
	
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
	
	@FXML private Hyperlink donateHyperlink;
	
	private final String numOfSelectedSongsLabelText = "Selected: ";
	private final String totalSongsLabelText = "Total songs: ";
	private final String currentlyVisibleLabelText = "Currently visible: ";
	private int numOfSelectedSongs = 0;
	
	private MediaPlayer mediaPlayer;
	private TableViewData currentlyPlayedSong;
	private String pathToSongsFolder;
	private String pathToOsuDb;
	private boolean userChangedTimeSlider = true; // var for detecting user or comp changed slider value

//	private final String playButtonPlayText = "▶";
//	private final String playButtonPauseText = "⏸";
//	private final String shuffleButtonShuffleText = "🔀";
//	private final String shuffleButtonRepeatText = "∞";
//	private final String speakerUTFIcon = "🔊";
//	private final String speakerMuteUTFIcon = "🔇";
	private final ImageView speakerIcon = new ImageView();
	private final ImageView speakerMuteIcon = new ImageView();
	
	// TODO: add rotating screen while changing view, searching, etc.
	// TODO: allow user to select and copy words but not edit
	
	// TODO: add batch to hide and copy songs 
	
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
        
        this.songsTable.setRowFactory(value -> {
        	return new TableRow<TableViewData>() {
        		@Override protected void updateItem(TableViewData item, boolean empty) {
        			super.updateItem(item, empty);
    				if (item != null && item.isDownloadedProperty().get()) {
//    					setStyle("-fx-background-color: linear-gradient(to right, #20bdff, #a5fecb 1%, transparent 1%, transparent 99%, #a5fecb 99%, #20bdff);");
    					setStyle("-fx-background-color: linear-gradient(to right, rgba(116, 235, 213, 0.2), rgba(165, 254, 203, 0.2) , transparent);");
    				}
        			else {
        				setStyle("");
        			}
        		}
        	};
        });
        
        this.initMenuItemsListener();
        
        // set selected items to 0
        this.numOfSelectedSongsLabel.setText(this.numOfSelectedSongsLabelText + this.numOfSelectedSongs);
        
        // focus on search bar on start (runlater or it wont focus)
        Platform.runLater(() -> {
        	this.searchBar.requestFocus();
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
        	
        	this.initSongsFilteredList.setPredicate(new CustomPredicate(this.searchBar.getText()));
        });
	}
	
	public void initData(Stage currentStage, SqliteDatabase connectedSongsDb) throws SQLException {
		this.currentStage = currentStage;
		this.songsDb = connectedSongsDb;
		currentStage.setOnCloseRequest(e -> {
			try {
				this.updatePreference();
				this.songsDb.closeConnection();	
				logger.logp(Level.INFO, this.getClass().getName(), "initData", "Safely saved and exited");
			} 
			// catch exceptions as they can prevent window from closing
			catch (Exception e1) {
				logger.logp(Level.SEVERE,  this.getClass().getName(), "initData", "Failed to updatePreference when closed", e1);
			}
		});
		this.initTableView();
	}
	
	private void initTableView() throws SQLException {
        ResultSet tableInitDataRs = this.songsDb.getTableInitData();
       
        ObservableList<TableViewData> initSongsObsList = FXCollections.observableArrayList(TableViewData.extractor());
        while (tableInitDataRs.next()) {
        	TableViewData t = new TableViewData(
        			tableInitDataRs.getInt(SqliteDatabase.TableData.BeatmapSet.BEATMAP_SET_AUTO_ID)
					, tableInitDataRs.getString(SqliteDatabase.TableData.Song.SONG_SOURCE)
        			, tableInitDataRs.getString(SqliteDatabase.TableData.Artist.ARTIST_NAME)
        			, tableInitDataRs.getString(SqliteDatabase.TableData.Artist.ARTIST_NAME_UNICODE)
        			, tableInitDataRs.getString(SqliteDatabase.TableData.Song.SONG_TITLE)
        			, tableInitDataRs.getString(SqliteDatabase.TableData.Song.SONG_TITLE_UNICODE)
        			, tableInitDataRs.getInt(SqliteDatabase.TableData.Beatmap.TOTAL_TIME)
        			, tableInitDataRs.getLong(SqliteDatabase.TableData.Beatmap.LAST_MODIFICATION_TIME)
        			, tableInitDataRs.getBoolean(SqliteDatabase.TableData.BeatmapSet.IS_DOWNLOADED)
        			, tableInitDataRs.getBoolean(SqliteDatabase.TableData.BeatmapSet.IS_HIDDEN)
        			, false // set isSelectedProperty default to not selected
        			, tableInitDataRs.getString(SqliteDatabase.TableData.BeatmapSet.FOLDER_NAME)
        			, tableInitDataRs.getString(SqliteDatabase.TableData.BeatmapSet.AUDIO_NAME)
        			, tableInitDataRs.getString(SqliteDatabase.TableData.SongTag.SONG_TAG_NAME).replaceAll(",", " ")
        			, tableInitDataRs.getString(SqliteDatabase.TableData.BeatmapSet.CREATOR_NAME)
        			);
        	
        	t.isSelectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean wasSelected, Boolean isSelected) {
                	if (isSelected) {
                		numOfSelectedSongs++;
                	}
                	else {
                		numOfSelectedSongs--;
                	}
                	numOfSelectedSongsLabel.setText(numOfSelectedSongsLabelText + numOfSelectedSongs); 
                }
            });
        	
        	initSongsObsList.add(t);
        }
       
        
        FilteredList<TableViewData> initSongsFilteredList = new FilteredList<TableViewData>(initSongsObsList, new CustomPredicate(""));
        PauseTransition pause = new PauseTransition(Duration.millis(200));
        this.searchBar.textProperty().addListener((obs, oldValue, newValue) -> {
        	this.songsTable.setDisable(true);
        	pause.setOnFinished(event -> {
        		initSongsFilteredList.setPredicate(new CustomPredicate(newValue));
        		this.songsTable.setDisable(false);
        	});
            pause.playFromStart();
        });
        
        // whenever the list is filtered, change currentlyVisibleLabel
        initSongsFilteredList.addListener((ListChangeListener.Change<? extends TableViewData> l) -> {
        	this.currentlyVisibleLabel.setText(this.currentlyVisibleLabelText + initSongsFilteredList.size());
        });
        
        SortedList<TableViewData> initSongsSortedList = new SortedList<TableViewData>(initSongsFilteredList);
        
        // store the references first as the following operation can trigger listener which requires these references
        this.initSongsObsList = initSongsObsList;
        this.initSongsFilteredList = initSongsFilteredList;
        this.initSongsSortedList = initSongsSortedList;
        
        
        this.totalSongsLabel.setText(this.totalSongsLabelText + initSongsObsList.size());
        
        
        
        // initialize comboBox (this is initialized here for setting config later)
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
        
		try {
			// start to load metadata
			ResultSet metadataRs = this.songsDb.selectMetadata();
			if (metadataRs.next()) {
				String userName = metadataRs.getString(SqliteDatabase.TableData.Metadata.PLAYER_NAME);
				this.userNameLabel.setText(userName);
			}
			else {
				throw new SQLException("Failed to get metadata");
			}
			
			// start to load preferences
			ResultSet configRs = this.songsDb.selectConfig();
	        if (configRs.next()) {
	        	this.pathToOsuDb = configRs.getString(SqliteDatabase.TableData.Config.PATH_TO_OSU_DB);
	        	this.pathToSongsFolder = configRs.getString(SqliteDatabase.TableData.Config.PATH_TO_SONGS_FOLDER);
	        	double soundVolume = configRs.getDouble(SqliteDatabase.TableData.Config.SOUND_VOLUME);
	        	boolean isSongSourceShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_SONG_SOURCE_SHOWN);
	        	boolean isArtistNameShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_ARTIST_NAME_SHOWN);
	        	boolean isArtistNameUnicodeShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_ARTIST_NAME_UNICODE_SHOWN);
	        	boolean isSongTitleShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_SONG_TITLE_SHOWN);
	        	boolean isSongTitleUnicodeShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_SONG_TITLE_UNICODE_SHOWN);
	        	boolean isCreatorNameShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_CREATOR_NAME_SHOWN);
	        	boolean isTotalTimeShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_TOTAL_TIME_SHOWN);
	        	boolean isIsDownloadedShown = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_IS_DOWNLOADED_SHOWN);
	        	String ordering = configRs.getString(SqliteDatabase.TableData.Config.ORDERING);
	        	boolean isRepeatToggled = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_REPEAT_TOGGLED);
	        	boolean isShuffleToggled = configRs.getBoolean(SqliteDatabase.TableData.Config.IS_SHUFFLE_TOGGLED);
	        	
	        	// ordering is empty only when it's the first time loading the app, 
	        	// so if it's first time, dun overwrite the menuItem as the data from songsDb is defaulted to false
	        	if (!ordering.isEmpty()) {
	        		// setting these will trigger the listener set in initialize,
	        		// which in turn trigger the filterList predicate. 
	        		this.songSourceShowCheckMenuItem.setSelected(isSongSourceShown);
	        		this.artistNameShowCheckMenuItem.setSelected(isArtistNameShown);
	        		this.artistNameUnicodeShowCheckMenuItem.setSelected(isArtistNameUnicodeShown);
	        		this.songTitleShowCheckMenuItem.setSelected(isSongTitleShown);
	        		this.songTitleUnicodeShowCheckMenuItem.setSelected(isSongTitleUnicodeShown);
	        		this.creatorNameShowCheckMenuItem.setSelected(isCreatorNameShown);
	        		this.totalTimeShowCheckMenuItem.setSelected(isTotalTimeShown);
	        		this.isDownloadedShowCheckMenuItem.setSelected(isIsDownloadedShown);
	        		
	        		// so by here, the 'isDownloadedShow' is already accounted for and we can get the currently visible from the list
	        		this.currentlyVisibleLabel.setText("Currently visible: " + this.initSongsFilteredList.size());
	        	}
	        	
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
				
				this.initMediaPlayerEssentials(soundVolume, isRepeatToggled, isShuffleToggled);
	        }
	        else {
	        	throw new SQLException("Failed to get config data");
	        }
		}
		// if SQLException (ie. the application can still run just that without preferences), then show alert and continue
        catch (SQLException e) {
        	logger.log(Level.WARNING, "Failed to load config from songs.db", e);
        	Alert alert = new Alert(AlertType.ERROR, "Failed to load preferences", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
        }
		// else throw and don't allow continuing
		catch (Exception e) {
			throw e;
		}
		
		// only setItems here to prevent extra work if the observableList is sorted in the config above
		this.songsTable.setItems(initSongsSortedList);
	}
	
	
	// procedure:
	// startMusic (random) -> repeat Or new song base on repeatToggleButton 
	// if repeatToggleButton is pressed while playing, set mediaPlayer to repeat or not
	// shuffleToggleButton is only used when it's time to get new song
	// modelSelection prevails all
	
	private void initMediaPlayerEssentials(double soundVolume, boolean isRepeatToggled, boolean isShuffleToggled) {
		final int speakerSize = 24;
		this.speakerIcon.setImage(new Image(getClass().getResourceAsStream("/resources/sound-on-icon.png")));
		this.speakerIcon.setSmooth(true);
		this.speakerIcon.setCache(true);
		this.speakerIcon.setFitHeight(speakerSize);
		this.speakerIcon.setFitWidth(speakerSize);
		this.speakerMuteIcon.setImage(new Image(getClass().getResourceAsStream("/resources/mute-icon.png")));
		this.speakerMuteIcon.setSmooth(true);
		this.speakerMuteIcon.setCache(true);
		this.speakerMuteIcon.setFitHeight(speakerSize);
		this.speakerMuteIcon.setFitWidth(speakerSize);
		
		// this must be set after querying database so that mediaPlayer later can refer to this value when boot
		this.mediaPlayerVolumeSlider.setValue(soundVolume);
		if (Math.abs(soundVolume) < 0.01) {
//			this.mediaPlayerSpeakerLabel.setText(this.speakerMuteUTFIcon);
			this.mediaPlayerSpeakerLabel.setGraphic(this.speakerMuteIcon);
		}
		else {
			this.mediaPlayerSpeakerLabel.setGraphic(this.speakerIcon);
		}
		
		this.mediaPlayerRepeatToggleButton.setSelected(isRepeatToggled);
		this.mediaPlayerShuffleToggleButton.setSelected(isShuffleToggled);
		
		this.songsTable.getSelectionModel().selectedItemProperty().addListener((obs, wasSelected, isSelected) -> {
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
        			this.mediaPlayerSpeakerLabel.setGraphic(this.speakerMuteIcon);
        		}
        		else {
        			this.mediaPlayerSpeakerLabel.setGraphic(this.speakerIcon);
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
//						this.playNewSong(this.getNextRowForMusic(false));
						this.songsTable.getSelectionModel().select(this.getNextRowForMusic(false));
					}); 
				}
			}
		});
		
		this.mediaPlayerPlayButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
				// this is for situation where user press stop and select new song then press play
				// in this case, the song played should be the currently selected song instead of the cached song before stop
				// selectedItem will equal to null if nothing is selected when stop
				if (this.mediaPlayer.getStatus() == Status.STOPPED && this.songsTable.getSelectionModel().getSelectedItem() != null
						&& !this.songsTable.getSelectionModel().getSelectedItem().equals(this.currentlyPlayedSong)) {
					this.playNewSong(this.songsTable.getSelectionModel().getSelectedItem());
					
				}
				else {
					if (this.mediaPlayer.getStatus() == Status.PAUSED || this.mediaPlayer.getStatus() == Status.STOPPED) {
						this.mediaPlayer.play();
					}
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
//				this.playNewSong(this.getNextRowForMusic(false));
				this.songsTable.getSelectionModel().select(this.getNextRowForMusic(false));
			}
		});
		
		this.mediaPlayerPreviousButton.setOnAction(e -> {
			if (this.mediaPlayer != null) {
//				this.playNewSong(this.getNextRowForMusic(true));
				this.songsTable.getSelectionModel().select(this.getNextRowForMusic(true));
			}
		});
	}
	
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
			try {
				this.mediaPlayer = new MediaPlayer(new Media(mp3Path.toUri().toString()));
			}
			catch (MediaException e) {
				String errorMessage =  "Failed to play " + mp3Path.getFileName().toString() + " (probably due to unsupported format)";
				logger.log(Level.WARNING, errorMessage, e);
				Alert alert = new Alert(AlertType.ERROR, errorMessage, ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
				return;
			}
			// always set to repeat so that if shuffle is removed, it can repeat itself
			this.mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
			// if repeat is not chosen, play next song when current song ends
			if (!this.mediaPlayerRepeatToggleButton.isSelected()) {
				this.mediaPlayer.setOnEndOfMedia(() -> {
//					this.playNewSong(this.getNextRowForMusic(false));
					this.songsTable.getSelectionModel().select(this.getNextRowForMusic(false));
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
			String errorMessage =  mp3Path.toString() + " is not found! Songs data is likely to be outdated. Try to 'Check for new songs'.";
			Alert alert = new Alert(AlertType.ERROR, errorMessage, ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
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
	
	
	// This is only called when stage is shown (ie. every required initialization has been done)
	public void startMusic() {
		TableViewData randomSong = this.getRandomRow();
		if (randomSong != null) {
			this.songsTable.getSelectionModel().select(randomSong);
		}
	}
	
	private TableViewData getRandomRow() {
		if (this.songsTable.getItems().isEmpty()) 
			return null;
		
		Random rand = new Random();
		return this.songsTable.getItems().get(rand.nextInt(this.songsTable.getItems().size()));
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
			List<TableViewData> songList = this.songsTable.getItems();
			int index = Collections.binarySearch(songList, this.currentlyPlayedSong, this.orderByComboBox.getSelectionModel().getSelectedItem());
			// if found, return directly
			if (index >= 0) {
				TableViewData nextSong;
				if (!reverse) {
					nextSong = index + 1 >= songList.size() ? songList.get(0) : songList.get(index + 1);
				}
				else {
					nextSong = index - 1 < 0 ? songList.get(songList.size() - 1) : songList.get(index - 1);
				}
				return nextSong;
			}
			
			// if not found, (ie. filteredList is empty or filteredList does not contain currentlyPLayedSong)
			if (this.songsTable.getItems().isEmpty()) {
				return this.currentlyPlayedSong;
			}
			else {
				// return 1st in list (ie. when filtering and currentlyPlayedSong is not in, play the first in the list)
				return this.songsTable.getItems().get(0);
			}
		}
	}
	
	
	@FXML private void sortSongs(ActionEvent event) {
		this.initSongsSortedList.setComparator(this.orderByComboBox.getSelectionModel().getSelectedItem());
	}
	
	// copySongButton
	// ! both this and hideUnhide depend on numOfSelectedSongs, so if for any reason the var goes wrong, these methods will not function
	@FXML private void copySong(ActionEvent event) {
		if (this.numOfSelectedSongs <= 0) {
			Alert alert = new Alert(AlertType.INFORMATION, "No Song is chosen!", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
			return;
		}
		
		Map<String, List<TableViewData>> selectedSongsMap = this.songsTable.getItems().stream()
				.filter(row -> row.isSelectedProperty().get())
				.collect(Collectors.groupingBy(row -> row.folderNameProperty().get()));
		
		boolean containCopiedSongs = selectedSongsMap.values().stream().anyMatch(list -> list.stream().anyMatch(row -> row.isDownloadedProperty().get()));
		boolean proceed = true;
		if (containCopiedSongs) {
			String warningText = "One or more collected songs are found in your collect list. Are you sure you want to proceed to collect those songs again? (This will result in duplicated songs in the same folder)";
			Alert duplicatedAlert = new Alert(AlertType.WARNING, warningText, ButtonType.YES, ButtonType.NO);
			ViewLoader.addStyleToAlert(duplicatedAlert);
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
				logger.log(Level.SEVERE, "Failed to get config from songs.db during loading SaveToOptionView", e);
				Alert alert = new Alert(AlertType.ERROR, "Error getting data from songs.db", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to load SaveToOptionView", e);
				Alert alert = new Alert(AlertType.ERROR, "Failed to load songs option screen", ButtonType.OK);
				ViewLoader.addStyleToAlert(alert);
				alert.showAndWait();
			}
		}
		
	}
	
	// hideUnhideButton
	@FXML private void hideUnhideSelectedSongs(ActionEvent event) throws SQLException {
		if (this.numOfSelectedSongs <= 0) {
			Alert alert = new Alert(AlertType.INFORMATION, "No Song is chosen!", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
			return;
		}
		
		try {
			String[] items = {SqliteDatabase.TableData.BeatmapSet.IS_HIDDEN};
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
			updateBeatmapSetBooleanPStatement.executeBatch();
			this.songsDb.getConn().commit();
		}
		catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to update hidden property in songs.db", e);
			Alert alert = new Alert(AlertType.ERROR, "Failed to update preference in songs.db", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
		finally {
			this.songsDb.getConn().setAutoCommit(true);
		}
	}
	
	@FXML private void selectUnselectAll(ActionEvent event) {
		boolean setValue = this.selectAllCheckBoxInCheckBoxCol.isSelected();
		ObservableList<TableViewData> obsList = this.songsTable.getItems();
		for (TableViewData row : obsList) {
			row.isSelectedProperty().set(setValue);
		}
	}
	
	// display menu radioMenuItem 
	@FXML private void displaySongs(ActionEvent event) {
		this.searchBar.clear();

		if (this.unhiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.hideUnhideButton.setText("Hide");
			this.isDownloadedShowCheckMenuItem.setVisible(true);
			this.hideUnhideButton.setVisible(true);
		}
		else if (this.hiddenSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.hideUnhideButton.setText("Unhide");
			this.isDownloadedShowCheckMenuItem.setVisible(false);
			this.hideUnhideButton.setVisible(true);
		}
		else if (this.downloadedSongsRadioMenuItemInDisplayMenu.isSelected()) {
			this.isDownloadedShowCheckMenuItem.setVisible(false);
			this.hideUnhideButton.setVisible(false);
		}
		
		this.initSongsFilteredList.setPredicate(new CustomPredicate(""));
		
	}
	
	@FXML private void resetAll(ActionEvent event) {
		String warning = "Are you sure you want to reset all data? All stored data such as collected songs, hidden songs, chosen path, and preferences"
				+ " will be lost! (Application will be restarted after reset)";
		Alert alert = new Alert(AlertType.WARNING, warning, ButtonType.YES, ButtonType.NO);
		ViewLoader.addStyleToAlert(alert);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				logger.logp(Level.INFO, this.getClass().getName(), "resetAll", "Reseting all data");
				this.restartProgram(true);
			}
		});
	}
	
	@FXML private void openDonateLink(ActionEvent event) {
		this.hostServices.showDocument(Constants.DONATE_LINK);
	}
	
	@FXML private void openImageLink(ActionEvent event) {
		this.hostServices.showDocument(Constants.IMAGE_LINK);
	}
	
	// file menu exit menuitem
	@FXML private void exit(ActionEvent event) {
		this.currentStage.fireEvent(new WindowEvent(this.currentStage, WindowEvent.WINDOW_CLOSE_REQUEST));
	}
	
	public void restartProgram(boolean deleteSongsDb) {
		try {
			if (this.mediaPlayer != null) {
				this.mediaPlayer.dispose();
			}
			this.songsDb.closeConnection();
			if (deleteSongsDb) {
				this.songsDb.deleteSongsDb();
			}
			this.currentStage.hide();
			Main newApp = new Main();
			newApp.start(new Stage());
		} 
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to restart program", e);
			Alert restartAlert = new Alert(AlertType.ERROR, "Failed to restart", ButtonType.OK);
			ViewLoader.addStyleToAlert(restartAlert);
			restartAlert.showAndWait();
		}
	}
	
	@FXML private void fullBeatmapsUpdate(ActionEvent event) {
		try {
			this.loadUpdateDataView();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to load UpdateDetailsView", e);
			Alert alert = new Alert(AlertType.ERROR, "Failed to load update window", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
	}
	
	private void loadUpdateDataView() throws IOException {
		Stage updateDetailsStage = new Stage();
		updateDetailsStage.setTitle("Update Songs Data");
		updateDetailsStage.setResizable(false);
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/LoadingDialogParentView.fxml"));
		UpdateDataInSongsDisplayController ctr = new UpdateDataInSongsDisplayController();
		loader.setController(ctr);
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		updateDetailsStage.initModality(Modality.WINDOW_MODAL);
		updateDetailsStage.initOwner(this.currentStage);
		updateDetailsStage.setScene(scene);
		// the last two paths must have already initialized to come to here
		ctr.newInitDataAndStart(this, updateDetailsStage, this.songsDb, this.pathToOsuDb, this.pathToSongsFolder);
		updateDetailsStage.setScene(scene);
		updateDetailsStage.show();
	}
	
	private void loadSaveToOptionView(Map<String, List<TableViewData>> selectedSongsMap) throws SQLException, IOException {
		Stage saveToOptionStage = new Stage();
		saveToOptionStage.setTitle("Options");
		saveToOptionStage.setResizable(false);
		// pretty save to use this here as the new stage opened is not minimizable
		// if that changes later, this must be changed as well
		saveToOptionStage.showingProperty().addListener((obs, oldValue, newValue) -> {
			if (!newValue) { // when closed
				// so that copied songs can be reflected in the table 
				this.songsTable.refresh();
			}
		});
		
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/SaveToOptionView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		SaveToOptionController ctr = loader.<SaveToOptionController>getController();
		saveToOptionStage.initModality(Modality.WINDOW_MODAL);
		saveToOptionStage.initOwner(this.currentStage);
		saveToOptionStage.setScene(scene);
		ctr.setHostServices(this.hostServices);
		ctr.initData(saveToOptionStage, this.songsDb, selectedSongsMap, this.artistNameUnicodeCol.isVisible(), this.songTitleUnicodeCol.isVisible());
		saveToOptionStage.show();
	}
	
	private void updatePreference() throws SQLException {
		ResultSet configRs = this.songsDb.selectConfig();
		if (configRs.next()) {
			int configID = configRs.getInt(SqliteDatabase.TableData.Config.CONFIG_ID);
			String pathToOsuDb = configRs.getString(SqliteDatabase.TableData.Config.PATH_TO_OSU_DB);
			String pathToSongsFolder = configRs.getString(SqliteDatabase.TableData.Config.PATH_TO_SONGS_FOLDER);
			String saveFolder = configRs.getString(SqliteDatabase.TableData.Config.SAVE_FOLDER);
			String ordering = this.orderByComboBox.getSelectionModel().getSelectedItem().toString();
			String comboBoxPrefix = configRs.getString(SqliteDatabase.TableData.Config.COMBO_BOX_PREFIX);
			String comboBoxSuffix = configRs.getString(SqliteDatabase.TableData.Config.COMBO_BOX_SUFFIX);
			this.songsDb.updateConfigFull(configID, pathToOsuDb, pathToSongsFolder, saveFolder
					, this.songSourceShowCheckMenuItem.isSelected(), this.artistNameShowCheckMenuItem.isSelected(), this.artistNameUnicodeShowCheckMenuItem.isSelected()
					, this.songTitleShowCheckMenuItem.isSelected(), this.songTitleUnicodeShowCheckMenuItem.isSelected(), this.creatorNameShowCheckMenuItem.isSelected()
					, this.totalTimeShowCheckMenuItem.isSelected(), this.isDownloadedShowCheckMenuItem.isSelected(), ordering, this.mediaPlayerVolumeSlider.getValue()
					, this.mediaPlayerRepeatToggleButton.isSelected(), this.mediaPlayerShuffleToggleButton.isSelected(), comboBoxPrefix
					, comboBoxSuffix);
		}
		else {
			throw new SQLException("No config data is found");
		}
	}
	
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
			
			// to speed up searching becuz if this is false, return value at the end is always false
			if (!displayCondition) {
				return false;
			}
			
			if (this.searchedText.isEmpty()) {
				return displayCondition && true;
			}
			
			String[] words = searchedText.toLowerCase().split("\\s+");
			
			// if search for length specifically
			if (this.matchLengthFilter(words)) {
				return this.matchLengthCondition(words[0], row.totalTimeProperty().get());
			}
			
			// normal search
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
	
	public static class TableViewData {
		private final SimpleIntegerProperty beatmapSetAutoID;
		private final SimpleStringProperty songSource;
		private final SimpleStringProperty artistName;
		private final SimpleStringProperty artistNameUnicode;
		private final SimpleStringProperty songTitle;
		private final SimpleStringProperty songTitleUnicode;
		private final SimpleIntegerProperty totalTime;
		private final SimpleLongProperty lastModificationTime;
		private final SimpleBooleanProperty isDownloaded;
		private final SimpleBooleanProperty isHidden;
		private final SimpleBooleanProperty isSelected;
		private final SimpleStringProperty folderName;
		private final SimpleStringProperty audioName;
		private final SimpleStringProperty songTagNames;
		private final SimpleStringProperty creatorName;
		
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

