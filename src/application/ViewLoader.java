package application;

import java.io.IOException;
import java.sql.SQLException;

import controllers.LoadingDialogParentController;
import controllers.SongsDisplayController;
import controllers.UpdateDataController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;


// class for reuse purpose. Loaders which only associate with one controller (ie. no repeated code) are not included here
public class ViewLoader {
	// hide currentStage and open up new SongsDisplay stage, setting title, starting music by default
	public static void loadNewSongsDisplayView(Stage currentStage, SqliteDatabase connectedSongsDb) throws SQLException, IOException {
		Stage songsDisplayStage = new Stage();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(ViewLoader.class.getResource("/fxml/SongsDisplayView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
		
		songsDisplayStage.setTitle(currentStage.getTitle());
		songsDisplayStage.setScene(scene);
		ctr.initData(songsDisplayStage, connectedSongsDb);
		songsDisplayStage.show();
		ctr.startMusic(); // TODO: if decide, add this to updateData and LoadAndCreateDB
		currentStage.hide();
	}
	
//	public static void loadNewUpdateDataView(Stage currentStage, SqliteDatabase songsDb, String pathToOsuDb
//			, String pathToSongsFolder, boolean closeCurrentStage) throws IOException {
//		Stage updateDataStage = new Stage();
//		FXMLLoader loader = new FXMLLoader();
//		loader.setLocation(ViewLoader.class.getResource("/fxml/LoadingDialogParentView.fxml"));
//		UpdateDataController ctr = new UpdateDataController();
//		loader.setController(ctr);
//		BorderPane root = loader.load();
//		Scene scene = new Scene(root);
//		
//		updateDataStage.setTitle("Update Songs Data");
//		updateDataStage.setScene(scene);
//		// the last two paths must have already initialized to come to here
//		ctr.initDataAndStart(updateDataStage, songsDb, pathToOsuDb, pathToSongsFolder);
//		updateDataStage.setScene(scene);
//		updateDataStage.show();
//		if (closeCurrentStage) {
//			currentStage.hide();
//		}
//		else {
//			updateDataStage.initModality(Modality.WINDOW_MODAL);
//			updateDataStage.initOwner(currentStage);
//		}
//		
//	}
}
