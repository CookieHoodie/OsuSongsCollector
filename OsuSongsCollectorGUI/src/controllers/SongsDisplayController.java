package controllers;

import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.SqliteDatabase;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class SongsDisplayController {
	private SqliteDatabase songsDb;
	private Stage currentStage;
	private TableColumn<TableViewData, String> songTitleCol = new TableColumn<TableViewData, String>("Song Title");
	private TableColumn<TableViewData, String> artistNameCol = new TableColumn<TableViewData, String>("Artist");
	private TableColumn<TableViewData, String> songSourceCol = new TableColumn<TableViewData, String>("Source");
	private TableColumn<TableViewData, Integer> totalTimeCol = new TableColumn<TableViewData, Integer>("Approx length");
	
	@FXML private TextField testSearchText;
	@FXML private Button testSearchButton;
	@FXML private TableView<TableViewData> testTable;
	
	public void initData(Stage currentStage, SqliteDatabase connectedSongsDb) throws SQLException {
		this.songsDb = connectedSongsDb;
		this.currentStage = currentStage;
		currentStage.setOnCloseRequest(e -> {
			try {
				this.songsDb.closeConnection();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		this.initTableView();
	}
	
	@FXML private void searchDb(ActionEvent event) throws FileNotFoundException, SQLException {
		String text = this.testSearchText.getText();
		
			
			String[] items = {this.songsDb.Data.Song.SONG_TITLE, this.songsDb.Data.Artist.ARTIST_NAME};
			String[] searchedStrings = text.split("\\s+");
			String[] orderBy = {this.songsDb.Data.Beatmap.LAST_MODIFICATION_TIME};
			ResultSet rs = this.songsDb.searchAll(items, searchedStrings, orderBy);
			while (rs.next()) {
				System.out.println(rs.getString(this.songsDb.Data.Song.SONG_TITLE) + "\t" + rs.getString(this.songsDb.Data.Artist.ARTIST_NAME));
			}
			System.out.println("--------------------------------------------------------------------------------");
//			this.songsDb.testSearch();
	}
	
	private void initTableView() throws SQLException {
        String[] items = {this.songsDb.Data.Song.SONG_SOURCE, this.songsDb.Data.Song.SONG_TITLE
        		, this.songsDb.Data.Song.SONG_TITLE_UNICODE, this.songsDb.Data.Artist.ARTIST_NAME
        		, this.songsDb.Data.Artist.ARTIST_NAME_UNICODE, this.songsDb.Data.Beatmap.TOTAL_TIME};
        ResultSet rs = this.songsDb.getTableInitData(items);
        ObservableList<TableViewData> data = FXCollections.observableArrayList();
        while (rs.next()) {
        	data.add(new TableViewData(
        			rs.getString(items[0])
        			, rs.getString(items[1])
        			, rs.getString(items[2])
        			, rs.getString(items[3])
        			, rs.getString(items[4])
        			, rs.getInt(items[5])
        			));
        }
        this.songTitleCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("songTitle"));
        this.artistNameCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("artistName"));
        this.songSourceCol.setCellValueFactory(new PropertyValueFactory<TableViewData, String>("songSource"));
        this.totalTimeCol.setCellValueFactory(new PropertyValueFactory<TableViewData, Integer>("totalTime"));
        this.testTable.setItems(data);
        this.testTable.getColumns().addAll(this.songTitleCol, this.artistNameCol, this.songSourceCol, this.totalTimeCol);
	}
	
	public static class TableViewData {
		private final SimpleStringProperty songSource;
		private final SimpleStringProperty songTitle;
		private final SimpleStringProperty songTitleUnicode;
		private final SimpleStringProperty artistName;
		private final SimpleStringProperty artistNameUnicode;
		private final SimpleIntegerProperty totalTime;
		
		private TableViewData(String songSource, String songTitle, String songTitleUnicode, String artistName, String artistNameUnicode, int totalTime) {
			this.songSource = new SimpleStringProperty(songSource);
			this.songTitle = new SimpleStringProperty(songTitle);
			this.songTitleUnicode = new SimpleStringProperty(songTitleUnicode);
			this.artistName = new SimpleStringProperty(artistName);
			this.artistNameUnicode = new SimpleStringProperty(artistNameUnicode);
			this.totalTime = new SimpleIntegerProperty(totalTime);
		}

		public String getSongSource() {
			return songSource.get();
		}


		public String getSongTitle() {
			return songTitle.get();
		}


		public String getSongTitleUnicode() {
			return songTitleUnicode.get();
		}


		public String getArtistName() {
			return artistName.get();
		}


		public String getArtistNameUnicode() {
			return artistNameUnicode.get();
		}
		
		public Integer getTotalTime() {
			return totalTime.get();
		}
	}
}
