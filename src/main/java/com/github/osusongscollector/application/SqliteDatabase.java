package com.github.osusongscollector.application;

import com.github.osusongscollector.controllers.SaveToOptionController.ComboBoxChoice;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;


// REMINDER: if any modification to the table (ie. add or delete fields) in the future, bear in mind
// to use insert but not change them directly otherwise error will happen and user will have to delete 
// songsDb which loses their data

public class SqliteDatabase {
	private final String SQLITE_PREFIX = "jdbc:sqlite:";
	private final String URL;
	public final String DB_NAME;
	
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
        // turn on foreign key constraint on every connection as it is off by default
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
        // ensure encoding use is utf-8
		Statement stmt = this.getConn().createStatement();
		stmt.execute("PRAGMA encoding = 'UTF-8'");
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
		String sql = "CREATE TABLE IF NOT EXISTS "+ TableData.Metadata.TABLE_NAME + " ("
				+ TableData.Metadata.METADATA_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.Metadata.OSU_VERSION + " INTEGER,"
				+ TableData.Metadata.FOLDER_COUNT + " INTEGER,"
				+ TableData.Metadata.PLAYER_NAME + " TEXT,"
				+ TableData.Metadata.NUMBER_OF_BEATMAPS + " INTEGER"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableConfig() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.Config.TABLE_NAME + " ("
				+ TableData.Config.CONFIG_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.Config.PATH_TO_OSU_DB + " TEXT,"
				+ TableData.Config.PATH_TO_SONGS_FOLDER + " TEXT,"
				+ TableData.Config.SAVE_FOLDER + " TEXT,"
				+ TableData.Config.IS_SONG_SOURCE_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_ARTIST_NAME_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_ARTIST_NAME_UNICODE_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_SONG_TITLE_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_SONG_TITLE_UNICODE_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_CREATOR_NAME_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_TOTAL_TIME_SHOWN + " BOOLEAN,"
				+ TableData.Config.IS_IS_DOWNLOADED_SHOWN + " BOOLEAN,"
				+ TableData.Config.ORDERING + " TEXT,"
				+ TableData.Config.SOUND_VOLUME + " REAL,"
				+ TableData.Config.IS_REPEAT_TOGGLED + " BOOLEAN,"
				+ TableData.Config.IS_SHUFFLE_TOGGLED + " BOOLEAN,"
				+ TableData.Config.COMBO_BOX_PREFIX + " TEXT,"
				+ TableData.Config.COMBO_BOX_SUFFIX + " TEXT"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableBeatmapSet() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.BeatmapSet.TABLE_NAME + " ("
				+ TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.BeatmapSet.BEATMAP_SET_ID + " INTEGER,"
				+ TableData.BeatmapSet.ARTIST_ID + " INTEGER,"
				+ TableData.BeatmapSet.SONG_ID + " INTEGER,"
				+ TableData.BeatmapSet.CREATOR_NAME + " TEXT COLLATE NOCASE,"
				+ TableData.BeatmapSet.FOLDER_NAME + " TEXT COLLATE NOCASE,"
				+ TableData.BeatmapSet.AUDIO_NAME + " TEXT,"
				+ TableData.BeatmapSet.IS_DOWNLOADED + " BOOLEAN,"
				+ TableData.BeatmapSet.IS_HIDDEN + " BOOLEAN,"
				+ "UNIQUE (" + TableData.BeatmapSet.FOLDER_NAME + "," + TableData.BeatmapSet.AUDIO_NAME + "),"
				+ "FOREIGN KEY (" + TableData.BeatmapSet.ARTIST_ID + ") REFERENCES " + TableData.Artist.TABLE_NAME + "(" + TableData.Artist.ARTIST_ID + ")" + " ON UPDATE CASCADE ON DELETE CASCADE,"
				+ "FOREIGN KEY (" + TableData.BeatmapSet.SONG_ID + ") REFERENCES " + TableData.Song.TABLE_NAME + "(" + TableData.Song.SONG_ID + ")" + " ON UPDATE CASCADE ON DELETE CASCADE"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableArtist() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.Artist.TABLE_NAME + " ("
				+ TableData.Artist.ARTIST_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.Artist.ARTIST_NAME + " TEXT COLLATE NOCASE,"
				+ TableData.Artist.ARTIST_NAME_UNICODE + " TEXT COLLATE NOCASE,"
				+ "UNIQUE (" + TableData.Artist.ARTIST_NAME + ", " + TableData.Artist.ARTIST_NAME_UNICODE + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableSong() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.Song.TABLE_NAME + " ("
				+ TableData.Song.SONG_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.Song.SONG_TITLE + " TEXT COLLATE NOCASE,"
				+ TableData.Song.SONG_TITLE_UNICODE + " TEXT COLLATE NOCASE,"
				+ TableData.Song.SONG_SOURCE + " TEXT,"
				+ "UNIQUE (" + TableData.Song.SONG_TITLE + "," + TableData.Song.SONG_TITLE_UNICODE + "," + TableData.Song.SONG_SOURCE + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableSongTag() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.SongTag.TABLE_NAME + " ("
				+ TableData.SongTag.SONG_TAG_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.SongTag.SONG_TAG_NAME + " TEXT COLLATE NOCASE,"
				+ "UNIQUE (" + TableData.SongTag.SONG_TAG_NAME + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableBeatmap() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.Beatmap.TABLE_NAME + " ("
				+ TableData.Beatmap.BEATMAP_AUTO_ID + " INTEGER NOT NULL PRIMARY KEY,"
				+ TableData.Beatmap.BEATMAP_ID + " INTEGER,"
				+ TableData.Beatmap.BEATMAP_SET_AUTO_ID + " INTEGER,"
				+ TableData.Beatmap.RANKED_STATUS + " INTEGER,"
				+ TableData.Beatmap.LAST_MODIFICATION_TIME + " INTEGER,"
				+ TableData.Beatmap.TOTAL_TIME + " INTEGER,"
				+ TableData.Beatmap.PREVIEW_TIME + " INTEGER,"
				+ TableData.Beatmap.THREAD_ID + " INTEGER,"
				+ TableData.Beatmap.DIFFICULTY + " TEXT,"
				+ TableData.Beatmap.IS_UNPLAYED + " BOOLEAN,"
				+ TableData.Beatmap.LAST_PLAYED_TIME + " INTEGER,"
				+ "FOREIGN KEY (" + TableData.Beatmap.BEATMAP_SET_AUTO_ID + ") REFERENCES " + TableData.BeatmapSet.TABLE_NAME + "(" + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + ")" + " ON UPDATE CASCADE ON DELETE CASCADE"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	private void createTableBeatmapSet_SongTag() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + TableData.BeatmapSet_SongTag.TABLE_NAME + "("
				+ TableData.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " INTEGER NOT NULL,"
				+ TableData.BeatmapSet_SongTag.SONG_TAG_ID + " INTEGER NOT NULL,"
				+ "PRIMARY KEY (" + TableData.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + "," + TableData.BeatmapSet_SongTag.SONG_TAG_ID + ")"
				+ ");";
		Statement stmt = this.getConn().createStatement();
		stmt.execute(sql);
	}
	
	
	private void createIndexBeatmapSet() throws SQLException {
		Statement stmt = this.getConn().createStatement();
		String sql = "CREATE INDEX IF NOT EXISTS idx_artist_id ON " + TableData.BeatmapSet.TABLE_NAME + "(" + TableData.BeatmapSet.ARTIST_ID + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_song_id ON " + TableData.BeatmapSet.TABLE_NAME + "(" + TableData.BeatmapSet.SONG_ID + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_creator_name ON " + TableData.BeatmapSet.TABLE_NAME + "(" + TableData.BeatmapSet.CREATOR_NAME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_folder_name ON " + TableData.BeatmapSet.TABLE_NAME + "(" + TableData.BeatmapSet.FOLDER_NAME + ");";
		stmt.execute(sql);
	}
	
	private void createIndexBeatmap() throws SQLException {
		Statement stmt = this.getConn().createStatement();
		String sql = "CREATE INDEX IF NOT EXISTS idx_beatmap_set_auto_id ON " + TableData.Beatmap.TABLE_NAME + "(" + TableData.Beatmap.BEATMAP_SET_AUTO_ID + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_last_modification_time ON " + TableData.Beatmap.TABLE_NAME + "(" + TableData.Beatmap.LAST_MODIFICATION_TIME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_total_time ON " + TableData.Beatmap.TABLE_NAME + "(" + TableData.Beatmap.TOTAL_TIME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_preview_time ON " + TableData.Beatmap.TABLE_NAME + "(" + TableData.Beatmap.PREVIEW_TIME + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_difficulty ON " + TableData.Beatmap.TABLE_NAME + "(" + TableData.Beatmap.DIFFICULTY + ");";
		stmt.execute(sql);
		sql = "CREATE INDEX IF NOT EXISTS idx_last_played_time ON " + TableData.Beatmap.TABLE_NAME + "(" + TableData.Beatmap.LAST_PLAYED_TIME + ");";
		stmt.execute(sql);
	}
	
	
	private void insertIntoMetadata(int osuVersion, int folderCount, String playerName, int numberOfBeatmaps) throws SQLException {
		String sql = "INSERT INTO " + TableData.Metadata.TABLE_NAME + "(" 
				+ TableData.Metadata.OSU_VERSION + "," 
				+ TableData.Metadata.FOLDER_COUNT + "," 
				+ TableData.Metadata.PLAYER_NAME + "," 
				+ TableData.Metadata.NUMBER_OF_BEATMAPS
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
			boolean isRepeatToggled, boolean isShuffleToggled, String comboBoxPrefix,
			String comboBoxSuffix) throws SQLException {
		String sql = "INSERT INTO " + TableData.Config.TABLE_NAME + "(" 
				+ TableData.Config.PATH_TO_OSU_DB + ","
				+ TableData.Config.PATH_TO_SONGS_FOLDER + ","
				+ TableData.Config.SAVE_FOLDER + ","
				+ TableData.Config.IS_SONG_SOURCE_SHOWN + ","
				+ TableData.Config.IS_ARTIST_NAME_SHOWN + ","
				+ TableData.Config.IS_ARTIST_NAME_UNICODE_SHOWN + ","
				+ TableData.Config.IS_SONG_TITLE_SHOWN + ","
				+ TableData.Config.IS_SONG_TITLE_UNICODE_SHOWN + ","
				+ TableData.Config.IS_CREATOR_NAME_SHOWN + ","
				+ TableData.Config.IS_TOTAL_TIME_SHOWN + ","
				+ TableData.Config.IS_IS_DOWNLOADED_SHOWN + ","
				+ TableData.Config.ORDERING + ","
				+ TableData.Config.SOUND_VOLUME + ","
				+ TableData.Config.IS_REPEAT_TOGGLED + ","
				+ TableData.Config.IS_SHUFFLE_TOGGLED + ","
				+ TableData.Config.COMBO_BOX_PREFIX + ","
				+ TableData.Config.COMBO_BOX_SUFFIX
				+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
		pstmt.setString(16, comboBoxPrefix);
		pstmt.setString(17, comboBoxSuffix);
		pstmt.executeUpdate();
	}
	
	private PreparedStatement getInsertIntoBeatmapSetPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + TableData.BeatmapSet.TABLE_NAME + "("
				+ TableData.BeatmapSet.BEATMAP_SET_ID + ","
				+ TableData.BeatmapSet.ARTIST_ID + ","
				+ TableData.BeatmapSet.SONG_ID + ","
				+ TableData.BeatmapSet.CREATOR_NAME + ","
				+ TableData.BeatmapSet.FOLDER_NAME + ","
				+ TableData.BeatmapSet.AUDIO_NAME + ","
				+ TableData.BeatmapSet.IS_DOWNLOADED + ","
				+ TableData.BeatmapSet.IS_HIDDEN 
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
		String sql = "INSERT OR IGNORE INTO " + TableData.Artist.TABLE_NAME + "(" 
				+ TableData.Artist.ARTIST_NAME + ","
				+ TableData.Artist.ARTIST_NAME_UNICODE
				+ ") VALUES(?,?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoArtistBatch(PreparedStatement artistPStatement, String artistName, String artistNameUnicode) throws SQLException {
		artistPStatement.setString(1, artistName);
		artistPStatement.setString(2, artistNameUnicode);
		artistPStatement.addBatch();
	}
	
	
	private PreparedStatement getInsertIntoSongPStatement() throws SQLException {
		String sql = "INSERT OR IGNORE INTO " + TableData.Song.TABLE_NAME + "("
				+ TableData.Song.SONG_TITLE + ","
				+ TableData.Song.SONG_TITLE_UNICODE + ","
				+ TableData.Song.SONG_SOURCE
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
		String sql = "INSERT OR IGNORE INTO " + TableData.SongTag.TABLE_NAME + "(" 
				+ TableData.SongTag.SONG_TAG_NAME
				+ ") VALUES(?)";
		return this.getConn().prepareStatement(sql);
	}
	
	private void insertIntoSongTagBatch(PreparedStatement songTagPStatement, String songTagName) throws SQLException {
		songTagPStatement.setString(1, songTagName);
		songTagPStatement.addBatch();
	}
	

			
	private PreparedStatement getInsertIntoBeatmapPStatement() throws SQLException {
		String sql = "INSERT INTO " + TableData.Beatmap.TABLE_NAME + "("
				+ TableData.Beatmap.BEATMAP_ID + ","
				+ TableData.Beatmap.BEATMAP_SET_AUTO_ID + ","
				+ TableData.Beatmap.RANKED_STATUS + ","
				+ TableData.Beatmap.LAST_MODIFICATION_TIME + ","
				+ TableData.Beatmap.TOTAL_TIME + ","
				+ TableData.Beatmap.PREVIEW_TIME + ","
				+ TableData.Beatmap.THREAD_ID + ","
				+ TableData.Beatmap.DIFFICULTY + ","
//				+ TableData.Beatmap.GRADE_STANDARD + ","
//				+ TableData.Beatmap.GRADE_TAIKO + ","
//				+ TableData.Beatmap.GRADE_CTB + ","
//				+ TableData.Beatmap.GRADE_MANIA + ","
				+ TableData.Beatmap.IS_UNPLAYED + ","
				+ TableData.Beatmap.LAST_PLAYED_TIME
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
		String sql = "INSERT OR IGNORE INTO " + TableData.BeatmapSet_SongTag.TABLE_NAME + "(" 
				+ TableData.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + ","
				+ TableData.BeatmapSet_SongTag.SONG_TAG_ID 
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
				throw new InterruptedException("CreateDatabaseTask is interrupted at: (" + i + "/" + (dataToInsert.size() - 1) + ") while inserting artist, song, and songTag");
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
				throw new InterruptedException("CreateDatabaseTask is interrupted at: (" + i + "/" + (dataToInsert.size() -1) + ") while inserting beatmapSet");
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
				throw new InterruptedException("CreateDatabaseTask is interrupted at: " + i + "/" + (dataToInsert.size() - 1) + ") while inserting beatmap and beatmapSet_songTag");
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
		this.insertIntoConfig(osuDb.getPathToOsuDb(), osuDb.getPathToSongsFolder(), "", false, false, false, false, false, false, false, false, "", 50.0, false, false, ComboBoxChoice.NONE.toString(), ComboBoxChoice.NONE.toString());
		
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
				// check cuz if the insertDataIntoDb method is interrupted, it closes the db connection and this will throw error
				if (!Thread.currentThread().isInterrupted()) {
					this.getConn().setAutoCommit(true);
				}
				
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
		
		String selectAllBeatmapSetAutoIDSql = "SELECT " + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " FROM " + TableData.BeatmapSet.TABLE_NAME;
		Statement allBeatmapSetAutoIDStatement = this.getConn().createStatement();
		
		// getting all ID
		ResultSet allBeatmapSetAutoIDRs = allBeatmapSetAutoIDStatement.executeQuery(selectAllBeatmapSetAutoIDSql);
		while (allBeatmapSetAutoIDRs.next()) {
			int beatmapSetAutoID = allBeatmapSetAutoIDRs.getInt(1);
			dbRecords.put(beatmapSetAutoID, 0);
		}
		
		
		
		String selectBeatmapCountUsingBeatmapSetAutoIDSql = "SELECT COUNT(*) FROM " + TableData.Beatmap.TABLE_NAME 
				+ " WHERE " + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
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
		
		
		
		// Start checking for updates
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
		
		
		isAnyUpdated = updateList.isEmpty() && modifiedList.isEmpty() && dbRecords.isEmpty() ? false : true;
		if (Thread.currentThread().isInterrupted()) {
			// not closing songDb connection here as it might be at an instance where user is already in displaySongs stage
			throw new InterruptedException("Interrupted before starting to update songs");
		}
		
		// even if all is empty, still go till the end to clean up certain img
		// wrap in try to clean img afterwards
		try {
			if (!dbRecords.isEmpty()) {
				// delete
				String deleteFromBeatmapSetSql = "DELETE FROM " + TableData.BeatmapSet.TABLE_NAME + " WHERE "; 
				StringJoiner sj = new StringJoiner(" OR ");
				for (int i = 0; i < dbRecords.size(); i++) {
					sj.add(TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?");
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
				
				// modified songs
				String getBeatmapAutoIDAndDifficultySql = "SELECT " + TableData.Beatmap.BEATMAP_AUTO_ID 
						+ "," + TableData.Beatmap.DIFFICULTY + " FROM "
						+ TableData.Beatmap.TABLE_NAME + " WHERE " + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
				PreparedStatement getBeatmapAutoIDAndDifficultyPStatement = this.getConn().prepareStatement(getBeatmapAutoIDAndDifficultySql);
				
				String deleteFromBeatmapSql = "DELETE FROM " + TableData.Beatmap.TABLE_NAME + " WHERE "
						+ TableData.Beatmap.BEATMAP_AUTO_ID + " = ?";
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
							String difficulty = beatmapAutoIDAndDifficultyRs.getString(2);
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
				throw new InterruptedException("Interrupted before inserting new songs");
			}
			
			if (this.progressUpdate != null) {
				this.progressUpdate.accept(3, totalProgress);
			}
			
			
			if (!updateList.isEmpty()) {
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
			if (!Thread.currentThread().isInterrupted()) {
				this.getConn().setAutoCommit(true);
			}
			
			// if threading, set back to null to prevent inadvertent access later
			if (this.progressUpdate != null) {
				this.setProgressUpdate(null);
			}
		}
		
		// lastly update metadata here so that if disrupted, next start will still have same metadata and probably come here
		// to update again if no change was done to osuDb between this period
		ResultSet metadataRs = this.selectMetadata();
		if (metadataRs.next()) {
			int metadataID = metadataRs.getInt(TableData.Metadata.METADATA_ID);
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
		
		
		for (Map.Entry<String, List<Beatmap>> entry : osuDbBeatmapsMap.entrySet()) {
			String folderName = entry.getKey();
			List<Beatmap> beatmapSet = entry.getValue();
			Map<String, Beatmap> beatmapsMap = beatmapSet.stream().collect(Collectors.toMap(Beatmap::getDifficulty, Function.identity()));
			nestedMap.put(folderName, beatmapsMap);
			totalProgress += beatmapsMap.size();
		}
		
		
		String selectBeatmapSql = "SELECT " + TableData.Beatmap.BEATMAP_AUTO_ID + "," 
				+ TableData.BeatmapSet.FOLDER_NAME + ","
				+ TableData.Beatmap.LAST_MODIFICATION_TIME + ","
				+ TableData.Beatmap.DIFFICULTY
				+ " FROM " + TableData.Beatmap.TABLE_NAME
				+ " INNER JOIN " + TableData.BeatmapSet.TABLE_NAME + " ON " + TableData.BeatmapSet.TABLE_NAME + "." 
				+ TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " = " + TableData.Beatmap.TABLE_NAME + "."
				+ TableData.Beatmap.BEATMAP_SET_AUTO_ID;
		
		String[] items = {TableData.Beatmap.LAST_MODIFICATION_TIME};
		PreparedStatement updateBeatmapPStatement = this.getUpdateBeatmapPStatement(items);
		Statement stmt = this.getConn().createStatement();
		
		// updating
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
				beatmap.isUnplayed(),
				beatmap.getLastPlayedTime()
				);
	}
	
	
	private ResultSet selectArtistIDFromArtist(String artistName, String artistNameUnicode) throws SQLException {
		String sql = "SELECT " + TableData.Artist.ARTIST_ID + " FROM " + TableData.Artist.TABLE_NAME + " "
				+ "WHERE " + TableData.Artist.ARTIST_NAME + " = ? AND " + TableData.Artist.ARTIST_NAME_UNICODE + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, artistName);
		pstmt.setString(2, artistNameUnicode);
		return pstmt.executeQuery();
	}
	
	private ResultSet selectSongIDFromSong(String songTitle, String songTitleUnicode, String songSource) throws SQLException {
		String sql = "SELECT " + TableData.Song.SONG_ID + " FROM " + TableData.Song.TABLE_NAME + " "
				+ "WHERE " + TableData.Song.SONG_TITLE + " = ? AND " + TableData.Song.SONG_TITLE_UNICODE + " = ? AND "
				+ TableData.Song.SONG_SOURCE + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, songTitle);
		pstmt.setString(2, songTitleUnicode);
		pstmt.setString(3, songSource);
		return pstmt.executeQuery();
	}
	
	private ResultSet selectBeatmapSetAutoIDFromBeatmapSet(String folderName, String audioName) throws SQLException {
		String sql = "SELECT " + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " FROM " + TableData.BeatmapSet.TABLE_NAME + " "
				+ "WHERE " + TableData.BeatmapSet.FOLDER_NAME + " = ? AND " + TableData.BeatmapSet.AUDIO_NAME + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, folderName);
		pstmt.setString(2, audioName);
		return pstmt.executeQuery();
	}
	
	
	private ResultSet selectSongTagIDFromSongTag(String[] songTagNames) throws SQLException {
		String sql = "SELECT " + TableData.SongTag.SONG_TAG_ID + " FROM " + TableData.SongTag.TABLE_NAME + " "
				+ "WHERE " + TableData.SongTag.SONG_TAG_NAME + " IN (" 
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
		String sql = "SELECT * FROM " + TableData.Metadata.TABLE_NAME + " ORDER BY ROWID ASC LIMIT 1";
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
	}
	
	public ResultSet selectConfig() throws SQLException {
		String sql = "SELECT * FROM " + TableData.Config.TABLE_NAME + " ORDER BY ROWID ASC LIMIT 1";
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
	}
	
	
	public ResultSet getTableInitData() throws SQLException {
		String sql = "SELECT " + TableData.BeatmapSet.TABLE_NAME + "." + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + "," + TableData.Song.SONG_SOURCE + "," + TableData.Artist.ARTIST_NAME + "," + TableData.Artist.ARTIST_NAME_UNICODE + "," + TableData.Song.SONG_TITLE + "," + TableData.Song.SONG_TITLE_UNICODE + "," 
				+ "MAX(" + TableData.Beatmap.TOTAL_TIME + ") AS " + TableData.Beatmap.TOTAL_TIME + "," + TableData.Beatmap.LAST_MODIFICATION_TIME + "," + TableData.BeatmapSet.IS_DOWNLOADED + "," + TableData.BeatmapSet.IS_HIDDEN + "," + TableData.BeatmapSet.FOLDER_NAME + "," 
				+ TableData.BeatmapSet.AUDIO_NAME + ",group_concat(DISTINCT " + TableData.SongTag.SONG_TAG_NAME + ") AS " + TableData.SongTag.SONG_TAG_NAME + ","
				+ TableData.BeatmapSet.CREATOR_NAME + "\n"
				+ "FROM " + TableData.BeatmapSet.TABLE_NAME + "\n"
				+ "INNER JOIN " + TableData.Beatmap.TABLE_NAME + " ON " + TableData.Beatmap.TABLE_NAME + "." + TableData.Beatmap.BEATMAP_SET_AUTO_ID + " = " + TableData.BeatmapSet.TABLE_NAME + "." + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + TableData.Artist.TABLE_NAME + " ON " + TableData.Artist.TABLE_NAME + "." + TableData.Artist.ARTIST_ID + " = " + TableData.BeatmapSet.TABLE_NAME + "." + TableData.BeatmapSet.ARTIST_ID + "\n"
				+ "INNER JOIN " + TableData.Song.TABLE_NAME + " ON " + TableData.Song.TABLE_NAME + "." + TableData.Song.SONG_ID + " = " + TableData.BeatmapSet.TABLE_NAME + "." + TableData.BeatmapSet.SONG_ID + "\n"
				+ "INNER JOIN " + TableData.BeatmapSet_SongTag.TABLE_NAME + " ON " + TableData.BeatmapSet_SongTag.TABLE_NAME + "." + TableData.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = " + TableData.BeatmapSet.TABLE_NAME + "." + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + TableData.SongTag.TABLE_NAME + " ON " + TableData.SongTag.TABLE_NAME + "." + TableData.SongTag.SONG_TAG_ID + " = " + TableData.BeatmapSet_SongTag.TABLE_NAME + "." + TableData.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
//				+ "WHERE " + TableData.BeatmapSet.IS_HIDDEN + " = 0\n"
				+ "GROUP BY " + TableData.BeatmapSet.FOLDER_NAME + ", " + TableData.BeatmapSet.AUDIO_NAME + "\n"
				+ "ORDER BY MAX(" + TableData.Beatmap.LAST_MODIFICATION_TIME + ")";
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
	}
	
	
	public void updateMetadata(int metadataID, int osuVersion, int folderCount, String playerName, int numberOfBeatmaps) throws Exception, SQLException {
		String sql = "UPDATE " + TableData.Metadata.TABLE_NAME + "\n"
				+ "SET " + TableData.Metadata.OSU_VERSION + " = ?,"
				+ TableData.Metadata.FOLDER_COUNT + " = ?,"
				+ TableData.Metadata.PLAYER_NAME + " = ?,"
				+ TableData.Metadata.NUMBER_OF_BEATMAPS + " = ? "
				+ "WHERE " + TableData.Metadata.METADATA_ID + " = ?";
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
			boolean isRepeatToggled, boolean isShuffleToggled, String comboBoxPrefix,
			String comboBoxSuffix) throws SQLException {
		
		String sql = "UPDATE " + TableData.Config.TABLE_NAME + "\n"
				+ "SET " + TableData.Config.PATH_TO_OSU_DB + " = ?,"
				+ TableData.Config.PATH_TO_SONGS_FOLDER + " = ?,"
				+ TableData.Config.SAVE_FOLDER + " = ?,"
				+ TableData.Config.IS_SONG_SOURCE_SHOWN + " = ?,"
				+ TableData.Config.IS_ARTIST_NAME_SHOWN + " = ?,"
				+ TableData.Config.IS_ARTIST_NAME_UNICODE_SHOWN + " = ?,"
				+ TableData.Config.IS_SONG_TITLE_SHOWN + " = ?,"
				+ TableData.Config.IS_SONG_TITLE_UNICODE_SHOWN + " = ?,"
				+ TableData.Config.IS_CREATOR_NAME_SHOWN + " = ?,"
				+ TableData.Config.IS_TOTAL_TIME_SHOWN + " = ?,"
				+ TableData.Config.IS_IS_DOWNLOADED_SHOWN + " = ?,"
				+ TableData.Config.ORDERING + " = ?,"
				+ TableData.Config.SOUND_VOLUME + " = ?,"
				+ TableData.Config.IS_REPEAT_TOGGLED + " = ?,"
				+ TableData.Config.IS_SHUFFLE_TOGGLED + " = ?,"
				+ TableData.Config.COMBO_BOX_PREFIX + " = ?,"
				+ TableData.Config.COMBO_BOX_SUFFIX + " = ? "
				+ "WHERE " + TableData.Config.CONFIG_ID + " = ?";
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
		pstmt.setString(16, comboBoxPrefix);
		pstmt.setString(17, comboBoxSuffix);
		pstmt.setInt(18, configID);
		pstmt.executeUpdate();
	}
	
	public void updateConfigString(int configID, String[] items, String[] results) throws SQLException {
		if (items.length != results.length) {
			throw new RuntimeException("Update config string argument num doesn't match");
		}
		
		String sql = "UPDATE " + TableData.Config.TABLE_NAME + "\n"
				+ "SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString();
		sql += " WHERE " + TableData.Config.CONFIG_ID + " = ?;";
		
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
		
		String sql = "UPDATE " + TableData.Config.TABLE_NAME + "\n"
				+ "SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString();
		sql += " WHERE " + TableData.Config.CONFIG_ID + " = ?;";
		
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
		String sql = "UPDATE " + TableData.Beatmap.TABLE_NAME + " SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString() + " WHERE " + TableData.Beatmap.BEATMAP_AUTO_ID + " = ?";
		return this.getConn().prepareStatement(sql);
	}
	
	private void addUpdateBeatmapLMTBatch(PreparedStatement updateBeatmapPStatement, int beatmapAutoID, long lastModificationTime) throws SQLException {
		updateBeatmapPStatement.setLong(1, lastModificationTime);
		updateBeatmapPStatement.setInt(2, beatmapAutoID);
		updateBeatmapPStatement.addBatch();
	}
	
	
	public PreparedStatement getUpdateBeatmapSetBooleanPStatement(String[] items) throws SQLException {
		String sql = "UPDATE " + TableData.BeatmapSet.TABLE_NAME + " SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString() + " WHERE " + TableData.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
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
	
	
	public class TableData {
		public class Metadata {
			public static final String TABLE_NAME = "Metadata";
			// ===== Metadata fields =====
			public static final String METADATA_ID = "MetadataID";
			public static final String OSU_VERSION = "OsuVersion";
			public static final String FOLDER_COUNT = "FolderCount";
			public static final String PLAYER_NAME = "PlayerName";
			public static final String NUMBER_OF_BEATMAPS = "NumberOfBeatmaps";
		}
		
		public class Config {
			public static final String TABLE_NAME = "Config";
			// ===== Config fields =====
			public static final String CONFIG_ID = "ConfigID";
			public static final String PATH_TO_OSU_DB = "PathToOsuDb";
			public static final String PATH_TO_SONGS_FOLDER = "PathToSongsFolder";
			public static final String SAVE_FOLDER = "SaveFolder";
			public static final String IS_SONG_SOURCE_SHOWN = "IsSongSourceShown";
			public static final String IS_ARTIST_NAME_SHOWN = "IsArtistNameShown";
			public static final String IS_ARTIST_NAME_UNICODE_SHOWN = "IsArtistNameUnicodeShown";
			public static final String IS_SONG_TITLE_SHOWN = "IsSongTitleShown";
			public static final String IS_SONG_TITLE_UNICODE_SHOWN = "IsSongTitleUnicodeShown";
			public static final String IS_CREATOR_NAME_SHOWN = "IsCreatorNameShown";
			public static final String IS_TOTAL_TIME_SHOWN = "IsTotalTimeShown";
			public static final String IS_IS_DOWNLOADED_SHOWN = "IsIsDownloadedShown";
			public static final String ORDERING = "Ordering";
			public static final String SOUND_VOLUME = "SoundVolume";
			public static final String IS_REPEAT_TOGGLED = "IsRepeatToggled";
			public static final String IS_SHUFFLE_TOGGLED = "IsShuffleToggled";
			public static final String COMBO_BOX_PREFIX = "ComboBoxPrefix";
			public static final String COMBO_BOX_SUFFIX = "ComboBoxSuffix";
		}
		
		public class Beatmap {
			public static final String TABLE_NAME = "Beatmap";
			// ===== Beatmap fields =====
			public static final String BEATMAP_AUTO_ID = "BeatmapAutoID";
			public static final String BEATMAP_ID = "BeatmapID";
			public static final String BEATMAP_SET_AUTO_ID = "BeatmapSetAutoID";
			public static final String RANKED_STATUS = "RankedStatus";
			public static final String LAST_MODIFICATION_TIME = "LastModificationTime";
			public static final String TOTAL_TIME = "TotalTime";
			public static final String PREVIEW_TIME = "PreviewTime";
			public static final String THREAD_ID = "ThreadID";
			public static final String DIFFICULTY = "Difficulty";
			public static final String IS_UNPLAYED = "IsUnplayed";
			public static final String LAST_PLAYED_TIME = "LastPlayedTime";
		}
		
		public class BeatmapSet {
			public static final String TABLE_NAME = "BeatmapSet";
			// ===== BeatmapSet fields =====
			public static final String BEATMAP_SET_AUTO_ID = "BeatmapSetAutoID";
			public static final String BEATMAP_SET_ID = "BeatmapSetID";
			public static final String ARTIST_ID = "ArtistID";
			public static final String SONG_ID = "SongID";
			public static final String CREATOR_NAME = "CreatorName";
			public static final String FOLDER_NAME = "FolderName"; 
			public static final String AUDIO_NAME = "AudioName"; 
			public static final String IS_DOWNLOADED = "IsDownloaded";
			public static final String IS_HIDDEN = "IsHidden";
		}
		
		public class Artist {
			public static final String TABLE_NAME = "Artist";
			// ===== Artist fields =====
			public static final String ARTIST_ID = "ArtistID";
			public static final String ARTIST_NAME = "ArtistName";
			public static final String ARTIST_NAME_UNICODE = "ArtistNameUnicode";
		}
		
		public class Song {
			public static final String TABLE_NAME = "Song";
			// ===== Song fields =====
			public static final String SONG_ID = "SongID";
			public static final String SONG_TITLE = "SongTitle";
			public static final String SONG_TITLE_UNICODE = "SongTitleUnicode";
			public static final String SONG_SOURCE = "SongSource";
		}
		
		public class SongTag {
			public static final String TABLE_NAME = "SongTag";
			// ===== SongTag fields =====
			public static final String SONG_TAG_ID = "SongTagID";
			public static final String SONG_TAG_NAME = "SongTagName";
		}
		
		public class BeatmapSet_SongTag {
			public static final String TABLE_NAME = "BeatmapSet_SongTag";
			// ===== BeatmapSet_SongTag fields =====
			public static final String BEATMAP_SET_AUTO_ID = "BeatmapSetAutoID";
			public static final String SONG_TAG_ID = "SongTagID";
		}
		
	}
}