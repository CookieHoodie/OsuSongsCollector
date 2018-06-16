package controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import application.Main;
import application.OsuDbParser;
import application.SqliteDatabase;
import controllers.SongsDisplayController.TableViewData;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
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
	
	private Stage currentStage;
	private SqliteDatabase songsDb;
	private List<SongsDisplayController.TableViewData> selectedDataList;
	
	// all initially from db, should not be changed
	private int metadataID;
	private String pathToSongsFolder = "";
	private String saveFolder = "";
	
	// TODO: figure out a way to update the observableList (isdownloaded) according to what's been copied. 
	// the List selectedDataList may need to be modified to do this.
	// TODO: unselect all checked checkbox after closing stage
	
	public void initData(Stage currentStage, SqliteDatabase songsDb, List<SongsDisplayController.TableViewData> selectedDataList) throws FileNotFoundException, SQLException {
		this.songsDb = songsDb;
		this.selectedDataList = selectedDataList;
		this.currentStage = currentStage;
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
	
	// choose Path Button
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
			System.out.println(this.metadataID + " - " + items[0] + " - " + results[0]);
			this.songsDb.updateMetadata(this.metadataID, items, results);
		}
		for (TableViewData row : this.selectedDataList) {
			Path oriPath = Paths.get(this.pathToSongsFolder, row.folderNameProperty().get(), row.audioNameProperty().get());
//			String fileName = row.songTitleProperty().get();
			// TODO: if unicode, use english if empty
			// with also option to 
			// let user choose how to deal with duplicated files (such as using length)
			// warn user if they change the order of the filename as old files does not recognize the previous one
			String fileName = row.artistNameProperty().get().trim().replaceAll("[\\\\/:*?\"<>|]", "_") + " - " + row.songTitleProperty().get().trim().replaceAll("[\\\\/:*?\"<>|]", "_") + row.audioNameProperty().get().substring(row.audioNameProperty().get().lastIndexOf('.'));
			Path cpPath = Paths.get(this.chosenPathTextField.getText(), fileName);
			try {
				Files.copy(oriPath, cpPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.currentStage.hide();
		// TODO: do in thread and close the stage after starting
	}
	
//	private Task<OsuDbParser> getLoadOsuDbTask() {
//		return new Task<OsuDbParser>() {
//			@Override
//	        protected OsuDbParser call() throws Exception {
//				OsuDbParser osuDb = new OsuDbParser(fullPathToOsuDb, pathToSongsFolder);
//				osuDb.setThreadData((workDone, totalWork) -> updateProgress(workDone, totalWork));
//				osuDb.startParsing();
//	            return osuDb;
//	        }
//		};
//    }
}
