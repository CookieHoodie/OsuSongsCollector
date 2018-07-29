package controllers;

import java.util.List;
import java.util.Map;

import controllers.SongsDisplayController.TableViewData;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class FilterDialogController {
	private Map<String, List<TableViewData>> selectedSongsMap;
	
	@FXML private Button selectOneFromEachButton;
	@FXML private Button okButton;
	@FXML private Button cancelButton;
	@FXML private TableView<SimplifiedTableViewData> displayTable;
	@FXML private TableColumn<SimplifiedTableViewData, String> nameCol;
	@FXML private TableColumn<SimplifiedTableViewData, String> totalTimeCol;
	@FXML private TableColumn<SimplifiedTableViewData, Boolean> checkBoxCol;
	
	@FXML private void initialize() {
        this.nameCol.setCellValueFactory(new PropertyValueFactory<SimplifiedTableViewData, String>("name"));
        this.totalTimeCol.setCellValueFactory(new PropertyValueFactory<SimplifiedTableViewData, String>("totalTime"));
        this.checkBoxCol.setCellValueFactory(new PropertyValueFactory<SimplifiedTableViewData, Boolean>("isSelected"));
        this.checkBoxCol.setCellFactory(tc -> new CheckBoxTableCell<>());
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
		this.displayTable.setItems(possibleDuplicatedObsList);
	}
	
	@FXML private void removeDuplicates(ActionEvent event) {
		for (SimplifiedTableViewData row : this.displayTable.getItems()) {
			if (!row.isSelectedProperty().get()) {
				if (!row.folderNameProperty().get().isEmpty()) {
				    // unselect the obsList to reflect the change in tableView
                    // supposedly the 'this.selectedSongsMap.get(row.folderNameProperty().get())' is list of size 1
                    // loop through it just in case of any logic error
				    for (TableViewData oriRow : this.selectedSongsMap.get(row.folderNameProperty().get())) {
				        oriRow.isSelectedProperty().set(false);
                    }
					this.selectedSongsMap.remove(row.folderNameProperty().get());
				}
			}
		}
		this.exit();
	}
	
	
	@FXML private void selectOneFromEach(ActionEvent event) {
		boolean isNewGroup = true;
		for (SimplifiedTableViewData row : this.displayTable.getItems()) {
			if (!row.folderNameProperty().get().isEmpty()) {
				if (isNewGroup) {
					row.isSelectedProperty().set(true);
					isNewGroup = false;
				}
				else {
					row.isSelectedProperty().set(false);
				}
			}
			else {
				isNewGroup = true;
			}
		}
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
