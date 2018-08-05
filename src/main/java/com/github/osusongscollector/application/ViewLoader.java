package com.github.osusongscollector.application;

import com.github.osusongscollector.controllers.SongsDisplayController;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;


// class for reuse purpose. Loaders which only associate with one controller (ie. no repeated code) are not included here
public class ViewLoader {
	// hide currentStage and open up new SongsDisplay stage, setting title, starting music by default
	public static void loadNewSongsDisplayView(Stage currentStage, SqliteDatabase connectedSongsDb, HostServices hostServices) throws SQLException, IOException {
		Stage songsDisplayStage = new Stage();
        songsDisplayStage.getIcons().addAll(currentStage.getIcons());
		songsDisplayStage.setTitle("osu! Songs Collector");
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(ViewLoader.class.getResource("/com/github/osusongscollector/fxml/SongsDisplayView.fxml"));
		StackPane root = loader.load();
		Scene scene = new Scene(root);
		SongsDisplayController ctr = loader.<SongsDisplayController>getController();
		
		songsDisplayStage.setScene(scene);
		ctr.setHostServices(hostServices);
		ctr.initData(songsDisplayStage, connectedSongsDb);
		songsDisplayStage.show();
		ctr.startMusic(); 
		currentStage.hide();
	}
	
	public static void addStyleToAlert(Alert alert) {
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().addAll(ViewLoader.class.getResource("/com/github/osusongscollector/css/shared.css").toExternalForm(),
				ViewLoader.class.getResource("/com/github/osusongscollector/css/simpleDialog.css").toExternalForm());
		dialogPane.getStyleClass().addAll("root-pane", "bigger-font");
		Label headerLabel = (Label) ((GridPane) dialogPane.getChildren().get(0)).getChildren().get(0);
		headerLabel.getStyleClass().add("instruction-label");
		ButtonBar buttonBar = (ButtonBar) dialogPane.getChildren().get(2);
		buttonBar.getButtons().forEach(button -> button.getStyleClass().add("button-design"));
	}
}
