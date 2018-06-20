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

import application.Main;
import application.OsuDbParser;
import application.SqliteDatabase;
import controllers.SongsDisplayController.TableViewData;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SaveToOptionController {
	@FXML private Label testDragLabel1;
	@FXML private Label testDragLabel2;
	@FXML private Label testDragLabel3;
	
	@FXML private Label instructionLabel;
	@FXML private TextField chosenPathTextField;
	@FXML private Button choosePathButton;
	@FXML private CheckBox rememberPathCheckBox;
	@FXML private Button startButton;
	@FXML private ProgressBar downloadProgressBar;
	@FXML private Button cancelButton;
	@FXML private TextArea taskDetailsTextArea;
	
	private Stage currentStage;
	private SqliteDatabase songsDb;
	private List<TableViewData> selectedSongsList;
	
	// all initially from db, should not be changed
	private int metadataID;
	private String pathToSongsFolder = "";
	private String saveFolder = "";
	
	private Task<List<TableViewData>> task;
	
	// TODO: figure out a way to update the observableList (isdownloaded) according to what's been copied. 
	// the List selectedDataList may need to be modified to do this.
	// TODO: unselect all checked checkbox after closing stage
	
	public void initData(Stage currentStage, SqliteDatabase songsDb, List<TableViewData> selectedSongsList) throws FileNotFoundException, SQLException {
		// assigning to member all data passed in
		this.songsDb = songsDb;
		this.currentStage = currentStage;
		this.selectedSongsList = selectedSongsList;
		// get metadata from database to initialize some needed variables
		ResultSet rs = this.songsDb.selectMetadata();
		if (rs.next()) {
			this.metadataID = rs.getInt(this.songsDb.Data.Metadata.METADATA_ID);
			this.saveFolder = rs.getString(this.songsDb.Data.Metadata.SAVE_FOLDER);
			this.pathToSongsFolder = rs.getString(this.songsDb.Data.Metadata.PATH_TO_SONGS_FOLDER);
			if (!this.saveFolder.isEmpty()) {
				this.chosenPathTextField.setText(this.saveFolder);
				this.rememberPathCheckBox.setSelected(true);
				// TODO: make sure naming option is set as well before enable
				this.startButton.setDisable(false);
			}
		}
		// shouldn't be the case
		else {
			throw new SQLException("No metadata available?");
		}
		
//		class Delta {
//	        double x, y;
//	    }
//
//		Delta dragDelta = new Delta();
//		testDragLabel1.setOnMousePressed(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                // record a delta distance for the drag and drop operation.
//            	dragDelta.x = testDragLabel1.getLayoutX() - mouseEvent.getSceneX();
//            	dragDelta.y = testDragLabel1.getLayoutY() - mouseEvent.getSceneY();
//                testDragLabel1.setCursor(Cursor.MOVE);
//            }
//        });
//		testDragLabel1.setOnMouseReleased(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//            	testDragLabel1.setCursor(Cursor.HAND);
//            }
//        });
//		testDragLabel1.setOnMouseDragged(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//            	testDragLabel1.setLayoutX(mouseEvent.getSceneX() + dragDelta.x);
//            	testDragLabel1.setLayoutY(mouseEvent.getSceneY() + dragDelta.y);
//            }
//        });
//		testDragLabel1.setOnMouseEntered(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//            	testDragLabel1.setCursor(Cursor.HAND);
//            }
//        });
		
	}
	
	// choosePath Button
	@FXML private void promptToChoosePath(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(this.currentStage);
          
        if (selectedDirectory == null) {
    		this.chosenPathTextField.setText("No path selected");
        	if (!this.startButton.isDisable()) {
        		this.startButton.setDisable(true);
        	}
        }
        else {
    		this.chosenPathTextField.setText(selectedDirectory.getAbsolutePath());
    		this.startButton.setDisable(false);
    		this.startButton.requestFocus();
    		
        }
	}
	
	// start Button
	@FXML private void startCopying(ActionEvent event) throws SQLException, Exception {
		if (this.rememberPathCheckBox.isSelected() && !this.chosenPathTextField.getText().equals(this.saveFolder)) {
			String[] items = {this.songsDb.Data.Metadata.SAVE_FOLDER};
			String[] results = {this.chosenPathTextField.getText()};
			this.songsDb.updateMetadata(this.metadataID, items, results);
		}
		
		Task<List<TableViewData>> copySongsTask = new CopySongsTask(this.selectedSongsList, this.pathToSongsFolder, this.chosenPathTextField.getText()); 
		this.task = copySongsTask;
		this.downloadProgressBar.progressProperty().bind(copySongsTask.progressProperty());
		copySongsTask.messageProperty().addListener((obs, oldValue, newValue) -> {
			this.taskDetailsTextArea.appendText(newValue + "\n");
		});
		copySongsTask.setOnCancelled(e -> {
			this.taskDetailsTextArea.appendText("Cancelling. Waiting for the last song to finish...");
		});
		copySongsTask.setOnSucceeded(e -> {
			this.taskDetailsTextArea.appendText("All songs are successfully copied!");
		});
		new Thread(copySongsTask).start();
	}
	
	
	@FXML private void cancelCopySongs(ActionEvent event) {
		this.task.cancel();
	}
	
	
	private class CopySongsTask extends Task<List<TableViewData>> {
		private final String pathToSongsFolderInTask;
		private final String destinationFolder;
		private final List<TableViewData> selectedSongsListInTask;
		private List<TableViewData> failedSongs = new ArrayList<TableViewData>();
		
		public CopySongsTask(List<TableViewData> selectedSongsList, String pathToSongsFolder, String destinationFolder) {
			this.selectedSongsListInTask = selectedSongsList;
			this.pathToSongsFolderInTask = pathToSongsFolder;
			this.destinationFolder = destinationFolder;
		}
		
		@Override
        protected List<TableViewData> call() throws Exception {
//			int currentProgress = 0;
//			boolean isCompleted = true;
			updateProgress(0, 0);
			int totalProgress = this.selectedSongsListInTask.size();
			String[] items = {songsDb.Data.BeatmapSet.IS_DOWNLOADED};
			Boolean[] results = {true};
			PreparedStatement updateBeatmapSetBooleanPStatement = songsDb.getUpdateBeatmapSetBooleanPreparedStatement(items);
			for (int i = 0; i < totalProgress; i++) {
				if (!isCancelled()) {
					TableViewData row = this.selectedSongsListInTask.get(i);
					if (row.isSelectedProperty().get()) {
						int beatmapSetAutoID = row.beatmapSetAutoIDProperty().get();
						Path oriPath = Paths.get(this.pathToSongsFolderInTask, row.folderNameProperty().get(), row.audioNameProperty().get());
						// TODO: if unicode, use english if empty
						// with also option to 
						// let user choose how to deal with duplicated files (such as using length)
						// warn user if they change the order of the filename as old files does not recognize the previous one
						String fileName = row.artistNameProperty().get().trim().replaceAll("[\\\\/:*?\"<>|]", "_") + " - " + row.songTitleProperty().get().trim().replaceAll("[\\\\/:*?\"<>|]", "_") + row.audioNameProperty().get().substring(row.audioNameProperty().get().lastIndexOf('.'));
						Path cpPath = Paths.get(this.destinationFolder, fileName);
						try {
							Files.copy(oriPath, cpPath);
							songsDb.addUpdateBeatmapSetBatch(updateBeatmapSetBooleanPStatement, beatmapSetAutoID, results);
							Platform.runLater(() -> {
								row.isDownloadedProperty().set(true);
								row.isSelectedProperty().set(false);
							}); 
							updateProgress(i + 1, totalProgress);
							updateMessage(fileName + " --- Done");
						}
						catch (UnsupportedOperationException e) {
							// TODO: throw all these exceptions instead
						}
						catch (FileAlreadyExistsException e) {
							// TODO: change according to user option
						}
						catch (IOException e) {
							
						}
						catch (SecurityException e) {
							
						}
						
					}
//					currentProgress++;
				}
				else {
					updateMessage((i+1) + " songs are copied, " + (totalProgress-i-1) + " are cancelled.");
					break;
				}
//				else {
//					if (i + 1 != totalProgress) {
//						isCompleted = false;
//					}
//					break;
//				}
			}
//			if (!isCompleted) {
//				for (int i = currentProgress; i < totalProgress; i++) {
//					TableViewData row = this.tableViewObservableListInTask.get(i);
//					if (row.isSelectedProperty().get()) {
//						this.failedSongs.add(row);
//					}
//				}
//			}
			updateBeatmapSetBooleanPStatement.executeBatch();
			return this.failedSongs;
        }
    }
	
	//TODO: allow cancel button, when it's pressed (or closed is pressed) popup window warning then send isCancelled signal if true
}
