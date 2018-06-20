package application;
	
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import controllers.InitScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;


public class Main extends Application {
	// centralized constants
	public static final String OSU_EXE = "osu!.exe";
	public static final String DB_NAME = "songs.db";
	public static final String OSU_DB_NAME = "osu! - Copy.db";
	public static final String SONGS_FOLDER = "Songs";
	
	// TODO: change all testlabels etc. in controllers to proper names
	
	@Override
	public void start(Stage primaryStage) throws IOException, SQLException {
		primaryStage.setTitle("Osu! Songs Collector");
//		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/InitScreenView.fxml"));
		BorderPane root = loader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
		// show the screen 1st, then start the checking progress
		// and handle subsequent processes in the controllers
		InitScreenController initScreenController = loader.<InitScreenController>getController();
		initScreenController.startChecking();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
