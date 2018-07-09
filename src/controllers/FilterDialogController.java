package controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import controllers.SongsDisplayController.TableViewData;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.collections.FXCollections;

public class FilterDialogController {
	private Map<String, List<TableViewData>> selectedSongsMap;
	private ObservableList<SimplifiedTableViewData> possibleDuplicatedObsList;
	
	@FXML private Button okButton;
	@FXML private Button cancelButton;
	@FXML private TableView<SimplifiedTableViewData> displayTable;
	@FXML private TableColumn<SimplifiedTableViewData, String> nameCol;
	@FXML private TableColumn<SimplifiedTableViewData, String> totalTimeCol;
	@FXML private TableColumn<SimplifiedTableViewData, Boolean> checkBoxCol;
	
	@FXML private void initialize() {
        this.nameCol.setCellValueFactory(new PropertyValueFactory<SimplifiedTableViewData, String>("name"));
        this.totalTimeCol.setCellValueFactory(new PropertyValueFactory<SimplifiedTableViewData, String>("totalTime"));
        Label totalTimeLabel = new Label("Length �");
        totalTimeLabel.setTooltip(new Tooltip("This length corresponds to the length of beatmap in osu!. Real mp3 length may be longer than this."));
        this.totalTimeCol.setGraphic(totalTimeLabel);
        this.checkBoxCol.setCellValueFactory(new PropertyValueFactory<SimplifiedTableViewData, Boolean>("isSelected"));
        this.checkBoxCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        // TODO: rectify this to make it efficient and pretty
        this.displayTable.setRowFactory(value -> {
        	return new TableRow<SimplifiedTableViewData>() {
        		@Override protected void updateItem(SimplifiedTableViewData item, boolean empty) {
        			super.updateItem(item, empty);
    				if (item != null && item.folderNameProperty().get().isEmpty()) {
        				this.setDisable(true);
        			}
        			else {
        				this.setDisable(false);
        			}
        		}
        	};
        });
	}
	
	public void initData(Map<String, List<TableViewData>> selectedSongsMap, ObservableList<SimplifiedTableViewData> possibleDuplicatedObsList) {
		this.selectedSongsMap = selectedSongsMap;
		this.possibleDuplicatedObsList = possibleDuplicatedObsList;
		this.displayTable.setItems(possibleDuplicatedObsList);
		possibleDuplicatedObsList.addListener(new ListChangeListener<SimplifiedTableViewData>() {
		    @Override
		    public void onChanged(ListChangeListener.Change<? extends SimplifiedTableViewData> c) {
		        while (c.next()) {
		            if (c.wasUpdated()) {
		                System.out.println(possibleDuplicatedObsList.get(c.getFrom()).folderNameProperty().get() + " -> " + possibleDuplicatedObsList.get(c.getFrom()).isSelectedProperty().get());
		            }
		          }
		    }
		});
	}
	
	@FXML private void removeDuplicates(ActionEvent event) {
		for (SimplifiedTableViewData row : this.displayTable.getItems()) {
			if (!row.isSelectedProperty().get()) {
				// TODO: modify this if found better way to group data
				if (!row.folderNameProperty().get().isEmpty()) {
					this.selectedSongsMap.remove(row.folderNameProperty().get());
				}
				
			}
		}
		this.exit();
	}
	
	@FXML private void closeWindow(ActionEvent event) {
		this.exit();
	}
	
	private void exit() {
		this.displayTable.getScene().getWindow().hide();
	}
	
	public static class SimplifiedTableViewData {
		private final SimpleStringProperty name;
		private final SimpleStringProperty totalTime;
		private final SimpleBooleanProperty isSelected;
		private final SimpleStringProperty folderName;
		
		public SimplifiedTableViewData() {
			this.name = new SimpleStringProperty("");
			this.totalTime = new SimpleStringProperty("");
			this.isSelected = new SimpleBooleanProperty(false);
			this.folderName = new SimpleStringProperty("");
		}
		
		public SimplifiedTableViewData(String name, String totalTime, boolean isSelected, String folderName) {
			this.name = new SimpleStringProperty(name);
			this.totalTime = new SimpleStringProperty(totalTime);
			this.isSelected = new SimpleBooleanProperty(isSelected);
			this.folderName = new SimpleStringProperty(folderName);
		}
		
		public static Callback<SimplifiedTableViewData, Observable[]> extractor() {
		   return (p) -> new Observable[]{p.isSelectedProperty()};
		}

		public SimpleBooleanProperty isSelectedProperty() {
			return isSelected;
		}

//		public void setIsSelectedProperty(SimpleBooleanProperty isSelectedProperty) {
//			this.isSelected = isSelectedProperty;
//		}
		
		public SimpleStringProperty nameProperty() {
			return name;
		}

		public SimpleStringProperty totalTimeProperty() {
			return totalTime;
		}
		
		public SimpleStringProperty folderNameProperty() {
			return folderName;
		}
	}
}
