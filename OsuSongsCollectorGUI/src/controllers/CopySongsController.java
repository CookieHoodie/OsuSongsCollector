package controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import application.SqliteDatabase;
import controllers.SaveToOptionController.ComboBoxChoice;
import controllers.SongsDisplayController.TableViewData;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;

public class CopySongsController {
	@FXML private ProgressBar copyProgressBar;
	@FXML private TextArea copyDetailsTextArea;
	@FXML private Button cancelButton;
	
	private Task<Void> copySongsTask;
	private SqliteDatabase songsDb;
	private Stage currentStage;
	
	public void initDataAndStart(Stage currentStage, SqliteDatabase songsDb, List<TableViewData> selectedSongsList, String pathToSongsFolder, String destinationFolder, ComboBoxChoice prefix, ComboBoxChoice suffix) {
		this.currentStage = currentStage;
		this.songsDb = songsDb;
		this.copySongsTask = new CopySongsTask(selectedSongsList, pathToSongsFolder, destinationFolder, prefix, suffix);
		// if still copying when 'X' button is pressed, show warning. Else, close.
		this.currentStage.setOnCloseRequest(e -> {
			if (this.copySongsTask.isRunning()) {
				this.cancelTaskAlert();
				e.consume();
			}
		});
		this.startCopying();
	}
	
	private void startCopying() {
		this.copyProgressBar.progressProperty().bind(this.copySongsTask.progressProperty());
		copySongsTask.messageProperty().addListener((obs, oldValue, newValue) -> {
			this.copyDetailsTextArea.appendText(newValue + "\n");
		});
		
		copySongsTask.setOnCancelled(e -> {
			this.copyDetailsTextArea.appendText("Cancelling. Waiting for the last song to finish...\n");
			this.cancelButton.setDisable(true);
		});
		copySongsTask.setOnSucceeded(e -> {
//			this.copyDetailsTextArea.appendText("Finish!");
			this.cancelButton.setDisable(true);
		});
		copySongsTask.setOnFailed(e -> {
			this.cancelButton.setDisable(true);
			copySongsTask.getException().printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Error occured while copying songs: " + copySongsTask.getException().getMessage(), ButtonType.OK);
			alert.showAndWait();
		});
		new Thread(copySongsTask).start();
	}
	
	// cancelButton
	@FXML private void stopCopying(ActionEvent event) {
		this.cancelTaskAlert();
	}
	
	private void cancelTaskAlert() {
		Alert alert = new Alert(AlertType.WARNING, "Are you sure you want to cancel copying?", ButtonType.YES, ButtonType.NO);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				this.copySongsTask.cancel();
			}
		});
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
				return prefix.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + separator + suffix.trim().replaceAll("[\\\\/:*?\"<>|]", "_") + " (" + (occurance+1) + ")" + audioName.substring(audioName.lastIndexOf('.'));
			}
		}
		
		private String getFileNamePart(TableViewData row, ComboBoxChoice sufOrPreFix) {
			String fileNamePart;
			// if Source is empty, cascade to artistNameUnicode and finally artistName if still empty
			// if songTitleUnicode is empty, cascade to songTitle only
			switch (sufOrPreFix) {
				case NONE: {
					fileNamePart = "";
					break;
				}
				case SONG_SOURCE: {
					fileNamePart = row.songSourceProperty().get();
					if (!fileNamePart.isEmpty()) {
						break;
					}
				}
				case ARTIST_NAME_UNICODE: {
					fileNamePart = row.artistNameUnicodeProperty().get();
					if (!fileNamePart.isEmpty()) {
						break;
					}
				}
				case ARTIST_NAME: {
					fileNamePart = row.artistNameProperty().get();
					break;
				}
				case SONG_TITLE_UNICODE: {
					fileNamePart = row.songTitleProperty().get();
					if (!fileNamePart.isEmpty()) {
						break;
					}
				}
				case SONG_TITLE: {
					fileNamePart = row.songTitleProperty().get();
					break;
				}
				
				default: throw new RuntimeException("Invalid options");
			}
			return fileNamePart;
		}
		
		// TODO: if possible, use another way instead of congesting UI Thread with runlater like this
		private void appendTextArea(final String text) {
			Platform.runLater(() -> {
				copyDetailsTextArea.appendText(text + "\n");
			});
		}
		
		@Override
        protected Void call() throws Exception {
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
						// check if exist and if not, log error and continue 
						Path oriPath = Paths.get(this.pathToSongsFolderInTask, row.folderNameProperty().get(), row.audioNameProperty().get());
						File oriFile = oriPath.toFile();
						if (!oriFile.exists()) {
//							updateMessage("(" + (i+1) + "/" + totalProgress + ") " + " Error: " + oriPath.toString() + " is not found!");
							this.appendTextArea("(" + (i+1) + "/" + totalProgress + ") " + " Error: " + oriPath.toString() + " is not found!");
							updateProgress(i + 1, totalProgress);
							continue;
						}
						
						int occurance = 0;
						String fileNamePrefix = this.getFileNamePart(row, this.prefix);
						String fileNameSuffix = this.getFileNamePart(row, this.suffix);
						String fileName = this.formatFileName(fileNamePrefix, fileNameSuffix, row.audioNameProperty().get(), occurance);
						
						Path cpPath = Paths.get(this.destinationFolder, fileName);
						File cpFile = cpPath.toFile();
						if (cpFile.exists()) {
							fileNameSuffix += " (" + TableViewData.totalTimeToString(row.totalTimeProperty().get()).replace(":", "-") + ")";
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
						
						// combine all together instead of using separate this.appendTextArea to reduce overhead
						final String updateText = "(" + (i+1) + "/" + totalProgress + ") " + fileName + "\n";
						Platform.runLater(() -> {
							row.isDownloadedProperty().set(true);
							row.isSelectedProperty().set(false);
							copyDetailsTextArea.appendText(updateText);
						}); 
						updateProgress(i + 1, totalProgress);
//						updateMessage("(" + (i+1) + "/" + totalProgress + ") " + fileName);
//						this.appendTextArea("(" + (i+1) + "/" + totalProgress + ") " + fileName);
					}
					else {
//						updateMessage(i + " songs are copied, " + (totalProgress-i-1) + " are cancelled.");
						this.appendTextArea(i + " songs are copied, " + (totalProgress - i) + " are cancelled.");
						break;
					}
				}
//				updateMessage("Updating copied songs data...");
				this.appendTextArea("Updating copied songs data... Please don't close the window!");
				updateBeatmapSetBooleanPStatement.executeBatch();
				songsDb.getConn().commit();
			}
			finally {
				this.appendTextArea("Finish");
				songsDb.getConn().setAutoCommit(true);
			}
			return null;
        }
    }
	
}
