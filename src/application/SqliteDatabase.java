package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.concurrent.Task;

public class SqliteDatabase {
	private final String SQLITE_PREFIX = "jdbc:sqlite:";
	private final String URL;
	public final String DB_NAME;
	public SongsDbData Data = new SongsDbData();
	
	private boolean isDbExist;
	private Connection conn;
	
	// for threading
	private BiConsumer<Integer, Integer> progressUpdate = null;
	
	public SqliteDatabase(String dbName) {
		this.DB_NAME = dbName;
		this.URL = this.SQLITE_PREFIX + this.DB_NAME;
		this.setConn(null);
		File db = new File(dbName);
	    if(db.exists()) {
	    	this.setDbExist(true);
	    }
	    else {
	    	this.setDbExist(false);
	    }
	}
	
	public void connect() throws SQLException, FileNotFoundException {
		if (!this.isDbExist()) {
			throw new FileNotFoundException("Database is not yet created");
		}
        this.setConn(DriverManager.getConnection(this.URL));
        // turn on foreign key constraint
        Statement stmt = this.getConn().createStatement();
        stmt.execute("PRAGMA foreign_keys = ON");
	}
	
	public void closeConnection() throws SQLException {
		this.conn.close();
	}
	
	public void setThreadData(BiConsumer<Integer, Integer> progressUpdate) {
		this.setProgressUpdate(progressUpdate);
	}
	
	
	public void createDatabase() throws SQLException {
		this.setConn(DriverManager.getConnection(this.URL));
        this.setDbExist(true);
	}
	
	public void createTables() throws SQLException {
		if (this.getConn() == null) {
			throw new SQLException("Connection is not yet established");
		}
		this.createTableMetadata();
		this.createTableConfig();
		this.createTableArtist();
		this.createTableSong();
		this.createTableBeatmapSet();
		this.createTableBeatmap();
		this.createTableSongTag();
		this.createTableBeatmapSet_SongTag();
		
		this.createIndexBeatmapSet();
		this.createIndexBeatmap();
	}
	
