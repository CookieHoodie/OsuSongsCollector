package application;

import java.io.IOException;
import java.sql.SQLException;

import controllers.LoadingDialogParentController;
import controllers.SongsDisplayController;
import controllers.UpdateDataController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;


// class for reuse purpose. Loaders which only associate with one controller (ie. no repeated code) are not included here
public class ViewLoader {
	// hide currentStage and open up new SongsDisplay stage, setting title, starting music by default
	public static void loadNewSongsDisplayView(Stage currentStage, SqliteDatabase connectedSongsDb) throws SQLException, IOException {
		Stage songsDisplayStage = new Stage();
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(ViewLoader.class.getResource("/fxml/SongsDisplayView.fxml"));
		StackPane root = loader.load();
		Scene scene = new Scene(root);
		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
		
		songsDisplayStage.setTitle(currentStage.getTitle());
		songsDisplayStage.setScene(scene);
		ctr.initData(songsDisplayStage, connectedSongsDb);
		songsDisplayStage.show();
		ctr.startMusic(); 
		currentStage.hide();
	}
	
	public static void addStyleToAlert(Alert alert) {
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().add(ViewLoader.class.getResource("/css/alert.css").toExternalForm());
		dialogPane.getStyleClass().add("dialog-root");
	}
}
