package application;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class WindowTitleBar extends ButtonBar {
	private Button minimizeBtn = new Button("🗕");
	private Button enlargeBtn = new Button("🗖");
	private Button closeBtn = new Button();
	private boolean isEnlarged = false;
	
	public WindowTitleBar(boolean enlargable) {
		enlargeBtn.setDisable(!enlargable);
		Image cancelIcon = new Image(getClass().getResourceAsStream("/resources/cancel-icon2.png"));
		closeBtn.setGraphic(new ImageView(cancelIcon));
		
        closeBtn.setOnAction(event -> {
        	Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        	currentStage.close();
        });
        
        minimizeBtn.setOnAction(event -> {
        	Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        	currentStage.setIconified(true);
        });
        
        enlargeBtn.setOnAction(event -> {
        	Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        	this.isEnlarged = !this.isEnlarged;
        	currentStage.setMaximized(this.isEnlarged);
        });
        this.getButtons().addAll(minimizeBtn, enlargeBtn, closeBtn);
	}
}
