package controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import application.Constants;
import application.SqliteDatabase;
import application.ViewLoader;
import controllers.SaveToOptionController.ComboBoxChoice;
import controllers.SongsDisplayController.TableViewData;
import javafx.animation.AnimationTimer;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class CopySongsController {
	private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private HostServices hostServices;
	public void setHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}
	
	@FXML private ProgressBar copyProgressBar;
	@FXML private TextArea copyDetailsTextArea;
	@FXML private Button donateButton;
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
		
		copySongsTask.setOnCancelled(e -> {
			this.copyDetailsTextArea.appendText("Cancelling. Waiting for the last song to finish...\n");
			this.cancelButton.setDisable(true);
		});
		copySongsTask.setOnSucceeded(e -> {
			this.cancelButton.setDisable(true);
		});
		copySongsTask.setOnFailed(e -> {
			Throwable e1 = copySongsTask.getException();
			this.cancelButton.setDisable(true);
			logger.logp(Level.SEVERE, this.getClass().getName(), "startCopying", "Failed to copy songs", e1);
			Alert alert = new Alert(AlertType.ERROR, "Error occured while collecting songs: " + e1.getMessage(), ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		});
		new Thread(copySongsTask).start();
	}
	
	@FXML private void openDonateLink(ActionEvent event) {
		this.hostServices.showDocument(Constants.DONATE_LINK);
	}
	
	// cancelButton
	@FXML private void stopCopying(ActionEvent event) {
		this.cancelTaskAlert();
	}
	
	private void cancelTaskAlert() {
		Alert alert = new Alert(AlertType.WARNING, "Are you sure you want to cancel collecting?", ButtonType.YES, ButtonType.NO);
		ViewLoader.addStyleToAlert(alert);
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.YES) {
				logger.logp(Level.INFO, this.getClass().getName(), "cancelTaskAlert", "Canceling copySongsTask");
				this.copySongsTask.cancel();
			}
		});
	}
	
	// for update message in task
	private class MessageConsumer extends AnimationTimer {
		private final BlockingQueue<String> messageQueue;
		private final TextArea textArea;
		public final String poisonPill = MessageConsumer.class.getName();
		
		public MessageConsumer(BlockingQueue<String> messageQueue, TextArea textArea) {
			this.messageQueue = messageQueue;
			this.textArea = textArea;
		}
		
		@Override public void handle(long now) {
			List<String> messages = new ArrayList<String>();
	        messageQueue.drainTo(messages);
	        StringBuilder sb = new StringBuilder();
	        for (String message : messages) {
	        	if (message.equals(this.poisonPill)) {
	        		// stop and break out to append the last text if any
	        		this.stop();
	        		break;
	        	}
	        	sb.append(message + "\n");
	        }
	        this.textArea.appendText(sb.toString());
		}
	}
	
	private class CopySongsTask extends Task<Void> {
		private final String pathToSongsFolderInTask;
		private final String destinationFolder;
		private final ComboBoxChoice prefix;
		private final ComboBoxChoice suffix;
		private final List<TableViewData> selectedSongsListInTask;
		private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
		
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
				
				default: throw new RuntimeException("Invalid filename options");
			}
			return fileNamePart;
		}
		
		// as put is a blocking call, InterruptedException is likely to be thrown if task is cancelled
		// so wrap in try and catch and put again so that the message can be updated anyway
		private void putMessageToQueue(String message) throws InterruptedException {
			try {
				messageQueue.put(message);
			}
			catch (InterruptedException e) {
				if (this.isCancelled()) {
					messageQueue.put(message);
				}
				else {
					// should not be happening so throw it to be handled at higher level
					throw e;
				}
			}
		}
		
		
		@Override
        protected Void call() throws Exception {
			updateProgress(0, 1);
			final int batchSize = 500;
			int copiedCount = 0;
			int totalProgress = this.selectedSongsListInTask.size();
			String[] items = {SqliteDatabase.TableData.BeatmapSet.IS_DOWNLOADED};
			Boolean[] results = {true};
			MessageConsumer messageConsumer = new MessageConsumer(messageQueue, copyDetailsTextArea);
			Platform.runLater(() -> {
				messageConsumer.start();
			});
			
			try {
				songsDb.getConn().setAutoCommit(false);
				PreparedStatement updateBeatmapSetBooleanPStatement = songsDb.getUpdateBeatmapSetBooleanPStatement(items);
				for (int i = 0; i < totalProgress; i++) {
					if (!isCancelled()) {
						TableViewData row = this.selectedSongsListInTask.get(i);
						int beatmapSetAutoID = row.beatmapSetAutoIDProperty().get();
						// check if exist and if not, log error and continue 
						Path oriPath = Paths.get(this.pathToSongsFolderInTask, row.folderNameProperty().get(), row.audioNameProperty().get());
						File oriFile = oriPath.toFile();
						if (!oriFile.exists()) {
							messageQueue.put("(" + (i+1) + "/" + totalProgress + ") " + " Error: " + oriPath.toString() + " is not found!");
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
							// directly throw and exit as these errors are likely to persist without user action
							messageQueue.put(e.getMessage());
							updateBeatmapSetBooleanPStatement.executeBatch();
							songsDb.getConn().commit();
							throw e;
						}

						Platform.runLater(() -> {
							row.isDownloadedProperty().set(true);
							row.isSelectedProperty().set(false);
						});

						updateProgress(i + 1, totalProgress);
						this.putMessageToQueue("(" + (i+1) + "/" + totalProgress + ") " + fileName);

                        songsDb.addUpdateBeatmapSetBatch(updateBeatmapSetBooleanPStatement, beatmapSetAutoID, results);

                        // use copiedCount instead of 'i' as i can skip if the mp3 does not exist
                        copiedCount++;
                        if (copiedCount % batchSize == 0) {
                            updateBeatmapSetBooleanPStatement.executeBatch();
                            songsDb.getConn().commit();
                        }
					}
					else {
						this.putMessageToQueue(i + " songs are collected, " + (totalProgress - i) + " are cancelled.");
						break;
					}
				}
				this.putMessageToQueue("Updating collected songs data... Please don't close the window!");
				updateBeatmapSetBooleanPStatement.executeBatch();
				songsDb.getConn().commit();
			}
			finally {
				songsDb.getConn().setAutoCommit(true);
				this.putMessageToQueue("Finish");
				// stopping the AnimationTimer
				this.putMessageToQueue(messageConsumer.poisonPill);
			}
			return null;
        }
    }
	
}
