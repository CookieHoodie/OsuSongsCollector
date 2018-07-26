package application;
	
import controllers.InitScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Main extends Application {
	// centralized constants
	public static final String OSU_EXE = "osu!.exe";
	public static final String DB_NAME = "songs.db";
	public static final String OSU_DB_NAME = "osu! - Copy.db";
	public static final String SONGS_FOLDER = "Songs";
	
	// TODO: change OSU_DB_NAME
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/fxml/InitScreenView.fxml"));
//			BorderPane root = loader.load();
			StackPane root = loader.load();
			Scene scene = new Scene(root);
			primaryStage.initStyle(StageStyle.TRANSPARENT);
			scene.setFill(Color.TRANSPARENT);
			primaryStage.setScene(scene);
			primaryStage.show();
			// show the screen 1st, then start the checking progress
			// and handle subsequent processes in the controllers
			InitScreenController initScreenController = loader.<InitScreenController>getController();
			initScreenController.startChecking();
		}
		catch (Exception e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR, "Failed to launch the program!", ButtonType.OK);
			ViewLoader.addStyleToAlert(alert);
			alert.showAndWait();
		}
		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