	private void createTableMetadata() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS "+ this.Data.Metadata.TABLE_NAME + " ("
				+ this.Data.Metadata.METADATA_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.Metadata.OSU_VERSION + " INTEGER,"
				+ this.Data.Metadata.FOLDER_COUNT + " INTEGER,"
				+ this.Data.Metadata.PLAYER_NAME + " TEXT,"
				+ this.Data.Metadata.NUMBER_OF_BEATMAPS + " INTEGER"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableConfig() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.Config.TABLE_NAME + " ("
				+ this.Data.Config.CONFIG_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.Config.PATH_TO_OSU_DB + " TEXT,"
				+ this.Data.Config.PATH_TO_SONGS_FOLDER + " TEXT,"
				+ this.Data.Config.SAVE_FOLDER + " TEXT,"
				+ this.Data.Config.IS_SONG_SOURCE_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_ARTIST_NAME_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_ARTIST_NAME_UNICODE_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_SONG_TITLE_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_SONG_TITLE_UNICODE_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_CREATOR_NAME_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_TOTAL_TIME_SHOWN + " BOOLEAN,"
				+ this.Data.Config.IS_IS_DOWNLOADED_SHOWN + " BOOLEAN,"
				+ this.Data.Config.ORDERING + " TEXT,"
				+ this.Data.Config.SOUND_VOLUME + " REAL,"
				+ this.Data.Config.IS_REPEAT_TOGGLED + " BOOLEAN,"
				+ this.Data.Config.IS_SHUFFLE_TOGGLED + " BOOLEAN"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableBeatmapSet() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.BeatmapSet.TABLE_NAME + " ("
				+ this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.BeatmapSet.BEATMAP_SET_ID + " INTEGER,"
				+ this.Data.BeatmapSet.ARTIST_ID + " INTEGER,"
				+ this.Data.BeatmapSet.SONG_ID + " INTEGER,"
				+ this.Data.BeatmapSet.CREATOR_NAME + " TEXT COLLATE NOCASE,"
				+ this.Data.BeatmapSet.FOLDER_NAME + " TEXT COLLATE NOCASE,"
				+ this.Data.BeatmapSet.AUDIO_NAME + " TEXT,"
				+ this.Data.BeatmapSet.IS_DOWNLOADED + " BOOLEAN,"
				+ this.Data.BeatmapSet.IS_HIDDEN + " BOOLEAN,"
				+ "UNIQUE (" + this.Data.BeatmapSet.FOLDER_NAME + "," + this.Data.BeatmapSet.AUDIO_NAME + "),"
				+ "FOREIGN KEY (" + this.Data.BeatmapSet.ARTIST_ID + ") REFERENCES " + this.Data.Artist.TABLE_NAME + "(" + this.Data.Artist.ARTIST_ID + ")" + " ON UPDATE CASCADE ON DELETE CASCADE,"
				+ "FOREIGN KEY (" + this.Data.BeatmapSet.SONG_ID + ") REFERENCES " + this.Data.Song.TABLE_NAME + "(" + this.Data.Song.SONG_ID + ")" + " ON UPDATE CASCADE ON DELETE CASCADE"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableArtist() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.Artist.TABLE_NAME + " ("
				+ this.Data.Artist.ARTIST_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.Artist.ARTIST_NAME + " TEXT COLLATE NOCASE,"
				+ this.Data.Artist.ARTIST_NAME_UNICODE + " TEXT COLLATE NOCASE,"
				+ "UNIQUE (" + this.Data.Artist.ARTIST_NAME + ", " + this.Data.Artist.ARTIST_NAME_UNICODE + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableSong() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.Song.TABLE_NAME + " ("
				+ this.Data.Song.SONG_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.Song.SONG_TITLE + " TEXT COLLATE NOCASE,"
				+ this.Data.Song.SONG_TITLE_UNICODE + " TEXT COLLATE NOCASE,"
				+ this.Data.Song.SONG_SOURCE + " TEXT,"
				+ "UNIQUE (" + this.Data.Song.SONG_TITLE + "," + this.Data.Song.SONG_TITLE_UNICODE + "," + this.Data.Song.SONG_SOURCE + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableSongTag() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.SongTag.TABLE_NAME + " ("
				+ this.Data.SongTag.SONG_TAG_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.SongTag.SONG_TAG_NAME + " TEXT COLLATE NOCASE,"
				+ "UNIQUE (" + this.Data.SongTag.SONG_TAG_NAME + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableBeatmap() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.Beatmap.TABLE_NAME + " ("
				+ this.Data.Beatmap.BEATMAP_AUTO_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ this.Data.Beatmap.BEATMAP_ID + " INTEGER,"
				+ this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " INTEGER,"
				+ this.Data.Beatmap.RANKED_STATUS + " INTEGER,"
				+ this.Data.Beatmap.LAST_MODIFICATION_TIME + " INTEGER,"
				+ this.Data.Beatmap.TOTAL_TIME + " INTEGER,"
				+ this.Data.Beatmap.PREVIEW_TIME + " INTEGER,"
				+ this.Data.Beatmap.THREAD_ID + " INTEGER,"
				+ this.Data.Beatmap.DIFFICULTY + " TEXT,"
//				+ this.Data.Beatmap.GRADE_STANDARD + " INTEGER,"
//				+ this.Data.Beatmap.GRADE_TAIKO + " INTEGER,"
//				+ this.Data.Beatmap.GRADE_CTB + " INTEGER,"
//				+ this.Data.Beatmap.GRADE_MANIA + " INTEGER,"
				+ this.Data.Beatmap.IS_UNPLAYED + " BOOLEAN,"
				+ this.Data.Beatmap.LAST_PLAYED_TIME + " INTEGER,"
				+ "FOREIGN KEY (" + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + ") REFERENCES " + this.Data.BeatmapSet.TABLE_NAME + "(" + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + ")" + " ON UPDATE CASCADE ON DELETE CASCADE"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableBeatmapSet_SongTag() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "("
				+ this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " INTEGER NOT NULL,"
				+ this.Data.BeatmapSet_SongTag.SONG_TAG_ID + " INTEGER NOT NULL,"
				+ "PRIMARY KEY (" + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + "," + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	
	private void createIndexBeatmapSet() throws SQLException {
		Statement stmt = this.getConn().createStatement();
		String sql = "CREATE INDEX IF NOT EXISTS idx_artist_id ON " + this.Data.BeatmapSet.TABLE_NAME + "(" + this.Data.BeatmapSet.ARTIST_ID + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_song_id ON " + this.Data.BeatmapSet.TABLE_NAME + "(" + this.Data.BeatmapSet.SONG_ID + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_creator_name ON " + this.Data.BeatmapSet.TABLE_NAME + "(" + this.Data.BeatmapSet.CREATOR_NAME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_folder_name ON " + this.Data.BeatmapSet.TABLE_NAME + "(" + this.Data.BeatmapSet.FOLDER_NAME + ");";
		stmt.execute(sql);
	}
	
	private void createIndexBeatmap() throws SQLException {
		Statement stmt = this.getConn().createStatement();
		String sql = "CREATE INDEX IF NOT EXISTS idx_beatmap_set_auto_id ON " + this.Data.Beatmap.TABLE_NAME + "(" + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_last_modification_time ON " + this.Data.Beatmap.TABLE_NAME + "(" + this.Data.Beatmap.LAST_MODIFICATION_TIME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_total_time ON " + this.Data.Beatmap.TABLE_NAME + "(" + this.Data.Beatmap.TOTAL_TIME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_preview_time ON " + this.Data.Beatmap.TABLE_NAME + "(" + this.Data.Beatmap.PREVIEW_TIME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_difficulty ON " + this.Data.Beatmap.TABLE_NAME + "(" + this.Data.Beatmap.DIFFICULTY + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_last_played_time ON " + this.Data.Beatmap.TABLE_NAME + "(" + this.Data.Beatmap.LAST_PLAYED_TIME + ");";
		stmt.execute(sql);
	}
	
	
	private void insertIntoMetadata(int osuVersion, int folderCount, String playerName, int numberOfBeatmaps) throws SQLException {
		String sql = "INSERT INTO " + this.Data.Metadata.TABLE_NAME + "(" 
				+ this.Data.Metadata.OSU_VERSION + "," 
				+ this.Data.Metadata.FOLDER_COUNT + "," 
				+ this.Data.Metadata.PLAYER_NAME + "," 
				+ this.Data.Metadata.NUMBER_OF_BEATMAPS
				+ ") VALUES(?,?,?,?)";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setInt(1, osuVersion);
		pstmt.setInt(2, folderCount);
		pstmt.setString(3, playerName);
		pstmt.setInt(4, numberOfBeatmaps);
		pstmt.executeUpdate();
	}
	
	private void insertIntoConfig(String pathToOsuDb, String pathToSongsFolder, String saveFolder,
			boolean isSongSourceShown, boolean isArtistNameShown, boolean isArtistNameUnicodeShown,
			boolean isSongTitleShown, boolean isSongTitleUnicodeShown, boolean isCreatorNameShown,
			boolean isTotalTimeShown, boolean isIsDownloadedShown, String ordering, double soundVolume,
			boolean isRepeatToggled, boolean isShuffleToggled) throws SQLException {
		String sql = "INSERT INTO " + this.Data.Config.TABLE_NAME + "(" 
				+ this.Data.Config.PATH_TO_OSU_DB + ","
				+ this.Data.Config.PATH_TO_SONGS_FOLDER + ","
				+ this.Data.Config.SAVE_FOLDER + ","
				+ this.Data.Config.IS_SONG_SOURCE_SHOWN + ","
				+ this.Data.Config.IS_ARTIST_NAME_SHOWN + ","
				+ this.Data.Config.IS_ARTIST_NAME_UNICODE_SHOWN + ","
				+ this.Data.Config.IS_SONG_TITLE_SHOWN + ","
				+ this.Data.Config.IS_SONG_TITLE_UNICODE_SHOWN + ","
				+ this.Data.Config.IS_CREATOR_NAME_SHOWN + ","
				+ this.Data.Config.IS_TOTAL_TIME_SHOWN + ","
				+ this.Data.Config.IS_IS_DOWNLOADED_SHOWN + ","
				+ this.Data.Config.ORDERING + ","
				+ this.Data.Config.SOUND_VOLUME + ","
				+ this.Data.Config.IS_REPEAT_TOGGLED + ","
				+ this.Data.Config.IS_SHUFFLE_TOGGLED
				+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, pathToOsuDb);
		pstmt.setString(2, pathToSongsFolder);
		pstmt.setString(3, saveFolder);
		pstmt.setBoolean(4, isSongSourceShown);
		pstmt.setBoolean(5, isArtistNameShown);
		pstmt.setBoolean(6, isArtistNameUnicodeShown);
		pstmt.setBoolean(7, isSongTitleShown);
		pstmt.setBoolean(8, isSongTitleUnicodeShown);
		pstmt.setBoolean(9, isCreatorNameShown);
		pstmt.setBoolean(10, isTotalTimeShown);
		pstmt.setBoolean(11, isIsDownloadedShown);
		pstmt.setString(12, ordering);
		pstmt.setDouble(13, soundVolume);
		pstmt.setBoolean(14, isRepeatToggled);
		pstmt.setBoolean(15, isShuffleToggled);
		pstmt.executeUpdate();
	}
	
	private PreparedStatement getInsertIntoBeatmapSetPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + this.Data.BeatmapSet.TABLE_NAME + "("
				+ this.Data.BeatmapSet.BEATMAP_SET_ID + ","
				+ this.Data.BeatmapSet.ARTIST_ID + ","
				+ this.Data.BeatmapSet.SONG_ID + ","
				+ this.Data.BeatmapSet.CREATOR_NAME + ","
				+ this.Data.BeatmapSet.FOLDER_NAME + ","
				+ this.Data.BeatmapSet.AUDIO_NAME + ","
				+ this.Data.BeatmapSet.IS_DOWNLOADED + ","
				+ this.Data.BeatmapSet.IS_HIDDEN 
				+ ") VALUES(?,?,?,?,?,?,?,?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoBeatmapSetBatch(PreparedStatement beatmapSetPStatement, 
			int beatmapSetID, 
			int artistID,
			int songID,
			String creatorName,
			String folderName, 
			String audioName,
			boolean isDownloaded, 
			boolean isHidden
			) throws SQLException {
		beatmapSetPStatement.setInt(1, beatmapSetID);
		beatmapSetPStatement.setInt(2, artistID);
		beatmapSetPStatement.setInt(3, songID);
		beatmapSetPStatement.setString(4, creatorName);
		beatmapSetPStatement.setString(5, folderName);
		beatmapSetPStatement.setString(6, audioName);
		beatmapSetPStatement.setBoolean(7, isDownloaded);
		beatmapSetPStatement.setBoolean(8, isHidden);
		beatmapSetPStatement.addBatch();
	}
	
	
	private PreparedStatement getInsertIntoArtistPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + this.Data.Artist.TABLE_NAME + "(" 
				+ this.Data.Artist.ARTIST_NAME + ","
				+ this.Data.Artist.ARTIST_NAME_UNICODE
				+ ") VALUES(?,?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoArtistBatch(PreparedStatement artistPStatement, String artistName, String artistNameUnicode) throws SQLException {
		artistPStatement.setString(1, artistName);
		artistPStatement.setString(2, artistNameUnicode);
		artistPStatement.addBatch();
	}
	
	
	private PreparedStatement getInsertIntoSongPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + this.Data.Song.TABLE_NAME + "("
				+ this.Data.Song.SONG_TITLE + ","
				+ this.Data.Song.SONG_TITLE_UNICODE + ","
				+ this.Data.Song.SONG_SOURCE
				+ ") VALUES(?,?,?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoSongBatch(PreparedStatement songPStatement, String songTitle, String songTitleUnicode, String songSource) throws SQLException {
		songPStatement.setString(1, songTitle);
		songPStatement.setString(2, songTitleUnicode);
		songPStatement.setString(3, songSource);
		songPStatement.addBatch();
	}
	
	
	private PreparedStatement getInsertIntoSongTagPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + this.Data.SongTag.TABLE_NAME + "(" 
				+ this.Data.SongTag.SONG_TAG_NAME
				+ ") VALUES(?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoSongTagBatch(PreparedStatement songTagPStatement, String songTagName) throws SQLException {
		songTagPStatement.setString(1, songTagName);
		songTagPStatement.addBatch();
	}
	

			
	private PreparedStatement getInsertIntoBeatmapPStatement() throws SQLException {
		String sql = "INSERT INTO " + this.Data.Beatmap.TABLE_NAME + "("
				+ this.Data.Beatmap.BEATMAP_ID + ","
				+ this.Data.Beatmap.BEATMAP_SET_AUTO_ID + ","
				+ this.Data.Beatmap.RANKED_STATUS + ","
				+ this.Data.Beatmap.LAST_MODIFICATION_TIME + ","
				+ this.Data.Beatmap.TOTAL_TIME + ","
				+ this.Data.Beatmap.PREVIEW_TIME + ","
				+ this.Data.Beatmap.THREAD_ID + ","
				+ this.Data.Beatmap.DIFFICULTY + ","
//				+ this.Data.Beatmap.GRADE_STANDARD + ","
//				+ this.Data.Beatmap.GRADE_TAIKO + ","
//				+ this.Data.Beatmap.GRADE_CTB + ","
//				+ this.Data.Beatmap.GRADE_MANIA + ","
				+ this.Data.Beatmap.IS_UNPLAYED + ","
				+ this.Data.Beatmap.LAST_PLAYED_TIME
				+ ") VALUES(?,?,?,?,?,?,?,?,?,?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoBeatmapBatch(PreparedStatement beatmapPStatement, 
			int beatmapID, 
			int beatmapSetAutoID, 
			int rankedStatus, 
			long lastModificationTime, 
			int totalTime, 
			int previewTime, 
			int threadID, 
			String difficulty,
			boolean isUnplayed, 
			long lastPlayedTime
			) throws SQLException {
		beatmapPStatement.setInt(1, beatmapID);
		beatmapPStatement.setInt(2, beatmapSetAutoID);
		beatmapPStatement.setInt(3, rankedStatus);
		beatmapPStatement.setLong(4, lastModificationTime);
		beatmapPStatement.setInt(5, totalTime);
		beatmapPStatement.setInt(6, previewTime);
		beatmapPStatement.setInt(7, threadID);
		beatmapPStatement.setString(8, difficulty);
		beatmapPStatement.setBoolean(9, isUnplayed);
		beatmapPStatement.setLong(10, lastPlayedTime);
		beatmapPStatement.addBatch();
	}
	
	
	private PreparedStatement getInsertIntoBeatmapSet_SongTagPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "(" 
				+ this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + ","
				+ this.Data.BeatmapSet_SongTag.SONG_TAG_ID 
				+ ") VALUES(?,?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoBeatmapSet_SongTagBatch(PreparedStatement beatmapSet_SongTagPStatement, int beatmapSetID, int songTagID) throws SQLException {
		beatmapSet_SongTagPStatement.setInt(1, beatmapSetID);
		beatmapSet_SongTagPStatement.setInt(2, songTagID);
		beatmapSet_SongTagPStatement.addBatch();
	}
	
	
	private Set<String> collectDataForUnranked(Beatmap beatmapDataForReference, List<Beatmap> beatmapSet) {
		// !! this method changes values in beatmapDataForReference
		// beatmapDataForReference is the object to be modified, beatmapSet is where the data is collected from
		// create a songTagSet to accumulate the (likely) sparse songTags
		Set<String> songTagNameSet = new HashSet<String>(Arrays.asList(beatmapDataForReference.getSongTags().split("\\s+")));
		
		// then start looping from 2nd Beatmap to collect data
		for (int j = 1; j < beatmapSet.size(); j++) {
			Beatmap b = beatmapSet.get(j);
			// if the songTag does not equal to 1st songTag, add them to set
			// (this can reduce the number of times codes in this if statement is executed, but not too if only 1st element
			// is different)
			if (!b.getSongTags().equals(beatmapDataForReference.getSongTags())) {
				String[] songTagNames = b.getSongTags().split("\\s+");
				for (String songTagName : songTagNames) {
					songTagNameSet.add(songTagName);
				}
			}
			
			// artistNameUnicode
			// if current artistNameUnicode is not empty and the reference is empty, set reference to current
			// if current artistNameUnicode is not empty and current is unicode, set reference to current unicode
			if (!b.getArtistNameUnicode().isEmpty() && (beatmapDataForReference.getArtistNameUnicode().isEmpty() || !b.getArtistNameUnicode().matches("\\A\\p{ASCII}*\\z"))) {
				beatmapDataForReference.setArtistNameUnicode(b.getArtistNameUnicode());
			}
			
			// songTitleUnicode
			// same as above
			if (!b.getSongTitleUnicode().isEmpty() && (beatmapDataForReference.getSongTitleUnicode().isEmpty() || !b.getSongTitleUnicode().matches("\\A\\p{ASCII}*\\z"))) {
				beatmapDataForReference.setSongTitleUnicode(b.getSongTitleUnicode());
			}
			
			// songSource
			if (!b.getSongSource().isEmpty() && beatmapDataForReference.getSongSource().isEmpty()) {
				beatmapDataForReference.setSongSource(b.getSongSource());
			}
		}
		// reconcate the collected songTags back to string for reference later 
		beatmapDataForReference.setSongTags(String.join(" ", songTagNameSet));
		return songTagNameSet;
	}
	
	// for threading
	private void insertDataIntoDb(List<List<Beatmap>> dataToInsert, List<Integer> rankedList, Map<Integer, Beatmap> unrankedDataMap
			, Map<Integer, List<List<Beatmap>>> atomizedBeatmapSetMap, Map<Integer, List<Beatmap>> atomizedBeatmapSetReferenceDataMap
			, boolean deleteSongsDb) throws SQLException, InterruptedException {
		// rankedList: store the rankedStatus of each of the corresponding beatmap in dataToInsert with the same order 
		// -1 for unranked, -2 for multi-audio, rankedIndex for ranked
		// unrankedDataMap: key is the index in dataToInsert list, value is the reference beatmap with refined attributes for inserting data
		// atomizedBeatmapSetMap: for multi-audio situation. key is the index like above, value is the atomizedBeatmapSets
		// atomizedBeatmapSetReferenceDataMap: each element corresponds to atomizedBeatmapSets in the above map, in order
		// * UI progress is updated here
		
		int batchSize = 300; // actually much bigger than this
		// get all the preparedStatements 1st for batch insert
		PreparedStatement artistPStatement = this.getInsertIntoArtistPStatement();
		PreparedStatement songPStatement = this.getInsertIntoSongPStatement();
		PreparedStatement songTagPStatement = this.getInsertIntoSongTagPStatement();
		
		// for progressBar in UI
		int totalProgress = dataToInsert.size() * 4;
		int currentProgress = 0;
		
		for (int i = 0; i < dataToInsert.size(); i++) {
			List<Beatmap> beatmapSet = dataToInsert.get(i);
			if (Thread.currentThread().isInterrupted()) {
				artistPStatement.cancel();
				songPStatement.cancel();
				songTagPStatement.cancel();
				this.cleanUpThread(deleteSongsDb);
				throw new InterruptedException("CreateDatabaseTask is interrupted");
			}
			
			int rankedIndex = rankedList.get(i);
			boolean isRanked = rankedIndex < 0 ? false : true;
			
			if (isRanked) {
				String[] songTagNames = beatmapSet.get(rankedIndex).getSongTags().split("\\s+");
				
				// actual storing of data to non-foreign tables
				for (String songTagName : songTagNames) {
					this.insertIntoSongTagBatch(songTagPStatement, songTagName);
				}
				this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(rankedIndex).getArtistName(), beatmapSet.get(rankedIndex).getArtistNameUnicode());
				this.insertIntoSongBatch(songPStatement, beatmapSet.get(rankedIndex).getSongTitle(), beatmapSet.get(rankedIndex).getSongTitleUnicode(), beatmapSet.get(rankedIndex).getSongSource());
			}
			
			else {
				boolean isAtomized = rankedIndex == -2 ? true : false;
				// 99% of the time
				if (!isAtomized) {
					Beatmap beatmapDataForReference = unrankedDataMap.get(i);
					String[] songTagNames = beatmapDataForReference.getSongTags().split("\\s+");
					for (String songTagName : songTagNames) {
						this.insertIntoSongTagBatch(songTagPStatement, songTagName);
					}
					
					this.insertIntoArtistBatch(artistPStatement, beatmapDataForReference.getArtistName(), beatmapDataForReference.getArtistNameUnicode());
					this.insertIntoSongBatch(songPStatement, beatmapDataForReference.getSongTitle(), beatmapDataForReference.getSongTitleUnicode(), beatmapDataForReference.getSongSource());
					
				}
				else {
					List<List<Beatmap>> atomizedBeatmapSets = atomizedBeatmapSetMap.get(i);
					List<Beatmap> beatmapSetReferenceDatas = atomizedBeatmapSetReferenceDataMap.get(i);
					for (int j = 0; j < atomizedBeatmapSets.size(); j++) {
						// get important data from corresponding stored reference
						Beatmap beatmapDataForReference = beatmapSetReferenceDatas.get(j);
						String[] songTagNames = beatmapDataForReference.getSongTags().split("\\s+");
						for (String songTagName : songTagNames) {
							this.insertIntoSongTagBatch(songTagPStatement, songTagName);
						}
						
						this.insertIntoArtistBatch(artistPStatement, beatmapDataForReference.getArtistName(), beatmapDataForReference.getArtistNameUnicode());
						this.insertIntoSongBatch(songPStatement, beatmapDataForReference.getSongTitle(), beatmapDataForReference.getSongTitleUnicode(), beatmapDataForReference.getSongSource());
					
					}
				}
			}
			
			// start to insert when batch size is considerable
			if ((i + 1) % batchSize == 0 || (i + 1) == dataToInsert.size()) {
				songTagPStatement.executeBatch(); // much more than batch size but not gonna be a problem
				artistPStatement.executeBatch();
				songPStatement.executeBatch();
				this.getConn().commit();
			}
			
			if (this.progressUpdate != null) {
				currentProgress++;
				progressUpdate.accept(currentProgress, totalProgress);
			}
		}
	
		
		// now insert beatmapSet which requires data from tables inserted above
		PreparedStatement beatmapSetPStatement = this.getInsertIntoBeatmapSetPStatement();
		for (int i = 0; i < dataToInsert.size(); i++) {
			List<Beatmap> beatmapSet = dataToInsert.get(i);
			if (Thread.currentThread().isInterrupted()) {
				beatmapSetPStatement.cancel();
				this.cleanUpThread(deleteSongsDb);
				throw new InterruptedException("CreateDatabaseTask is interrupted");
			}
			
			int rankedIndex = rankedList.get(i);
			boolean isRanked = rankedIndex < 0 ? false : true;
			if (isRanked) {
				Beatmap beatmap = beatmapSet.get(rankedIndex);
				ResultSet artistIDRs = this.selectArtistIDFromArtist(beatmap.getArtistName(), beatmap.getArtistNameUnicode());
				ResultSet songIDRs = this.selectSongIDFromSong(beatmap.getSongTitle(), beatmap.getSongTitleUnicode(), beatmap.getSongSource());
				int artistID;
				int songID;
				if (artistIDRs.next() && songIDRs.next()) {
					artistID = artistIDRs.getInt(1);
					songID = songIDRs.getInt(1);
				}
				else {
					throw new SQLException("Failed to retrieve newly inserted data");
				}
				this.insertIntoBeatmapSetBatch(beatmapSetPStatement, beatmap.getBeatmapSetID(), artistID, songID, beatmap.getCreatorName(), beatmap.getFolderName(), beatmap.getAudioFileName(), false, false);
				
			}
			else {
				// further check whether it is exception case
				boolean isAtomized = rankedIndex == -2 ? true : false;
				if (!isAtomized) {
					// get id, creatorName, etc. (those not so important data) from 1st element of currentBeatmapSet
					// while getting the important stored data from map
					Beatmap beatmapForOtherData = beatmapSet.get(0);
					Beatmap beatmapDataForReference = unrankedDataMap.get(i);
					ResultSet artistIDRs = this.selectArtistIDFromArtist(beatmapDataForReference.getArtistName(), beatmapDataForReference.getArtistNameUnicode());
					ResultSet songIDRs = this.selectSongIDFromSong(beatmapDataForReference.getSongTitle(), beatmapDataForReference.getSongTitleUnicode(), beatmapDataForReference.getSongSource());
					int artistID;
					int songID;
					if (artistIDRs.next() && songIDRs.next()) {
						artistID = artistIDRs.getInt(1);
						songID = songIDRs.getInt(1);
					}
					else {
						throw new SQLException("Failed to retrieve newly inserted data");
					}
					this.insertIntoBeatmapSetBatch(beatmapSetPStatement, beatmapForOtherData.getBeatmapSetID(), artistID, songID, beatmapForOtherData.getCreatorName(), beatmapForOtherData.getFolderName(), beatmapForOtherData.getAudioFileName(), false, false);
				}
				else {
					// almost same here just that retrive data from different map and loop through the atomizedBeatmapSets
					List<List<Beatmap>> atomizedBeatmapSets = atomizedBeatmapSetMap.get(i);
					List<Beatmap> beatmapSetReferenceDatas = atomizedBeatmapSetReferenceDataMap.get(i);
					// for each of the atomizedBeatmapSets,
					for (int j = 0; j < atomizedBeatmapSets.size(); j++) {
						List<Beatmap> atomizedBeatmapSet = atomizedBeatmapSets.get(j);
						// get unimportant data from 1st element of each atomizedBeatmapSets
						Beatmap beatmapForOtherData = atomizedBeatmapSet.get(0);
						// and important data from corresponding stored reference
						Beatmap beatmapDataForReference = beatmapSetReferenceDatas.get(j);
						ResultSet artistIDRs = this.selectArtistIDFromArtist(beatmapDataForReference.getArtistName(), beatmapDataForReference.getArtistNameUnicode());
						ResultSet songIDRs = this.selectSongIDFromSong(beatmapDataForReference.getSongTitle(), beatmapDataForReference.getSongTitleUnicode(), beatmapDataForReference.getSongSource());
						int artistID;
						int songID;
						if (artistIDRs.next() && songIDRs.next()) {
							artistID = artistIDRs.getInt(1);
							songID = songIDRs.getInt(1);
						}
						else {
							throw new SQLException("Failed to retrieve newly inserted data");
						}
						this.insertIntoBeatmapSetBatch(beatmapSetPStatement, beatmapForOtherData.getBeatmapSetID(), artistID, songID, beatmapForOtherData.getCreatorName(), beatmapForOtherData.getFolderName(), beatmapForOtherData.getAudioFileName(), false, false);
					}
				}
			}
			
			if ((i + 1) % batchSize == 0 || (i + 1) == dataToInsert.size()) {
				beatmapSetPStatement.executeBatch();
				this.getConn().commit();
			}
			
			if (this.progressUpdate != null) {
				currentProgress++;
				progressUpdate.accept(currentProgress, totalProgress);
			}
		}
		
		// lastly, insert the normalized table and beatmap table which depends on beatmapSet table just inserted
		PreparedStatement beatmapPStatement = this.getInsertIntoBeatmapPStatement();
		PreparedStatement beatmapSet_SongTagPStatement = this.getInsertIntoBeatmapSet_SongTagPStatement();
		for (int i = 0; i < dataToInsert.size(); i++) {
			List<Beatmap> beatmapSet = dataToInsert.get(i);
			if (Thread.currentThread().isInterrupted()) {
				beatmapPStatement.cancel();
				beatmapSet_SongTagPStatement.cancel();
				this.cleanUpThread(deleteSongsDb);
				throw new InterruptedException("CreateDatabaseTask is interrupted");
			}
			
			int rankedIndex = rankedList.get(i);
			boolean isRanked = rankedIndex < 0 ? false : true;
			if (isRanked) {
				ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmapSet.get(rankedIndex).getFolderName(), beatmapSet.get(rankedIndex).getAudioFileName());
				int beatmapSetAutoID;
				if (beatmapSetAutoIDRs.next()) {
					beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
				}
				else {
					throw new SQLException("Failed to retrieve newly inserted data");
				}
				String[] songTagNames = beatmapSet.get(rankedIndex).getSongTags().split("\\s+");
				ResultSet rs = this.selectSongTagIDFromSongTag(songTagNames);
				while (rs.next()) {
					this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
				}
				
				for (Beatmap beatmap : beatmapSet) {
					this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
				}
			}
			else {
				boolean isAtomized = rankedIndex == -1 ? true : false;
				if (isAtomized) {
					ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmapSet.get(0).getFolderName(), beatmapSet.get(0).getAudioFileName());
					int beatmapSetAutoID;
					if (beatmapSetAutoIDRs.next()) {
						beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
					}
					else {
						throw new SQLException("Failed to retrieve newly inserted data");
					}
					Beatmap beatmapDataForReference = unrankedDataMap.get(i);
					ResultSet rs = this.selectSongTagIDFromSongTag(beatmapDataForReference.getSongTags().split("\\s+"));
					while (rs.next()) {
						this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
					}
					for (Beatmap beatmap : beatmapSet) {
						this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
					}
				}
				else {
					List<List<Beatmap>> atomizedBeatmapSets = atomizedBeatmapSetMap.get(i);
					List<Beatmap> beatmapDataForReferences = atomizedBeatmapSetReferenceDataMap.get(i);
					for (int j = 0; j < atomizedBeatmapSets.size(); j++) {
						List<Beatmap> atomizedBeatmapSet = atomizedBeatmapSets.get(j);
						Beatmap beatmapDataForReference = beatmapDataForReferences.get(j);
						
						ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(atomizedBeatmapSet.get(0).getFolderName(), atomizedBeatmapSet.get(0).getAudioFileName());
						int beatmapSetAutoID;
						if (beatmapSetAutoIDRs.next()) {
							beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
						}
						else {
							throw new SQLException("Failed to retrieve newly inserted data");
						}
						
						ResultSet rs = this.selectSongTagIDFromSongTag(beatmapDataForReference.getSongTags().split("\\s+"));
						while (rs.next()) {
							this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
						}
						for (Beatmap beatmap : atomizedBeatmapSet) {
							this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
						}
					}
				}
			}
			
			if ((i + 1) % batchSize == 0 || (i + 1) == dataToInsert.size()) {
				beatmapPStatement.executeBatch();
				beatmapSet_SongTagPStatement.executeBatch();
				this.getConn().commit();
			}
			
			if (this.progressUpdate != null) {
				currentProgress += 2;
				progressUpdate.accept(currentProgress, totalProgress);
			}
		}
		// change back autoCommit
//		this.getConn().setAutoCommit(true);
		
	}

	
	public void insertAllData(OsuDbParser osuDb) throws SQLException, InterruptedException {
		this.insertIntoMetadata(osuDb.getOsuVersion(), osuDb.getFolderCount(), osuDb.getPlayerName(), osuDb.getNumberOfBeatmaps());
		this.insertIntoConfig(osuDb.getPathToOsuDb(), osuDb.getPathToSongsFolder(), "", false, false, false, false, false, false, false, false, "", 50.0, true, false);
		
		// store rankedIndex if ranked, -1 if is not and -2 if multi-audio
		List<Integer> rankedList = new ArrayList<Integer>();
		// for unranked only 
		// key is the index of the beamtapSet in songsFolder, value is the atomizedBeatmapSets (for very rare situation)
		Map<Integer, List<List<Beatmap>>> atomizedBeatmapSetMap = new HashMap<>();
		// key is the index of the beatmapSet in songsFolder, value is the beamtapReferenceData
		// for each corresponding atomizedBeatmapSet in the same order
		Map<Integer, List<Beatmap>> atomizedBeatmapSetReferenceDataMap = new HashMap<>();
		// key is the index, value is Beatmap which contains artistNameUnicode, songTitile, etc. 
		// that should be used to insert or select from database
		// *if the unranked map is not atomized, it's not stored here but only the above two maps
		Map<Integer, Beatmap> unrankedDataMap = new HashMap<>();
		List<List<Beatmap>> songsFolder = osuDb.getSongsFolder();
		
		try {
			for (int i = 0; i < songsFolder.size(); i++) {
				List<Beatmap> beatmapSet = songsFolder.get(i);
				boolean isRanked = false;
				int rankedIndex = 0;
				// search through the each beatmap in each beatmapSet and see if they are ranked
				// if yes, it's pretty safe to assume all metadata (artistName etc.) are the same
				// so can go out of loop and directly assign the data to corresponding beatmapSet
				
				// set audioName to 1st of the beatmapSet for unranked Comparison
				String audioName = beatmapSet.get(0).getAudioFileName();
				// create new instance of list everytime to store atomizedBeatmapSet into map
				List<List<Beatmap>> atomizedBeatmapSets = new ArrayList<>();
				int subListFromIndex = 0;
				
				for (int j = 0; j < beatmapSet.size(); j++) {
					if (beatmapSet.get(j).getRankedStatus() == 4) {
						isRanked = true;
						// store the index of the beatmap that is ranked. Most of the time this will be 0 but not in some case, so better 
						// use this as indication when assigning data to beatmapSet
						// !! some ranked maps still have beatmapSetID of -1... no fking idea why... but so far so good so just leave it
						rankedIndex = j;
						break;
					}
					
					// only for unranked situation (very rare)
					if (!audioName.equals(beatmapSet.get(j).getAudioFileName())) {
						// update audioName for new reference
						audioName = beatmapSet.get(j).getAudioFileName();
						// add the grouped beatmapSet into list
						atomizedBeatmapSets.add(beatmapSet.subList(subListFromIndex, j));
						subListFromIndex = j;
					}
					// account for last audio in beatmapSet
					if (j == beatmapSet.size() - 1 && !atomizedBeatmapSets.isEmpty() && !audioName.equals(atomizedBeatmapSets.get(atomizedBeatmapSets.size() - 1).get(0).getAudioFileName())) {
						atomizedBeatmapSets.add(beatmapSet.subList(subListFromIndex, j + 1));
					}
				}
				
				if (isRanked) {
					rankedList.add(rankedIndex);
				}
				else {
					// 99% of the time
					if (atomizedBeatmapSets.isEmpty()) {
						// same thing as above, but indicates as unranked
						rankedList.add(-1);
						
						// initialize the beatmapData to 1st Beatmap of beatmapSet
						Beatmap beatmapDataForReference = new Beatmap();
						beatmapDataForReference.setArtistName(beatmapSet.get(0).getArtistName());
						beatmapDataForReference.setArtistNameUnicode(beatmapSet.get(0).getArtistNameUnicode());
						beatmapDataForReference.setSongTitle(beatmapSet.get(0).getSongTitle());
						beatmapDataForReference.setSongTitleUnicode(beatmapSet.get(0).getSongTitleUnicode());
						beatmapDataForReference.setSongSource(beatmapSet.get(0).getSongSource());
						beatmapDataForReference.setSongTags(beatmapSet.get(0).getSongTags());
						
						this.collectDataForUnranked(beatmapDataForReference, beatmapSet);
						
						// puting data into map for reference later
						unrankedDataMap.put(i, beatmapDataForReference);
					}
					else {
						// indicate as not atomized
						rankedList.add(-2);
						List<Beatmap> beatmapDataForReferences = new ArrayList<Beatmap>();
						
						// for each of the atomizedBeatmapSets, do the same thing as above
						for (List<Beatmap> atomizedBeatmapSet : atomizedBeatmapSets) {
							// initialize the beatmapData to 1st Beatmap of atomizedBeatmapSet
							Beatmap beatmapDataForReference = new Beatmap();
							beatmapDataForReference.setArtistName(atomizedBeatmapSet.get(0).getArtistName());
							beatmapDataForReference.setArtistNameUnicode(atomizedBeatmapSet.get(0).getArtistNameUnicode());
							beatmapDataForReference.setSongTitle(atomizedBeatmapSet.get(0).getSongTitle());
							beatmapDataForReference.setSongTitleUnicode(atomizedBeatmapSet.get(0).getSongTitleUnicode());
							beatmapDataForReference.setSongSource(atomizedBeatmapSet.get(0).getSongSource());
							beatmapDataForReference.setSongTags(atomizedBeatmapSet.get(0).getSongTags());
							
							this.collectDataForUnranked(beatmapDataForReference, atomizedBeatmapSet);
							
							beatmapDataForReferences.add(beatmapDataForReference);
						}
						// store for reference later
						atomizedBeatmapSetMap.put(i, atomizedBeatmapSets);
						atomizedBeatmapSetReferenceDataMap.put(i, beatmapDataForReferences);
					}
				}
			}
			
			if (songsFolder.size() != rankedList.size() || atomizedBeatmapSetMap.size() != atomizedBeatmapSetReferenceDataMap.size()) {
				throw new RuntimeException("Logic error when gathering information for inserting data");
			}
			
			try {
				this.getConn().setAutoCommit(false);
				this.insertDataIntoDb(songsFolder, rankedList, unrankedDataMap, atomizedBeatmapSetMap, atomizedBeatmapSetReferenceDataMap, true);
			}
			finally {
				this.getConn().setAutoCommit(true);
				// if threading, set back to null to prevent inadvertent access later
				if (this.progressUpdate != null) {
					this.setProgressUpdate(null);
				}
			}
		}
		catch (Exception e) {
			this.cleanUpThread(true);
			throw e;
		}
				
	}
	
	
	// return true if update took place, false otherwise
	public boolean updateData(OsuDbParser osuDb) throws SQLException, InterruptedException, Exception {
		// only the key is useful
		Map<Integer, Integer> dbRecords = new TreeMap<Integer, Integer>();
		// for progress bar
		int totalProgress = 4;
		boolean isAnyUpdated = false;
		
		String selectAllBeatmapSetAutoIDSql = "SELECT " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " FROM " + this.Data.BeatmapSet.TABLE_NAME;
		Statement allBeatmapSetAutoIDStatement = this.getConn().createStatement();
		
		System.out.println("Start getting all ID");
		
		ResultSet allBeatmapSetAutoIDRs = allBeatmapSetAutoIDStatement.executeQuery(selectAllBeatmapSetAutoIDSql);
		while (allBeatmapSetAutoIDRs.next()) {
			int beatmapSetAutoID = allBeatmapSetAutoIDRs.getInt(1);
			dbRecords.put(beatmapSetAutoID, 0);
		}
		
		
		System.out.println("Finish getting all ID");
		
		
		String selectBeatmapCountUsingBeatmapSetAutoIDSql = "SELECT COUNT(*) FROM " + this.Data.Beatmap.TABLE_NAME 
				+ " WHERE " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
		PreparedStatement beatmapCountUsingBeatmapSetAutoIDPStatement = this.getConn().prepareStatement(selectBeatmapCountUsingBeatmapSetAutoIDSql);
		
		
		// !! each corresponding list should be of same size
		List<List<Beatmap>> updateList = new ArrayList<List<Beatmap>>();
		// store the rankedIndex of each corresponding element in updateList. 
		List<Integer> updateRankedList = new ArrayList<Integer>();
		
		List<List<Beatmap>> modifiedList = new ArrayList<List<Beatmap>>();
		// 1st int stores the status: 1 means beatmaps are added, 0 means deleted
		// 2nd int stores the beatmapSetAutoID of that beatmapSet
		List<Integer[]> modifiedStatusAndIDList = new ArrayList<Integer[]>();
		
		
		// for unranked only 
		// key is the index of the beamtapSet in songsFolder, value is the atomizedBeatmapSets (for very rare situation)
		Map<Integer, List<List<Beatmap>>> atomizedBeatmapSetMap = new HashMap<>();
		// key is the index of the beatmapSet in songsFolder, value is the beamtapReferenceData
		// for each corresponding atomizedBeatmapSet in the same order
		Map<Integer, List<Beatmap>> atomizedBeatmapSetReferenceDataMap = new HashMap<>();
		// key is the index, value is Beatmap which contains artistNameUnicode, songTitile, etc. 
		// that should be used to insert or select from database
		// *if the unranked map is not atomized, it's not stored here but only the above two maps
		Map<Integer, Beatmap> unrankedDataMap = new HashMap<>();
		
		List<List<Beatmap>> songsFolder = osuDb.getSongsFolder();
		
		
		System.out.println("Start checking for updates");
		
		
		
		for (List<Beatmap> beatmapSet : songsFolder) {
			// for unranked only
			// initialize to first audioName
			String audioName = beatmapSet.get(0).getAudioFileName();
			int subListFromIndex = 0;
			
			boolean isRanked = false;
			int rankedIndex = 0;
			List<List<Beatmap>> atomizedBeatmapSets = new ArrayList<>();
			for (int j = 0; j < beatmapSet.size(); j++) {
				if (beatmapSet.get(j).getRankedStatus() == 4) {
					isRanked = true;
					rankedIndex = j;
					break;
				}
				
				// only for unranked situation (very rare)
				if (!audioName.equals(beatmapSet.get(j).getAudioFileName())) {
					audioName = beatmapSet.get(j).getAudioFileName();
					// add the grouped beatmapSet into list
					atomizedBeatmapSets.add(beatmapSet.subList(subListFromIndex, j));
					subListFromIndex = j;
				}
				// account for last audio in beatmapSet
				if (j == beatmapSet.size() - 1 && !atomizedBeatmapSets.isEmpty() && !audioName.equals(atomizedBeatmapSets.get(atomizedBeatmapSets.size() - 1).get(0).getAudioFileName())) {
					atomizedBeatmapSets.add(beatmapSet.subList(subListFromIndex, j + 1));
				}
			}
			// if ranked and the query returns records, most probably the beatmaps inside beatmapSet 
			// has not been changed, so select only count for the first time and select second time
			// if it is really changed
			// even if it's unranked, if there's no multiple audioFiles in the beatmapSet, it's safe to treat it as if it's ranked
			// so it's also put here (for checking update only)
			if (isRanked || atomizedBeatmapSets.isEmpty()) {
				ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmapSet.get(rankedIndex).getFolderName(), beatmapSet.get(rankedIndex).getAudioFileName());
				if (beatmapSetAutoIDRs.next()) {
					int beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
					if (dbRecords.remove(beatmapSetAutoID) == null) {
						throw new RuntimeException("Cannot find key in map");
					}
					beatmapCountUsingBeatmapSetAutoIDPStatement.setInt(1, beatmapSetAutoID);
					ResultSet countRs = beatmapCountUsingBeatmapSetAutoIDPStatement.executeQuery();
					if (countRs.next()) {
						int count = countRs.getInt(1);
						if (count == beatmapSet.size()) {
							continue;
						}
						else {
							modifiedList.add(beatmapSet);
							modifiedStatusAndIDList.add(new Integer[] {count < beatmapSet.size() ? 1 : 0, beatmapSetAutoID});
						}
					}
					else {
						throw new SQLException("BeatmapSet is found but with no beatmap");
					}
					
				}
				// record doesn't exist -- new beatmapSet
				else {
					updateList.add(beatmapSet);
					updateRankedList.add(isRanked ? rankedIndex : -1);
				}
			}
			else {
				// very rare case: not ranked and multi-audio
				// for inserting updatedData later
				// not necessary every atomizedBeatmapSet is updated, so use this to gather the updated ones
				List<List<Beatmap>> atomizedBeatmapSetsGatherer = new ArrayList<List<Beatmap>>();
				
				String folderName = beatmapSet.get(0).getFolderName();
				for (int j = 0; j < atomizedBeatmapSets.size(); j++) {
					String currentAudioName = atomizedBeatmapSets.get(j).get(0).getAudioFileName();
					ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(folderName, currentAudioName);
					if (beatmapSetAutoIDRs.next()) {
						int beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
						if (dbRecords.remove(beatmapSetAutoID) == null) {
							throw new RuntimeException("Cannot find key in map");
						}
						beatmapCountUsingBeatmapSetAutoIDPStatement.setInt(1, beatmapSetAutoID);
						ResultSet countRs = beatmapCountUsingBeatmapSetAutoIDPStatement.executeQuery();
						if (countRs.next()) {
							int count = countRs.getInt(1);
							if (count == atomizedBeatmapSets.get(j).size()) {
								continue;
							}
							else {
								modifiedList.add(atomizedBeatmapSets.get(j));
								modifiedStatusAndIDList.add(new Integer[] {count < atomizedBeatmapSets.get(j).size() ? 1 : 0, beatmapSetAutoID});
							}
						}
						else {
							throw new SQLException("BeatmapSet is found but with no beatmap");
						}
						
					}
					// new
					else {
						atomizedBeatmapSetsGatherer.add(atomizedBeatmapSets.get(j));
					}
				}
				if (!atomizedBeatmapSetsGatherer.isEmpty()) {
					updateList.add(beatmapSet);
					updateRankedList.add(-2);
					atomizedBeatmapSetMap.put(updateList.size() - 1, atomizedBeatmapSetsGatherer);
				}
			}
		}
		
		if (updateList.size() != updateRankedList.size() || modifiedList.size() != modifiedStatusAndIDList.size()) {
			throw new RuntimeException("Logic error in storing states of updateList and modifiedList");
		}
		
		if (this.progressUpdate != null) {
			this.progressUpdate.accept(1, totalProgress);
		}
		
		System.out.println("Finish");
		System.out.println("Update list: " + updateList.size());
		System.out.println("Modified list: " + modifiedList.size());
		System.out.println("Deleted: " + dbRecords.size());
		isAnyUpdated = updateList.isEmpty() && modifiedList.isEmpty() && dbRecords.isEmpty() ? false : true;
		if (Thread.currentThread().isInterrupted()) {
			// not closing songDb connection here as it might be at an instance where user is already in displaySongs stage
			throw new InterruptedException("Interrupted while updating data");
		}
		
		// even if all is empty, still go till the end to clean up certain resources
		// wrap in try to clean resources afterwards
		try {
			if (!dbRecords.isEmpty()) {
				System.out.println("Start deleting");
				// delete
				String deleteFromBeatmapSetSql = "DELETE FROM " + this.Data.BeatmapSet.TABLE_NAME + " WHERE "; 
				StringJoiner sj = new StringJoiner(" OR ");
				for (int i = 0; i < dbRecords.size(); i++) {
					sj.add(this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?");
				}
				deleteFromBeatmapSetSql += sj.toString();
				PreparedStatement deleteFromBeatmapSetPStatement = this.getConn().prepareStatement(deleteFromBeatmapSetSql);
				
				int statementIndex = 1;
				for (int beatmapSetAutoID : dbRecords.keySet()) {
					deleteFromBeatmapSetPStatement.setInt(statementIndex, beatmapSetAutoID);
					statementIndex++;
				}
				
				deleteFromBeatmapSetPStatement.executeUpdate();
			}
			
			if (this.progressUpdate != null) {
				this.progressUpdate.accept(2, totalProgress);
			}
			
			// start setting autoCommit here
			this.getConn().setAutoCommit(false);
			
			if (!modifiedList.isEmpty()) {
				
				System.out.println("Start modifying");
				
				// modified songs
				String getBeatmapAutoIDAndDifficultySql = "SELECT " + this.Data.Beatmap.BEATMAP_AUTO_ID 
						+ "," + this.Data.Beatmap.DIFFICULTY + " FROM "
						+ this.Data.Beatmap.TABLE_NAME + " WHERE " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
				PreparedStatement getBeatmapAutoIDAndDifficultyPStatement = this.getConn().prepareStatement(getBeatmapAutoIDAndDifficultySql);
				
				String deleteFromBeatmapSql = "DELETE FROM " + this.Data.Beatmap.TABLE_NAME + " WHERE "
						+ this.Data.Beatmap.BEATMAP_AUTO_ID + " = ?";
				PreparedStatement deleteFromBeatmapPStatement = this.getConn().prepareStatement(deleteFromBeatmapSql);
				
				PreparedStatement beatmapPStatement = this.getInsertIntoBeatmapPStatement();
				
				for (int i = 0; i < modifiedList.size(); i++) {
					boolean beatmapDeleted = modifiedStatusAndIDList.get(i)[0] == 0 ? true : false;
					int beatmapSetAutoID = modifiedStatusAndIDList.get(i)[1];
					List<Beatmap> beatmapSet = modifiedList.get(i);
					
					getBeatmapAutoIDAndDifficultyPStatement.setInt(1, beatmapSetAutoID);
					ResultSet beatmapAutoIDAndDifficultyRs = getBeatmapAutoIDAndDifficultyPStatement.executeQuery();
					
					Map<String, Beatmap> beatmapsMap = beatmapSet.stream().collect(Collectors.toMap(Beatmap::getDifficulty, Function.identity()));
					// songsDb has extra beatmaps
					if (beatmapDeleted) {
						while (beatmapAutoIDAndDifficultyRs.next()) {
//							int beatmapAutoID = beatmapAutoIDAndDifficultyRs.getInt(1);
							String difficulty = beatmapAutoIDAndDifficultyRs.getString(2);
//							boolean isObsoleteRecord = beatmapSet.stream().noneMatch(beatmap -> beatmap.getDifficulty().equals(difficulty));
//							if (isObsoleteRecord) {
//								deleteFromBeatmapPStatement.setInt(1, beatmapAutoID);
//								deleteFromBeatmapPStatement.executeUpdate();
//							}
							if (!beatmapsMap.containsKey(difficulty)) {
								// directly execute as modified list is not likely to be large
								int beatmapAutoID = beatmapAutoIDAndDifficultyRs.getInt(1);
								deleteFromBeatmapPStatement.setInt(1, beatmapAutoID);
								deleteFromBeatmapPStatement.executeUpdate();
							}
						}
					}
					// songsDb has less beatmaps
					else {
						// store the beatmaps with nameOfOsuFile as key in a map
//						Map<String, Beatmap> toBeAddedMap = beatmapSet.stream().collect(Collectors.toMap(Beatmap::getDifficulty, Function.identity()));
						
						// then foreach record in songsDb, remove the elements in the map
						while (beatmapAutoIDAndDifficultyRs.next()) {
							String difficulty = beatmapAutoIDAndDifficultyRs.getString(2);
							beatmapsMap.remove(difficulty);
						}
						// finally we get the map with beatmaps to be added
						for (Beatmap beatmap : beatmapsMap.values()) {
							this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
						}
						
					}
				}
				beatmapPStatement.executeBatch();
				this.getConn().commit();
			}
			
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Interrupted while updating data");
			}
			
			if (this.progressUpdate != null) {
				this.progressUpdate.accept(3, totalProgress);
			}
			
			
			if (!updateList.isEmpty()) {
				System.out.println("Start inserting");
				// insert new songs
				// gather needed info for updating
				// actually this can be done while checking for updates, but for the sake of
				// readability, it's done here. Size of updateList should be small so doesn't affect performance much.
				for (int i = 0; i < updateList.size(); i++) {
					
					List<Beatmap> beatmapSet = updateList.get(i);
					int rankedIndex = updateRankedList.get(i);
					boolean isRanked = rankedIndex < 0 ? false : true;
					if (!isRanked) {
						boolean isAtomized = rankedIndex == -2 ? true : false;
						// 99% of the time
						if (!isAtomized) {
							// initialize the beatmapData to 1st Beatmap of beatmapSet
							Beatmap beatmapDataForReference = new Beatmap();
							beatmapDataForReference.setArtistName(beatmapSet.get(0).getArtistName());
							beatmapDataForReference.setArtistNameUnicode(beatmapSet.get(0).getArtistNameUnicode());
							beatmapDataForReference.setSongTitle(beatmapSet.get(0).getSongTitle());
							beatmapDataForReference.setSongTitleUnicode(beatmapSet.get(0).getSongTitleUnicode());
							beatmapDataForReference.setSongSource(beatmapSet.get(0).getSongSource());
							beatmapDataForReference.setSongTags(beatmapSet.get(0).getSongTags());
							
							this.collectDataForUnranked(beatmapDataForReference, beatmapSet);
							
							// puting data into map for reference later
							unrankedDataMap.put(i, beatmapDataForReference);
						}
						else {
							List<List<Beatmap>> atomizedBeatmapSets = atomizedBeatmapSetMap.get(i);
							List<Beatmap> beatmapDataForReferences = new ArrayList<Beatmap>();
							
							// for each of the atomizedBeatmapSets, do the same thing as above
							for (List<Beatmap> atomizedBeatmapSet : atomizedBeatmapSets) {
								// initialize the beatmapData to 1st Beatmap of atomizedBeatmapSet
								Beatmap beatmapDataForReference = new Beatmap();
								beatmapDataForReference.setArtistName(atomizedBeatmapSet.get(0).getArtistName());
								beatmapDataForReference.setArtistNameUnicode(atomizedBeatmapSet.get(0).getArtistNameUnicode());
								beatmapDataForReference.setSongTitle(atomizedBeatmapSet.get(0).getSongTitle());
								beatmapDataForReference.setSongTitleUnicode(atomizedBeatmapSet.get(0).getSongTitleUnicode());
								beatmapDataForReference.setSongSource(atomizedBeatmapSet.get(0).getSongSource());
								beatmapDataForReference.setSongTags(atomizedBeatmapSet.get(0).getSongTags());
								
								this.collectDataForUnranked(beatmapDataForReference, atomizedBeatmapSet);
								
								beatmapDataForReferences.add(beatmapDataForReference);
							}
							// store for reference later
							atomizedBeatmapSetReferenceDataMap.put(i, beatmapDataForReferences);
						}
					}
				}
				
				if (this.progressUpdate != null) {
					this.progressUpdate.accept(4, totalProgress);
				}
				// by here, autoCommit is already set to false
				// update using info gathered
				this.insertDataIntoDb(updateList, updateRankedList, unrankedDataMap, atomizedBeatmapSetMap, atomizedBeatmapSetReferenceDataMap, false);
			}
		}
		finally {
			this.getConn().setAutoCommit(true);
			// if threading, set back to null to prevent inadvertent access later
			if (this.progressUpdate != null) {
				this.setProgressUpdate(null);
			}
		}
		
		// lastly update metadata here so that if disrupted, next start will still have same metadata and probably come here
		// to update again if no change was done to osuDb between this period
		ResultSet metadataRs = this.selectMetadata();
		if (metadataRs.next()) {
			int metadataID = metadataRs.getInt(this.Data.Metadata.METADATA_ID);
			this.updateMetadata(metadataID, osuDb.getOsuVersion(), osuDb.getFolderCount(), osuDb.getPlayerName(), osuDb.getNumberOfBeatmaps());
		}
		else {
			throw new SQLException("Metadata does not exist");
		}
		
		return isAnyUpdated;
	}
	
	
	public boolean updateDetails(Map<String, List<Beatmap>> osuDbBeatmapsMap) throws SQLException {
		Map<String, Map<String, Beatmap>> nestedMap = new HashMap<>();
		int totalProgress = 0;
		int currentProgress = 0;
		boolean isAnyUpdated = false;
		
		System.out.println("Creating maps");
		
		for (Map.Entry<String, List<Beatmap>> entry : osuDbBeatmapsMap.entrySet()) {
			String folderName = entry.getKey();
			List<Beatmap> beatmapSet = entry.getValue();
			Map<String, Beatmap> beatmapsMap = beatmapSet.stream().collect(Collectors.toMap(Beatmap::getDifficulty, Function.identity()));
			nestedMap.put(folderName, beatmapsMap);
			totalProgress += beatmapsMap.size();
		}
		
//		osuDbBeatmapsMap.forEach((folderName, beatmapSet) -> {
//			Map<String, Beatmap> beatmapsMap = beatmapSet.stream().collect(Collectors.toMap(Beatmap::getDifficulty, Function.identity()));
//			nestedMap.put(folderName, beatmapsMap);
//		});
		
		String selectBeatmapSql = "SELECT " + this.Data.Beatmap.BEATMAP_AUTO_ID + "," 
				+ this.Data.BeatmapSet.FOLDER_NAME + ","
				+ this.Data.Beatmap.LAST_MODIFICATION_TIME + ","
				+ this.Data.Beatmap.DIFFICULTY
				+ " FROM " + this.Data.Beatmap.TABLE_NAME
				+ " INNER JOIN " + this.Data.BeatmapSet.TABLE_NAME + " ON " + this.Data.BeatmapSet.TABLE_NAME + "." 
				+ this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " = " + this.Data.Beatmap.TABLE_NAME + "."
				+ this.Data.Beatmap.BEATMAP_SET_AUTO_ID;
		
		String[] items = {this.Data.Beatmap.LAST_MODIFICATION_TIME};
		PreparedStatement updateBeatmapPStatement = this.getUpdateBeatmapPStatement(items);
		Statement stmt = this.getConn().createStatement();
		
		System.out.println("updating");
		
		
		try {
			this.getConn().setAutoCommit(false);
			ResultSet beatmapRs = stmt.executeQuery(selectBeatmapSql);
			while (beatmapRs.next()) {
				String folderName = beatmapRs.getString(2);
				long lastModificationTime = beatmapRs.getLong(3);
				String difficulty = beatmapRs.getString(4);
				Beatmap beatmap = nestedMap.get(folderName).get(difficulty);
				if (beatmap.getLastModificationTime() != lastModificationTime) {
					int beatmapAutoID = beatmapRs.getInt(1);
					this.addUpdateBeatmapLMTBatch(updateBeatmapPStatement, beatmapAutoID, beatmap.getLastModificationTime());
					isAnyUpdated = true;
				}
				currentProgress++;
				if (this.progressUpdate != null) {
					this.progressUpdate.accept(currentProgress, totalProgress);
				}
			}
			// execute batch here as the size is not likely to be large
			updateBeatmapPStatement.executeBatch();
			this.getConn().commit();
		}
		finally {
			this.getConn().setAutoCommit(true);
			
			if (this.progressUpdate != null) {
				this.setProgressUpdate(null);
			}
		}
		
		
		System.out.println("finish");
		return isAnyUpdated;
	}
	
	// ------------- for inner use--------------------
	private void insertIntoBeatmapBatchWrapper(PreparedStatement beatmapPStatement, Beatmap beatmap, int beatmapSetAutoID) throws SQLException {
		this.insertIntoBeatmapBatch(beatmapPStatement,
				beatmap.getBeatmapID(),
				beatmapSetAutoID,
				beatmap.getRankedStatus(),
				beatmap.getLastModificationTime(),
				beatmap.getTotalTime(),
				beatmap.getPreviewTime(),
				beatmap.getThreadID(),
				beatmap.getDifficulty(),
//				beatmap.getGradeStandard(),
//				beatmap.getGradeTaiko(),
//				beatmap.getGradeCTB(),
//				beatmap.getGradeMania(),
				beatmap.isUnplayed(),
				beatmap.getLastPlayedTime()
				);
	}
	
	
	private ResultSet selectArtistIDFromArtist(String artistName, String artistNameUnicode) throws SQLException {
		String sql = "SELECT " + this.Data.Artist.ARTIST_ID + " FROM " + this.Data.Artist.TABLE_NAME + " "
				+ "WHERE " + this.Data.Artist.ARTIST_NAME + " = ? AND " + this.Data.Artist.ARTIST_NAME_UNICODE + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, artistName);
		pstmt.setString(2, artistNameUnicode);
		return pstmt.executeQuery();
	}
	
	private ResultSet selectSongIDFromSong(String songTitle, String songTitleUnicode, String songSource) throws SQLException {
		String sql = "SELECT " + this.Data.Song.SONG_ID + " FROM " + this.Data.Song.TABLE_NAME + " "
				+ "WHERE " + this.Data.Song.SONG_TITLE + " = ? AND " + this.Data.Song.SONG_TITLE_UNICODE + " = ? AND "
				+ this.Data.Song.SONG_SOURCE + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, songTitle);
		pstmt.setString(2, songTitleUnicode);
		pstmt.setString(3, songSource);
		return pstmt.executeQuery();
	}
	
	private ResultSet selectBeatmapSetAutoIDFromBeatmapSet(String folderName, String audioName) throws SQLException {
		String sql = "SELECT " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " FROM " + this.Data.BeatmapSet.TABLE_NAME + " "
				+ "WHERE " + this.Data.BeatmapSet.FOLDER_NAME + " = ? AND " + this.Data.BeatmapSet.AUDIO_NAME + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, folderName);
		pstmt.setString(2, audioName);
		return pstmt.executeQuery();
	}
	
	
	private ResultSet selectSongTagIDFromSongTag(String[] songTagNames) throws SQLException {
		String sql = "SELECT " + this.Data.SongTag.SONG_TAG_ID + " FROM " + this.Data.SongTag.TABLE_NAME + " "
				+ "WHERE " + this.Data.SongTag.SONG_TAG_NAME + " IN (" 
				+  String.join(",", Collections.nCopies(songTagNames.length, "?"))
				+ ");";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		for (int i = 1; i <= songTagNames.length; i++) {
			pstmt.setString(i, songTagNames[i - 1]);
		}
		return pstmt.executeQuery();
	}
	
	// for public use
	public ResultSet selectMetadata() throws SQLException {
		// always select the 1st row in case of duplicated data
		String sql = "SELECT * FROM " + this.Data.Metadata.TABLE_NAME + " ORDER BY ROWID ASC LIMIT 1";
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
	}
	
	public ResultSet selectConfig() throws SQLException {
		String sql = "SELECT * FROM " + this.Data.Config.TABLE_NAME + " ORDER BY ROWID ASC LIMIT 1";
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
	}
	
	
	public ResultSet getTableInitData() throws SQLException {
		String sql = "SELECT " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "," + this.Data.Song.SONG_SOURCE + "," + this.Data.Artist.ARTIST_NAME + "," + this.Data.Artist.ARTIST_NAME_UNICODE + "," + this.Data.Song.SONG_TITLE + "," + this.Data.Song.SONG_TITLE_UNICODE + "," 
				+ "MAX(" + this.Data.Beatmap.TOTAL_TIME + ") AS " + this.Data.Beatmap.TOTAL_TIME + "," + this.Data.Beatmap.LAST_MODIFICATION_TIME + "," + this.Data.BeatmapSet.IS_DOWNLOADED + "," + this.Data.BeatmapSet.IS_HIDDEN + "," + this.Data.BeatmapSet.FOLDER_NAME + "," 
				+ this.Data.BeatmapSet.AUDIO_NAME + ",group_concat(DISTINCT " + this.Data.SongTag.SONG_TAG_NAME + ") AS " + this.Data.SongTag.SONG_TAG_NAME + ","
				+ this.Data.BeatmapSet.CREATOR_NAME + "\n"
				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + "\n"
				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " ON " + this.Data.Beatmap.TABLE_NAME + "." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " ON " + this.Data.Artist.TABLE_NAME + "." + this.Data.Artist.ARTIST_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.ARTIST_ID + "\n"
				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " ON " + this.Data.Song.TABLE_NAME + "." + this.Data.Song.SONG_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.SONG_ID + "\n"
				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " ON " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " ON " + this.Data.SongTag.TABLE_NAME + "." + this.Data.SongTag.SONG_TAG_ID + " = " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
//				+ "WHERE " + this.Data.BeatmapSet.IS_HIDDEN + " = 0\n"
				+ "GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME + "\n"
				+ "ORDER BY MAX(" + this.Data.Beatmap.LAST_MODIFICATION_TIME + ")";
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
		
		
//		String sql = "SELECT " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "," + this.Data.Song.SONG_SOURCE + "," + this.Data.Artist.ARTIST_NAME + "," + this.Data.Artist.ARTIST_NAME_UNICODE + "," + this.Data.Song.SONG_TITLE + "," + this.Data.Song.SONG_TITLE_UNICODE + "," 
//				+ this.Data.Beatmap.TOTAL_TIME + "," + this.Data.Beatmap.LAST_MODIFICATION_TIME + "," + this.Data.BeatmapSet.IS_DOWNLOADED + "," + this.Data.BeatmapSet.IS_HIDDEN + "," + this.Data.BeatmapSet.FOLDER_NAME + "," 
//				+ this.Data.BeatmapSet.AUDIO_NAME + ",group_concat(DISTINCT " + this.Data.SongTag.SONG_TAG_NAME + ") AS " + this.Data.SongTag.SONG_TAG_NAME + ","
//				+ this.Data.BeatmapSet.CREATOR_NAME + "\n"
//				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + "\n"
//				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " ON " + this.Data.Beatmap.TABLE_NAME + "." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " ON " + this.Data.Artist.TABLE_NAME + "." + this.Data.Artist.ARTIST_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.ARTIST_ID + "\n"
//				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " ON " + this.Data.Song.TABLE_NAME + "." + this.Data.Song.SONG_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.SONG_ID + "\n"
//				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " ON " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " ON " + this.Data.SongTag.TABLE_NAME + "." + this.Data.SongTag.SONG_TAG_ID + " = " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
////				+ "WHERE " + this.Data.BeatmapSet.IS_HIDDEN + " = 0\n"
//				+ "GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME + "\n"
//				+ "HAVING MAX(" + this.Data.Beatmap.TOTAL_TIME + ")\n"
//				+ "ORDER BY " + this.Data.Beatmap.LAST_MODIFICATION_TIME;
//		Statement stmt = this.getConn().createStatement();
//		return stmt.executeQuery(sql);
		
	}
	
	
	public void updateMetadata(int metadataID, int osuVersion, int folderCount, String playerName, int numberOfBeatmaps) throws Exception, SQLException {
		String sql = "UPDATE " + this.Data.Metadata.TABLE_NAME + "\n"
				+ "SET " + this.Data.Metadata.OSU_VERSION + " = ?,"
				+ this.Data.Metadata.FOLDER_COUNT + " = ?,"
				+ this.Data.Metadata.PLAYER_NAME + " = ?,"
				+ this.Data.Metadata.NUMBER_OF_BEATMAPS + " = ? "
				+ "WHERE " + this.Data.Metadata.METADATA_ID + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setInt(1, osuVersion);
		pstmt.setInt(2, folderCount);
		pstmt.setString(3, playerName);
		pstmt.setInt(4, numberOfBeatmaps);
		pstmt.setInt(5, metadataID);
		pstmt.executeUpdate();
	}
	
	public void updateConfigFull(int configID, String pathToOsuDb, String pathToSongsFolder, String saveFolder,
			boolean isSongSourceShown, boolean isArtistNameShown, boolean isArtistNameUnicodeShown,
			boolean isSongTitleShown, boolean isSongTitleUnicodeShown, boolean isCreatorNameShown,
			boolean isTotalTimeShown, boolean isIsDownloadedShown, String ordering, double soundVolume,
			boolean isRepeatToggled, boolean isShuffleToggled) throws SQLException {
		
		String sql = "UPDATE " + this.Data.Config.TABLE_NAME + "\n"
				+ "SET " + this.Data.Config.PATH_TO_OSU_DB + " = ?,"
				+ this.Data.Config.PATH_TO_SONGS_FOLDER + " = ?,"
				+ this.Data.Config.SAVE_FOLDER + " = ?,"
				+ this.Data.Config.IS_SONG_SOURCE_SHOWN + " = ?,"
				+ this.Data.Config.IS_ARTIST_NAME_SHOWN + " = ?,"
				+ this.Data.Config.IS_ARTIST_NAME_UNICODE_SHOWN + " = ?,"
				+ this.Data.Config.IS_SONG_TITLE_SHOWN + " = ?,"
				+ this.Data.Config.IS_SONG_TITLE_UNICODE_SHOWN + " = ?,"
				+ this.Data.Config.IS_CREATOR_NAME_SHOWN + " = ?,"
				+ this.Data.Config.IS_TOTAL_TIME_SHOWN + " = ?,"
				+ this.Data.Config.IS_IS_DOWNLOADED_SHOWN + " = ?,"
				+ this.Data.Config.ORDERING + " = ?,"
				+ this.Data.Config.SOUND_VOLUME + " = ?,"
				+ this.Data.Config.IS_REPEAT_TOGGLED + " = ?,"
				+ this.Data.Config.IS_SHUFFLE_TOGGLED + " = ? "
				+ "WHERE " + this.Data.Config.CONFIG_ID + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, pathToOsuDb);
		pstmt.setString(2, pathToSongsFolder);
		pstmt.setString(3, saveFolder);
		pstmt.setBoolean(4, isSongSourceShown);
		pstmt.setBoolean(5, isArtistNameShown);
		pstmt.setBoolean(6, isArtistNameUnicodeShown);
		pstmt.setBoolean(7, isSongTitleShown);
		pstmt.setBoolean(8, isSongTitleUnicodeShown);
		pstmt.setBoolean(9, isCreatorNameShown);
		pstmt.setBoolean(10, isTotalTimeShown);
		pstmt.setBoolean(11, isIsDownloadedShown);
		pstmt.setString(12, ordering);
		pstmt.setDouble(13, soundVolume);
		pstmt.setBoolean(14, isRepeatToggled);
		pstmt.setBoolean(15, isShuffleToggled);
		pstmt.setInt(16, configID);
		pstmt.executeUpdate();
	}
	
	public void updateConfigString(int configID, String[] items, String[] results) throws SQLException {
		if (items.length != results.length) {
			throw new RuntimeException("Update config string argument num doesn't match");
		}
		
		String sql = "UPDATE " + this.Data.Config.TABLE_NAME + "\n"
				+ "SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString();
		sql += " WHERE " + this.Data.Config.CONFIG_ID + " = ?;";
		
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		int index = 1;
		for (int i = 0; i < results.length; i++) {
			pstmt.setString(index, results[i]);
			index++;
		}
		pstmt.setInt(index, configID);
		pstmt.executeUpdate();
	}
	
	public void updateConfigBoolean(int configID, String[] items, boolean[] results) throws SQLException {
		if (items.length != results.length) {
			throw new RuntimeException("Update config boolean argument num doesn't match");
		}
		
		String sql = "UPDATE " + this.Data.Config.TABLE_NAME + "\n"
				+ "SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString();
		sql += " WHERE " + this.Data.Config.CONFIG_ID + " = ?;";
		
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		int index = 1;
		for (int i = 0; i < results.length; i++) {
			pstmt.setBoolean(index, results[i]);
			index++;
		}
		pstmt.setInt(index, configID);
		pstmt.executeUpdate();
	}
	
	private PreparedStatement getUpdateBeatmapPStatement(String[] items) throws SQLException {
		String sql = "UPDATE " + this.Data.Beatmap.TABLE_NAME + " SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString() + " WHERE " + this.Data.Beatmap.BEATMAP_AUTO_ID + " = ?";
		return this.getConn().prepareStatement(sql);
	}
	
	private void addUpdateBeatmapLMTBatch(PreparedStatement updateBeatmapPStatement, int beatmapAutoID, long lastModificationTime) throws SQLException {
		updateBeatmapPStatement.setLong(1, lastModificationTime);
		updateBeatmapPStatement.setInt(2, beatmapAutoID);
		updateBeatmapPStatement.addBatch();
	}
	
	// TODO: make all these statements to be safe by providing checks
	public PreparedStatement getUpdateBeatmapSetBooleanPStatement(String[] items) throws SQLException {
		String sql = "UPDATE " + this.Data.BeatmapSet.TABLE_NAME + " SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString() + " WHERE " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
		return this.getConn().prepareStatement(sql);
	}
	// results order must be same as that of items
	public void addUpdateBeatmapSetBatch(PreparedStatement updateBeatmapSetBooleanPStatement, int beatmapSetAutoID, Boolean[] results) throws SQLException {
		int index = 1;
		for (int i = 0; i < results.length; i++) {
			updateBeatmapSetBooleanPStatement.setBoolean(index, results[i]);
			index++;
		}
		updateBeatmapSetBooleanPStatement.setInt(index, beatmapSetAutoID);
		updateBeatmapSetBooleanPStatement.addBatch();
	}
	
	
	
	
	// for threading only
	public void cleanUpThread(boolean deleteSongsDb) throws SQLException {
		this.closeConnection();
		if (deleteSongsDb) {
			this.deleteSongsDb();
		}
	}
	
	public void deleteSongsDb() {
		File db = new File(this.DB_NAME);
		db.delete();
	}
	
	public boolean isDbExist() {
		return this.isDbExist;
	}
	
	private void setDbExist(boolean isDbExist) {
		this.isDbExist = isDbExist;
	}
	
	public Connection getConn() {
		return this.conn;
	}
	
	private void setConn(Connection conn) {
		this.conn = conn;
	}
	
	private void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
		this.progressUpdate = progressUpdate;
	}
	
	
	// TODO: change to static fields(?) instead of this shit
	public class SongsDbData {
		public Metadata Metadata = new Metadata();
		public Config Config = new Config();
		public Beatmap Beatmap = new Beatmap();
		public BeatmapSet BeatmapSet = new BeatmapSet();
		public Artist Artist = new Artist();
		public Song Song = new Song();
		public SongTag SongTag = new SongTag();
		public BeatmapSet_SongTag BeatmapSet_SongTag = new BeatmapSet_SongTag();
		
		public class Metadata {
			public final String TABLE_NAME = "Metadata";
			// ===== Metadata fields =====
			public final String METADATA_ID = "MetadataID";
			public final String OSU_VERSION = "OsuVersion";
			public final String FOLDER_COUNT = "FolderCount";
			public final String PLAYER_NAME = "PlayerName";
			public final String NUMBER_OF_BEATMAPS = "NumberOfBeatmaps";
		}
		
		public class Config {
			public final String TABLE_NAME = "Config";
			// ===== Config fields =====
			public final String CONFIG_ID = "ConfigID";
			public final String PATH_TO_OSU_DB = "PathToOsuDb";
			public final String PATH_TO_SONGS_FOLDER = "PathToSongsFolder";
			public final String SAVE_FOLDER = "SaveFolder";
			public final String IS_SONG_SOURCE_SHOWN = "IsSongSourceShown";
			public final String IS_ARTIST_NAME_SHOWN = "IsArtistNameShown";
			public final String IS_ARTIST_NAME_UNICODE_SHOWN = "IsArtistNameUnicodeShown";
			public final String IS_SONG_TITLE_SHOWN = "IsSongTitleShown";
			public final String IS_SONG_TITLE_UNICODE_SHOWN = "IsSongTitleUnicodeShown";
			public final String IS_CREATOR_NAME_SHOWN = "IsCreatorNameShown";
			public final String IS_TOTAL_TIME_SHOWN = "IsTotalTimeShown";
			public final String IS_IS_DOWNLOADED_SHOWN = "IsIsDownloadedShown";
			public final String ORDERING = "Ordering";
			public final String SOUND_VOLUME = "SoundVolume";
			public final String IS_REPEAT_TOGGLED = "IsRepeatToggled";
			public final String IS_SHUFFLE_TOGGLED = "IsShuffleToggled";
		}
		
		public class Beatmap {
			public final String TABLE_NAME = "Beatmap";
			// ===== Beatmap fields =====
			public final String BEATMAP_AUTO_ID = "BeatmapAutoID";
			public final String BEATMAP_ID = "BeatmapID";
			public final String BEATMAP_SET_AUTO_ID = "BeatmapSetAutoID";
			public final String RANKED_STATUS = "RankedStatus";
			public final String LAST_MODIFICATION_TIME = "LastModificationTime";
			public final String TOTAL_TIME = "TotalTime";
			public final String PREVIEW_TIME = "PreviewTime";
			public final String THREAD_ID = "ThreadID";
			public final String DIFFICULTY = "Difficulty";
//			public final String NAME_OF_OSU_FILE = "NameOfOsuFile";
//			public final String GRADE_STANDARD = "GradeStandard";
//			public final String GRADE_TAIKO = "GradeTaiko";
//			public final String GRADE_CTB = "GradeCTB";
//			public final String GRADE_MANIA = "GradeMania";
			public final String IS_UNPLAYED = "IsUnplayed";
			public final String LAST_PLAYED_TIME = "LastPlayedTime";
		}
		
		public class BeatmapSet {
			public final String TABLE_NAME = "BeatmapSet";
			// ===== BeatmapSet fields =====
			public final String BEATMAP_SET_AUTO_ID = "BeatmapSetAutoID";
			public final String BEATMAP_SET_ID = "BeatmapSetID";
			public final String ARTIST_ID = "ArtistID";
			public final String SONG_ID = "SongID";
			public final String CREATOR_NAME = "CreatorName";
			public final String FOLDER_NAME = "FolderName"; 
			public final String AUDIO_NAME = "AudioName"; 
			public final String IS_DOWNLOADED = "IsDownloaded";
			public final String IS_HIDDEN = "IsHidden";
		}
		
		public class Artist {
			public final String TABLE_NAME = "Artist";
			// ===== Artist fields =====
			public final String ARTIST_ID = "ArtistID";
			public final String ARTIST_NAME = "ArtistName";
			public final String ARTIST_NAME_UNICODE = "ArtistNameUnicode";
		}
		
		public class Song {
			public final String TABLE_NAME = "Song";
			// ===== Song fields =====
			public final String SONG_ID = "SongID";
			public final String SONG_TITLE = "SongTitle";
			public final String SONG_TITLE_UNICODE = "SongTitleUnicode";
			public final String SONG_SOURCE = "SongSource";
		}
		
		public class SongTag {
			public final String TABLE_NAME = "SongTag";
			// ===== SongTag fields =====
			public final String SONG_TAG_ID = "SongTagID";
			public final String SONG_TAG_NAME = "SongTagName";
		}
		
		public class BeatmapSet_SongTag {
			public final String TABLE_NAME = "BeatmapSet_SongTag";
			// ===== BeatmapSet_SongTag fields =====
			public final String BEATMAP_SET_AUTO_ID = "BeatmapSetAutoID";
			public final String SONG_TAG_ID = "SongTagID";
		}
		
	}
}