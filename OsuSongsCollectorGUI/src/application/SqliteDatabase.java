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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.BiConsumer;

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
	private Object results;
	
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
				+ this.Data.Config.ORDERING + " TEXT"
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
				+ this.Data.Beatmap.GRADE_STANDARD + " INTEGER,"
				+ this.Data.Beatmap.GRADE_TAIKO + " INTEGER,"
				+ this.Data.Beatmap.GRADE_CTB + " INTEGER,"
				+ this.Data.Beatmap.GRADE_MANIA + " INTEGER,"
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
			boolean isTotalTimeShown, boolean isIsDownloadedShown, String ordering) throws SQLException {
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
				+ this.Data.Config.ORDERING
				+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
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
				+ this.Data.Beatmap.GRADE_STANDARD + ","
				+ this.Data.Beatmap.GRADE_TAIKO + ","
				+ this.Data.Beatmap.GRADE_CTB + ","
				+ this.Data.Beatmap.GRADE_MANIA + ","
				+ this.Data.Beatmap.IS_UNPLAYED + ","
				+ this.Data.Beatmap.LAST_PLAYED_TIME
				+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
			int gradeStandard, 
			int gradeTaiko, 
			int gradeCTB, 
			int gradeMania, 
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
		beatmapPStatement.setInt(8, gradeStandard);
		beatmapPStatement.setInt(9, gradeTaiko);
		beatmapPStatement.setInt(10, gradeCTB);
		beatmapPStatement.setInt(11, gradeMania);
		beatmapPStatement.setBoolean(12, isUnplayed);
		beatmapPStatement.setLong(13, lastPlayedTime);
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
	
	// TODO: optimize insert part for unranked maps
	public void insertAllData(OsuDbParser osuDb) throws SQLException, InterruptedException {
		this.insertIntoMetadata(osuDb.getOsuVersion(), osuDb.getFolderCount(), osuDb.getPlayerName(), osuDb.getNumberOfBeatmaps());
		this.insertIntoConfig(osuDb.getPathToOsuDb(), osuDb.getPathToSongsFolder(), "", false, false, false, false, false, false, false, false, "");
//		, osuDb.getPathToOsuDb(), osuDb.getPathToSongsFolder(), ""
		int batchSize = 400; // actually much bigger than this
		// 1st int indicates rankedStatus (1 ranked 0 not), 2nd indicates rankedIndex
		Map<String, List<Integer>> rankedMap = new TreeMap<String, List<Integer>>();
		// must check if ranked 1st in rankedMap b4 accessing this as unranked is not stored
		Map<String, String[]> splitSongTags = new TreeMap<String, String[]>();
		
		List<List<Beatmap>> songsFolder = osuDb.getSongsFolder();
		// get all the preparedStatements 1st for batch insert
		PreparedStatement artistPStatement = this.getInsertIntoArtistPStatement();
		PreparedStatement songPStatement = this.getInsertIntoSongPStatement();
		PreparedStatement songTagPStatement = this.getInsertIntoSongTagPStatement();
		this.getConn().setAutoCommit(false);
		
		// for progressBar in UI
		int totalProgress = songsFolder.size() * 4;
		int currentProgress = 0;
		
		// var for tracking size of batch
		int i = 0;
		
		for (List<Beatmap> beatmapSet : songsFolder) {
			if (Thread.currentThread().isInterrupted()) {
				artistPStatement.cancel();
				songPStatement.cancel();
				songTagPStatement.cancel();
				this.cancelThread();
				throw new InterruptedException("CreateDatabaseTask is interrupted");
			}
			
			boolean isRanked = false;
			int rankedIndex = 0;
			// search through the each beatmap in each beatmapSet and see if they are ranked
			// if yes, it's pretty safe to assume all metadata (artistName etc.) are the same
			// so can go out of loop and directly assign the data to corresponding beatmapSet
			for (int j = 0; j < beatmapSet.size(); j++) {
				if (beatmapSet.get(j).getRankedStatus() == 4) {
					isRanked = true;
					// store the index of the beatmap that is ranked. Most of the time this will be 0 but not in some case, so better 
					// use this as indication when assigning data to beatmapSet
					// !! some ranked maps still have beatmapSetID of -1... no fking idea why... but so far so good so just leave it
					rankedIndex = j;
					
					break;
				}
			}
			
			if (isRanked) {
				// all these are for later reference, not now
				List<Integer> x = new ArrayList<Integer>();
				x.add(1); // 1 means ranked
				x.add(rankedIndex); 
				rankedMap.put(beatmapSet.get(rankedIndex).getFolderName(), x);
				String[] songTagNames = beatmapSet.get(rankedIndex).getSongTags().split("\\s+");
				splitSongTags.put(beatmapSet.get(rankedIndex).getFolderName(), songTagNames);
				
				
				// actual storing of data to non-foreign tables
				for (String songTagName : songTagNames) {
					this.insertIntoSongTagBatch(songTagPStatement, songTagName);
				}
				this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(rankedIndex).getArtistName(), beatmapSet.get(rankedIndex).getArtistNameUnicode());
				this.insertIntoSongBatch(songPStatement, beatmapSet.get(rankedIndex).getSongTitle(), beatmapSet.get(rankedIndex).getSongTitleUnicode(), beatmapSet.get(rankedIndex).getSongSource());
			}
			else {
				// same thing as above, but indicates as unranked
				List<Integer> x = new ArrayList<Integer>();
				x.add(0);
				rankedMap.put(beatmapSet.get(0).getFolderName(), x);
				
				// as it's unranked, it's safer to loop through each beatmap in the beatmapSet
				// as the metadata may scatter among the beatmaps
				for (int j = 0; j < beatmapSet.size(); j++) {
					String[] songTagNames = beatmapSet.get(j).getSongTags().split("\\s+");
					for (String songTagName : songTagNames) {
						this.insertIntoSongTagBatch(songTagPStatement, songTagName);
					}
					this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(j).getArtistName(), beatmapSet.get(j).getArtistNameUnicode());
					this.insertIntoSongBatch(songPStatement, beatmapSet.get(j).getSongTitle(), beatmapSet.get(j).getSongTitleUnicode(), beatmapSet.get(j).getSongSource());
				}
			}
			
			i++;
			// start to insert when batch size is considerable
			if (i % batchSize == 0 || i == songsFolder.size()) {
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
		i = 0;
		for (List<Beatmap> beatmapSet : songsFolder) {
			
			if (Thread.currentThread().isInterrupted()) {
				beatmapSetPStatement.cancel();
				this.cancelThread();
				throw new InterruptedException("CreateDatabaseTask is interrupted");
			}
			
			// get the cache values from previous loop
			List<Integer> value =  rankedMap.get(beatmapSet.get(0).getFolderName());
			boolean isRanked = value.get(0) == 1 ? true : false;
			// if ranked, safe to get data from one and insert
			if (isRanked) {
				Beatmap beatmap = beatmapSet.get(value.get(1));
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
			// if not, better loop through the unranked beatmaps to collect the data
			else {
				for (Beatmap beatmap : beatmapSet) {
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
			}
			i++;
			if (i % batchSize == 0 || i == songsFolder.size()) {
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
		i = 0;
		for (List<Beatmap> beatmapSet : songsFolder) {
			
			if (Thread.currentThread().isInterrupted()) {
				beatmapPStatement.cancel();
				beatmapSet_SongTagPStatement.cancel();
				this.cancelThread();
				throw new InterruptedException("CreateDatabaseTask is interrupted");
			}
			
			List<Integer> value =  rankedMap.get(beatmapSet.get(0).getFolderName());
			boolean isRanked = value.get(0) == 1 ? true : false;
			if (isRanked) {
				ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmapSet.get(value.get(1)).getFolderName(), beatmapSet.get(value.get(1)).getAudioFileName());
				int beatmapSetAutoID;
				if (beatmapSetAutoIDRs.next()) {
					beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
				}
				else {
					throw new SQLException("Failed to retrieve newly inserted data");
				}
				String[] songTagNames = splitSongTags.get(beatmapSet.get(value.get(1)).getFolderName());
				ResultSet rs = this.selectSongTagIDFromSongTag(songTagNames);
				while (rs.next()) {
					this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
				}
				
				for (Beatmap beatmap : beatmapSet) {
					this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
				}
			}
			else {
				for (Beatmap beatmap : beatmapSet) {
					ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmap.getFolderName(), beatmap.getAudioFileName());
					int beatmapSetAutoID;
					if (beatmapSetAutoIDRs.next()) {
						beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
					}
					else {
						throw new SQLException("Failed to retrieve newly inserted data");
					}
					ResultSet rs = this.selectSongTagIDFromSongTag(beatmap.getSongTags().split("\\s+"));
					while (rs.next()) {
						this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
					}
					this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
				}
			}
			i++;
			if (i % batchSize == 0 || i == songsFolder.size()) {
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
		this.getConn().setAutoCommit(true);
		// if threading, set back to null to prevent inadvertent access later
		if (this.progressUpdate != null) {
			this.setProgressUpdate(null);
		}
	}
	
	
	// TODO: add exception when pathToosuDb is no longer true so that the welcome message wont show forever
	public void updateData(OsuDbParser osuDb) throws SQLException, InterruptedException, Exception {
		// TODO: move this till the end? or prefer to delete the database at all if is abruptly closed
//		ResultSet metadataRs = this.selectMetadata();
//		if (metadataRs.next()) {
//			int metadataID = metadataRs.getInt(this.Data.Metadata.METADATA_ID);
//			this.updateMetadata(metadataID, osuDb.getOsuVersion(), osuDb.getFolderCount(), osuDb.getPlayerName(), osuDb.getNumberOfBeatmaps());
//		}
//		else {
//			throw new SQLException("Metadata does not exist");
//		}
		// only the key is useful
		Map<Integer, Integer> dbRecords = new TreeMap<Integer, Integer>();
		
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
		// store the rankedIndex of each corresponding element in updateList. -1 if unranked
		List<Integer> updateRankedList = new ArrayList<Integer>();
		List<List<Beatmap>> modifiedList = new ArrayList<List<Beatmap>>();
		// 1st int stores the status: 1 means beatmaps are added, 0 means deleted
		// 2nd int stores the beatmapSetAutoID of that beatmapSet
		List<Integer[]> modifiedStatusAndIDList = new ArrayList<Integer[]>();
		
		// for unranked only (both should be of same size) (should be reused)
		List<String> audioNameList = new ArrayList<String>();
		List<List<Beatmap>> atomizedBeatmapSet = new ArrayList<List<Beatmap>>();
		
		List<List<Beatmap>> songsFolder = osuDb.getSongsFolder();
		
		
		System.out.println("Start checking for updates");
		
		
		for (List<Beatmap> beatmapSet : songsFolder) {
			// for unranked only
			// initialize to first audioName
			String audioName = beatmapSet.get(0).getAudioFileName();
			int subListFromIndex = 0;
			
			boolean isRanked = false;
			int rankedIndex = 0;
			for (int j = 0; j < beatmapSet.size(); j++) {
				if (beatmapSet.get(j).getRankedStatus() == 4) {
					isRanked = true;
					rankedIndex = j;
					break;
				}
				
				// only for unranked situation (very rare)
				if (!audioName.equals(beatmapSet.get(j).getAudioFileName())) {
					audioNameList.add(audioName);
					audioName = beatmapSet.get(j).getAudioFileName();
					// add the grouped beatmapSet into list
					atomizedBeatmapSet.add(beatmapSet.subList(subListFromIndex, j));
					subListFromIndex = j;
				}
				// account for last audio in beatmapSet
				if (j == beatmapSet.size() - 1 && !audioNameList.isEmpty() && !audioName.equals(audioNameList.get(audioNameList.size() - 1))) {
					audioNameList.add(audioName);
					atomizedBeatmapSet.add(beatmapSet.subList(subListFromIndex, j + 1));
				}
			}
			// if ranked and the query returns records, most probably the beatmaps inside beatmapSet 
			// has not been changed, so select only count for the first time and select second time
			// if it is really changed
			// even if it's unranked, if there's no multiple audioFiles in the beatmapSet, it's safe to treat it as if it's ranked
			// so it's also put here (for checking update only)
			if (isRanked || audioNameList.isEmpty()) {
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
//				// TODO: seems like the thing after if part is redundant. try to modify that
//				String folderName = beatmapSet.get(0).getFolderName();
//				// 99% of the time
//				if (audioNameList.isEmpty()) {
//					// no multiple audioFiles, means that the folderName and audioName from 1st element is same as the others
//					ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(folderName, audioName);
//					if (beatmapSetAutoIDRs.next()) {
//						int beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
//						if (dbRecords.remove(beatmapSetAutoID) == null) {
//							throw new RuntimeException("Cannot find key in map");
//						}
//						beatmapCountUsingBeatmapSetAutoIDPStatement.setInt(1, beatmapSetAutoID);
//						ResultSet countRs = beatmapCountUsingBeatmapSetAutoIDPStatement.executeQuery();
//						if (countRs.next()) {
//							int count = countRs.getInt(1);
//							if (count == beatmapSet.size()) {
//								continue;
//							}
//							else {
//								modifiedList.add(beatmapSet);
//							}
//						}
//						else {
//							throw new SQLException("BeatmapSet is found but with no beatmap");
//						}
//						
//					}
//					// new beatmapSet
//					else {
//						updateList.add(beatmapSet);
//					}
//				}
//				else {
				// very rare case: not ranked and multi-audio
				String folderName = beatmapSet.get(0).getFolderName();
				if (atomizedBeatmapSet.size() != audioNameList.size()) {
					throw new RuntimeException("Something wrong with unranked, multi-audio beatmapSet logic");
				}
				for (int j = 0; j < audioNameList.size(); j++) {
					String currentAudioName = audioNameList.get(j);
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
							if (count == atomizedBeatmapSet.get(j).size()) {
								continue;
							}
							else {
								modifiedList.add(atomizedBeatmapSet.get(j));
								modifiedStatusAndIDList.add(new Integer[] {count < atomizedBeatmapSet.get(j).size() ? 1 : 0, beatmapSetAutoID});
							}
						}
						else {
							throw new SQLException("BeatmapSet is found but with no beatmap");
						}
						
					}
					else {
						updateList.add(atomizedBeatmapSet.get(j));
						updateRankedList.add(-1);
					}
				}
				// reuse the list
				atomizedBeatmapSet.clear();
				audioNameList.clear();
//				}
			}
		}
		
		if (updateList.size() != updateRankedList.size() || modifiedList.size() != modifiedStatusAndIDList.size()) {
			throw new RuntimeException("Logic error in storing states of updateList and modifiedList");
		}
		
		System.out.println("Finish");
		System.out.println("Update list: " + updateList.size());
		System.out.println("Modified list: " + modifiedList.size());
		System.out.println("Deleted: " + dbRecords.size());
		System.out.println("Start updating db");
		// TODO: account for thread forced exit
		// delete
		String deleteFromBeatmapSetSql = "DELETE FROM " + this.Data.BeatmapSet.TABLE_NAME + " WHERE " 
				+ this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " = ?";
		PreparedStatement deleteFromBeatmapSetPStatement = this.getConn().prepareStatement(deleteFromBeatmapSetSql);
		
		for (int beatmapSetAutoID : dbRecords.keySet()) {
			deleteFromBeatmapSetPStatement.setInt(1, beatmapSetAutoID);
			deleteFromBeatmapSetPStatement.executeUpdate();
		}
		
		
		// insert new songs
		int batchSize = 400;
		// TODO: setAutoCommit to false and use try to make sure it changes back
		this.getConn().setAutoCommit(false);
		PreparedStatement artistPStatement = this.getInsertIntoArtistPStatement();
		PreparedStatement songPStatement = this.getInsertIntoSongPStatement();
		PreparedStatement songTagPStatement = this.getInsertIntoSongTagPStatement();
		
		for (int i = 0; i < updateList.size(); i++) {
			List<Beatmap> beatmapSet = updateList.get(i);
			int rankedIndex = updateRankedList.get(i);
			// if ranked
			if (rankedIndex != -1) {
				String[] songTagNames = beatmapSet.get(rankedIndex).getSongTags().split("\\s+");
				for (String songTagName : songTagNames) {
					this.insertIntoSongTagBatch(songTagPStatement, songTagName);
				}
				this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(rankedIndex).getArtistName(), beatmapSet.get(rankedIndex).getArtistNameUnicode());
				this.insertIntoSongBatch(songPStatement, beatmapSet.get(rankedIndex).getSongTitle(), beatmapSet.get(rankedIndex).getSongTitleUnicode(), beatmapSet.get(rankedIndex).getSongSource());
			}
			else {
				for (int j = 0; j < beatmapSet.size(); j++) {
					String[] songTagNames = beatmapSet.get(j).getSongTags().split("\\s+");
					for (String songTagName : songTagNames) {
						this.insertIntoSongTagBatch(songTagPStatement, songTagName);
					}
					this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(j).getArtistName(), beatmapSet.get(j).getArtistNameUnicode());
					this.insertIntoSongBatch(songPStatement, beatmapSet.get(j).getSongTitle(), beatmapSet.get(j).getSongTitleUnicode(), beatmapSet.get(j).getSongSource());
				}
			}
			
			// start to insert when batch size is considerable
			if (i + 1 % batchSize == 0 || i + 1 == updateList.size()) {
				songTagPStatement.executeBatch(); // much more than batch size but not gonna be a problem
				artistPStatement.executeBatch();
				songPStatement.executeBatch();
				this.getConn().commit();
			}
		}
		
		PreparedStatement beatmapSetPStatement = this.getInsertIntoBeatmapSetPStatement();
		for (int i = 0; i < updateList.size(); i++) {
			List<Beatmap> beatmapSet = updateList.get(i);
			int rankedIndex = updateRankedList.get(i);
			if (rankedIndex != -1) {
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
			// if not, better loop through the unranked beatmaps to collect the data
			else {
				for (Beatmap beatmap : beatmapSet) {
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
			} 
			
			if (i + 1 % batchSize == 0 || i + 1 == updateList.size()) {
				beatmapSetPStatement.executeBatch();
				this.getConn().commit();
			}
		}
		
		PreparedStatement beatmapPStatement = this.getInsertIntoBeatmapPStatement();
		PreparedStatement beatmapSet_SongTagPStatement = this.getInsertIntoBeatmapSet_SongTagPStatement();
		for (int i = 0; i < updateList.size(); i++) {
			List<Beatmap> beatmapSet = updateList.get(i);
			int rankedIndex = updateRankedList.get(i);
			if (rankedIndex != -1) {
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
				for (Beatmap beatmap : beatmapSet) {
					ResultSet beatmapSetAutoIDRs = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmap.getFolderName(), beatmap.getAudioFileName());
					int beatmapSetAutoID;
					if (beatmapSetAutoIDRs.next()) {
						beatmapSetAutoID = beatmapSetAutoIDRs.getInt(1);
					}
					else {
						throw new SQLException("Failed to retrieve newly inserted data");
					}		
					ResultSet rs = this.selectSongTagIDFromSongTag(beatmap.getSongTags().split("\\s+"));
					while (rs.next()) {
						this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
						
					}
					this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
				}
			}
			
			if (i + 1 % batchSize == 0 || i + 1 == updateList.size()) {
				beatmapPStatement.executeBatch();
				beatmapSet_SongTagPStatement.executeBatch();
				this.getConn().commit();
			}
		}
		
		this.getConn().setAutoCommit(true);
		System.out.println("finish updating db");
//		int batchSize = 500; // actually much bigger than this
//		// 1st int indicates rankedStatus (1 ranked 0 not), 2nd indicates rankedIndex
//		Map<String, List<Integer>> rankedMap = new TreeMap<String, List<Integer>>();
//		// must check if ranked 1st in rankedMap b4 accessing this as unranked is not stored
//		Map<String, String[]> splitSongTags = new TreeMap<String, String[]>();
//		
//		List<List<Beatmap>> songsFolder = osuDb.getSongsFolder();
//		// get all the preparedStatements 1st for batch insert
//		PreparedStatement artistPStatement = this.getInsertIntoArtistPStatement();
//		PreparedStatement songPStatement = this.getInsertIntoSongPStatement();
//		PreparedStatement songTagPStatement = this.getInsertIntoSongTagPStatement();
//		this.getConn().setAutoCommit(false);
//		
//		// for progressBar in UI
//		int totalProgress = songsFolder.size() * 4;
//		int currentProgress = 0;
//		
//		// var for tracking size of batch
//		int i = 0;
//		
//		for (List<Beatmap> beatmapSet : songsFolder) {
//			if (Thread.currentThread().isInterrupted()) {
//				artistPStatement.cancel();
//				songPStatement.cancel();
//				songTagPStatement.cancel();
//				this.cancelThread();
//				throw new InterruptedException("CreateDatabaseTask is interrupted");
//			}
//			
//			boolean isRanked = false;
//			int rankedIndex = 0;
//			// search through the each beatmap in each beatmapSet and see if they are ranked
//			// if yes, it's pretty safe to assume all metadata (artistName etc.) are the same
//			// so can go out of loop and directly assign the data to corresponding beatmapSet
//			for (int j = 0; j < beatmapSet.size() && !isRanked; j++) {
//				if (beatmapSet.get(j).getRankedStatus() == 4) {
//					isRanked = true;
//					// store the index of the beatmap that is ranked. Most of the time this will be 0 but not in some case, so better 
//					// use this as indication when assigning data to beatmapSet
//					rankedIndex = j;
//					break;
//				}
//			}
//			
//			if (isRanked) {
//				// all these are for later reference, not now
//				List<Integer> x = new ArrayList<Integer>();
//				x.add(1); // 1 means ranked
//				x.add(rankedIndex); 
//				rankedMap.put(beatmapSet.get(rankedIndex).getFolderName(), x);
//				String[] songTagNames = beatmapSet.get(rankedIndex).getSongTags().split("\\s+");
//				splitSongTags.put(beatmapSet.get(rankedIndex).getFolderName(), songTagNames);
//				
//				
//				// actual storing of data to non-foreign tables
//				for (String songTagName : songTagNames) {
//					this.insertIntoSongTagBatch(songTagPStatement, songTagName);
//				}
//				this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(rankedIndex).getArtistName(), beatmapSet.get(rankedIndex).getArtistNameUnicode());
//				this.insertIntoSongBatch(songPStatement, beatmapSet.get(rankedIndex).getSongTitle(), beatmapSet.get(rankedIndex).getSongTitleUnicode(), beatmapSet.get(rankedIndex).getSongSource());
//			}
//			else {
//				// same thing as above, but indicates as unranked
//				List<Integer> x = new ArrayList<Integer>();
//				x.add(0);
//				rankedMap.put(beatmapSet.get(0).getFolderName(), x);
//				
//				// as it's unranked, it's safer to loop through each beatmap in the beatmapSet
//				// as the metadata may scatter among the beatmaps
//				for (int j = 0; j < beatmapSet.size(); j++) {
//					String[] songTagNames = beatmapSet.get(j).getSongTags().split("\\s+");
//					for (String songTagName : songTagNames) {
//						this.insertIntoSongTagBatch(songTagPStatement, songTagName);
//					}
//					this.insertIntoArtistBatch(artistPStatement, beatmapSet.get(j).getArtistName(), beatmapSet.get(j).getArtistNameUnicode());
//					this.insertIntoSongBatch(songPStatement, beatmapSet.get(j).getSongTitle(), beatmapSet.get(j).getSongTitleUnicode(), beatmapSet.get(j).getSongSource());
//				}
//			}
//			
//			i++;
//			// start to insert when batch size is considerable
//			if (i % batchSize == 0 || i == songsFolder.size()) {
//				songTagPStatement.executeBatch(); // much more than batch size but not gonna be a problem
//				artistPStatement.executeBatch();
//				songPStatement.executeBatch();
//				this.getConn().commit();
//			}
//			
//			if (this.progressUpdate != null) {
//				currentProgress++;
//				progressUpdate.accept(currentProgress, totalProgress);
//			}
//		}
//	
//		
//		// now insert beatmapSet which requires data from tables inserted above
//		PreparedStatement beatmapSetPStatement = this.getInsertIntoBeatmapSetPStatement();
//		i = 0;
//		for (List<Beatmap> beatmapSet : songsFolder) {
//			
//			if (Thread.currentThread().isInterrupted()) {
//				beatmapSetPStatement.cancel();
//				this.cancelThread();
//				throw new InterruptedException("CreateDatabaseTask is interrupted");
//			}
//			
//			// get the cache values from previous loop
//			List<Integer> value =  rankedMap.get(beatmapSet.get(0).getFolderName());
//			boolean isRanked = value.get(0) == 1 ? true : false;
//			// if ranked, safe to get data from one and insert
//			if (isRanked) {
//				Beatmap beatmap = beatmapSet.get(value.get(1));
//				int artistID = this.selectArtistIDFromArtist(beatmap.getArtistName(), beatmap.getArtistNameUnicode());
//				int songID = this.selectSongIDFromSong(beatmap.getSongTitle(), beatmap.getSongTitleUnicode(), beatmap.getSongSource());
//				this.insertIntoBeatmapSetBatch(beatmapSetPStatement, beatmap.getBeatmapSetID(), artistID, songID, beatmap.getCreatorName(), beatmap.getFolderName(), beatmap.getAudioFileName(), false, false);
//				
//			}
//			// if not, better loop through the unranked beatmaps to collect the data
//			else {
//				for (Beatmap beatmap : beatmapSet) {
//					int artistID = this.selectArtistIDFromArtist(beatmap.getArtistName(), beatmap.getArtistNameUnicode());
//					int songID = this.selectSongIDFromSong(beatmap.getSongTitle(), beatmap.getSongTitleUnicode(), beatmap.getSongSource());
//					this.insertIntoBeatmapSetBatch(beatmapSetPStatement, beatmap.getBeatmapSetID(), artistID, songID, beatmap.getCreatorName(), beatmap.getFolderName(), beatmap.getAudioFileName(), false, false);
//				}
//			}
//			i++;
//			if (i % batchSize == 0 || i == songsFolder.size()) {
//				beatmapSetPStatement.executeBatch();
//				this.getConn().commit();
//			}
//			
//			if (this.progressUpdate != null) {
//				currentProgress++;
//				progressUpdate.accept(currentProgress, totalProgress);
//			}
//		}
//		
//		// lastly, insert the normalized table and beatmap table which depends on beatmapSet table just inserted
//		PreparedStatement beatmapPStatement = this.getInsertIntoBeatmapPStatement();
//		PreparedStatement beatmapSet_SongTagPStatement = this.getInsertIntoBeatmapSet_SongTagPStatement();
//		i = 0;
//		for (List<Beatmap> beatmapSet : songsFolder) {
//			
//			if (Thread.currentThread().isInterrupted()) {
//				beatmapPStatement.cancel();
//				beatmapSet_SongTagPStatement.cancel();
//				this.cancelThread();
//				throw new InterruptedException("CreateDatabaseTask is interrupted");
//			}
//			
//			List<Integer> value =  rankedMap.get(beatmapSet.get(0).getFolderName());
//			boolean isRanked = value.get(0) == 1 ? true : false;
//			if (isRanked) {
//				int beatmapSetAutoID = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmapSet.get(value.get(1)).getFolderName(), beatmapSet.get(value.get(1)).getAudioFileName());
//				String[] songTagNames = splitSongTags.get(beatmapSet.get(value.get(1)).getFolderName());
//				ResultSet rs = this.selectSongTagIDFromSongTag(songTagNames);
//				while (rs.next()) {
//					this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
//				}
//				
//				for (Beatmap beatmap : beatmapSet) {
//					this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
//				}
//			}
//			else {
//				for (Beatmap beatmap : beatmapSet) {
//					int beatmapSetAutoID = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmap.getFolderName(), beatmap.getAudioFileName());
//					ResultSet rs = this.selectSongTagIDFromSongTag(beatmap.getSongTags().split("\\s+"));
//					while (rs.next()) {
//						this.insertIntoBeatmapSet_SongTagBatch(beatmapSet_SongTagPStatement, beatmapSetAutoID, rs.getInt(1));
//						
//					}
//					this.insertIntoBeatmapBatchWrapper(beatmapPStatement, beatmap, beatmapSetAutoID);
//				}
//			}
//			i++;
//			if (i % batchSize == 0 || i == songsFolder.size()) {
//				beatmapPStatement.executeBatch();
//				beatmapSet_SongTagPStatement.executeBatch();
//				this.getConn().commit();
//			}
//			
//			if (this.progressUpdate != null) {
//				currentProgress += 2;
//				progressUpdate.accept(currentProgress, totalProgress);
//			}
//		}
//		// change back autoCommit
//		this.getConn().setAutoCommit(true);
//		// if threading, set back to null to prevent inadvertent access later
//		if (this.progressUpdate != null) {
//			this.setProgressUpdate(null);
//		}
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
				beatmap.getGradeStandard(),
				beatmap.getGradeTaiko(),
				beatmap.getGradeCTB(),
				beatmap.getGradeMania(),
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
	
//	// only for ranked maps
//	private ResultSet selectBeatmapSetAutoIDFromBeatmapSet(int beatmapSetID) throws SQLException {
//		String sql = "SELECT " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " FROM " + this.Data.BeatmapSet.TABLE_NAME + " "
//				+ "WHERE " + this.Data.BeatmapSet.BEATMAP_SET_ID + " = ?";
//		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
//		pstmt.setInt(1, beatmapSetID);
//		return pstmt.executeQuery();
//	}
	
	
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
	
//	public ResultSet searchAll(String[] items, String[] searchedStrings, String[] orderBy) throws SQLException {
//		// if searchedStrings == 1, no need having count
//		
//		String sql = "SELECT " + String.join(",", items) + "\n"
//				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + " bs\n"
//				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " b ON b." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = bs." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " a ON a." + this.Data.Artist.ARTIST_ID + " = bs." + this.Data.BeatmapSet.ARTIST_ID + "\n"
//				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " s ON s." + this.Data.Song.SONG_ID + " = bs." + this.Data.BeatmapSet.SONG_ID + "\n"
//				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " bst ON bst." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = bs." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " st ON st." + this.Data.SongTag.SONG_TAG_ID + " = bst." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
//				+ "WHERE ";
//		
//		String[] searchedFields = {this.Data.SongTag.SONG_TAG_NAME, this.Data.Artist.ARTIST_NAME, this.Data.Artist.ARTIST_NAME_UNICODE
//				, this.Data.Song.SONG_TITLE, this.Data.Song.SONG_TITLE_UNICODE, this.Data.BeatmapSet.CREATOR_NAME
//				, this.Data.Song.SONG_SOURCE};
//		
//		List<String[]> allConditions = new ArrayList<String[]>();
//		
//		for (int i = 0; i < searchedFields.length; i++) {
//			String[] conditions = new String[searchedStrings.length];
//			for (int j = 0; j < searchedStrings.length; j++) {
//				conditions[j] = searchedFields[i] + " COLLATE NOCASE LIKE ? ";
//			}
//			allConditions.add(conditions);
//		}
//		
//		StringBuilder sb = new StringBuilder();
//		
//		for (int i = 0; i < allConditions.size(); i++) {
//			sb.append(String.join(" OR ", allConditions.get(i)));
//			if (i != allConditions.size() - 1) {
//				sb.append(" OR ");
//			}
//		}
//		sb.append(" GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME);
//		
//		if (searchedStrings.length == 1) {
//			sb.append(" HAVING COUNT(*) >= 1 ");
//		}
//		else {
//			sb.append(" HAVING COUNT(CASE WHEN "); 
//			for (int i = 0; i < searchedStrings.length; i++) { 
//				for (int j = 0; j < allConditions.size(); j++) {
//					sb.append(allConditions.get(j)[i]);
//					if (j != allConditions.size() - 1) {
//						sb.append(" OR ");
//					}
//				}
//				sb.append(" THEN 1 END) >= 1 ");
//				if (i != searchedStrings.length - 1) {
//					sb.append(" AND COUNT(CASE WHEN ");
//				}
//			}
//		}
//		sb.append(" AND MAX(" + this.Data.Beatmap.TOTAL_TIME + ") ");
//		// TODO: Collate nocase should not be applied to non-string fields
//		sb.append(" ORDER BY ");
//		for (int i = 0; i < orderBy.length; i++) {
//			sb.append(orderBy[i] + " COLLATE NOCASE ");
//			if (i != orderBy.length - 1) {
//				sb.append(",");
//			}
//		}
//		sql += sb.toString();
//		
//		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
//		int index = 1; 
//		
//		for (int i = 0; i < searchedFields.length; i++) {
//			for (int j = 0; j < searchedStrings.length; j++) {
//				pstmt.setString(index, "%" + searchedStrings[j] + "%");
//				index++;
//			}
//		}
//		
//		if (searchedStrings.length != 1) {
//			for (int i = 0; i < searchedStrings.length; i++) {
//				for (int j = 0; j < searchedFields.length; j++) {
//					pstmt.setString(index, "%" + searchedStrings[i] + "%");
//					index++;
//				}
//			}
//		}
//	
//		return pstmt.executeQuery();
//	}
	
	public ResultSet getTableInitData() throws SQLException {
		String sql = "SELECT " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "," + this.Data.Song.SONG_SOURCE + "," + this.Data.Artist.ARTIST_NAME + "," + this.Data.Artist.ARTIST_NAME_UNICODE + "," + this.Data.Song.SONG_TITLE + "," + this.Data.Song.SONG_TITLE_UNICODE + "," 
				+ this.Data.Beatmap.TOTAL_TIME + "," + this.Data.Beatmap.LAST_MODIFICATION_TIME + "," + this.Data.BeatmapSet.IS_DOWNLOADED + "," + this.Data.BeatmapSet.IS_HIDDEN + "," + this.Data.BeatmapSet.FOLDER_NAME + "," 
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
				+ "HAVING MAX(" + this.Data.Beatmap.TOTAL_TIME + ")\n"
				+ "ORDER BY " + this.Data.Beatmap.LAST_MODIFICATION_TIME;
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
		
	}
	
//	public ResultSet getHiddenTableInitData() throws SQLException {
//		String sql = "SELECT " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "," + this.Data.Song.SONG_SOURCE + "," + this.Data.Artist.ARTIST_NAME + "," + this.Data.Artist.ARTIST_NAME_UNICODE + "," + this.Data.Song.SONG_TITLE + "," + this.Data.Song.SONG_TITLE_UNICODE + "," 
//				+ this.Data.Beatmap.TOTAL_TIME + "," + this.Data.Beatmap.LAST_MODIFICATION_TIME + "," + this.Data.BeatmapSet.IS_DOWNLOADED + "," + this.Data.BeatmapSet.IS_HIDDEN + "," + this.Data.BeatmapSet.FOLDER_NAME + "," 
//				+ this.Data.BeatmapSet.AUDIO_NAME + ",group_concat(DISTINCT " + this.Data.SongTag.SONG_TAG_NAME + ") AS " + this.Data.SongTag.SONG_TAG_NAME + "," + this.Data.BeatmapSet.CREATOR_NAME + "\n"
//				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + "\n"
//				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " ON " + this.Data.Beatmap.TABLE_NAME + "." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " ON " + this.Data.Artist.TABLE_NAME + "." + this.Data.Artist.ARTIST_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.ARTIST_ID + "\n"
//				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " ON " + this.Data.Song.TABLE_NAME + "." + this.Data.Song.SONG_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.SONG_ID + "\n"
//				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " ON " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " ON " + this.Data.SongTag.TABLE_NAME + "." + this.Data.SongTag.SONG_TAG_ID + " = " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
//				+ "WHERE " + this.Data.BeatmapSet.IS_HIDDEN + " = 1\n"
//				+ "GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME + "\n"
//				+ "HAVING MAX(" + this.Data.Beatmap.TOTAL_TIME + ")\n"
//				+ "ORDER BY " + this.Data.Beatmap.LAST_MODIFICATION_TIME;
//		Statement stmt = this.getConn().createStatement();
//		return stmt.executeQuery(sql);
//	}
	
//	public ResultSet getDownloadedTableInitData() throws SQLException {
//		String sql = "SELECT " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "," + this.Data.Song.SONG_SOURCE + "," + this.Data.Artist.ARTIST_NAME + "," + this.Data.Artist.ARTIST_NAME_UNICODE + "," + this.Data.Song.SONG_TITLE + "," + this.Data.Song.SONG_TITLE_UNICODE + "," 
//				+ this.Data.Beatmap.TOTAL_TIME + "," + this.Data.Beatmap.LAST_MODIFICATION_TIME + "," + this.Data.BeatmapSet.IS_DOWNLOADED + "," + this.Data.BeatmapSet.IS_HIDDEN + "," + this.Data.BeatmapSet.FOLDER_NAME + "," 
//				+ this.Data.BeatmapSet.AUDIO_NAME + ",group_concat(DISTINCT " + this.Data.SongTag.SONG_TAG_NAME + ") AS " + this.Data.SongTag.SONG_TAG_NAME + "," + this.Data.BeatmapSet.CREATOR_NAME + "\n"
//				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + "\n"
//				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " ON " + this.Data.Beatmap.TABLE_NAME + "." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " ON " + this.Data.Artist.TABLE_NAME + "." + this.Data.Artist.ARTIST_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.ARTIST_ID + "\n"
//				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " ON " + this.Data.Song.TABLE_NAME + "." + this.Data.Song.SONG_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.SONG_ID + "\n"
//				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " ON " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = " + this.Data.BeatmapSet.TABLE_NAME + "." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
//				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " ON " + this.Data.SongTag.TABLE_NAME + "." + this.Data.SongTag.SONG_TAG_ID + " = " + this.Data.BeatmapSet_SongTag.TABLE_NAME + "." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
//				+ "WHERE " + this.Data.BeatmapSet.IS_DOWNLOADED + " = 1\n"
//				+ "GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME + "\n"
//				+ "HAVING MAX(" + this.Data.Beatmap.TOTAL_TIME + ")\n"
//				+ "ORDER BY " + this.Data.Beatmap.LAST_MODIFICATION_TIME;
//		Statement stmt = this.getConn().createStatement();
//		return stmt.executeQuery(sql);
//	}
	
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
//		if (items.length != results.length) {
//			throw new Exception("Update argument num doesn't match");
//		}
//		
//		String sql = "UPDATE " + this.Data.Metadata.TABLE_NAME + "\n"
//				+ "SET ";
//		StringJoiner sj = new StringJoiner(",");
//		for (int i = 0; i < items.length; i++) {
//			sj.add(items[i] + " = ?");
//		}
//		sql += sj.toString();
//		sql += " WHERE " + this.Data.Metadata.METADATA_ID + " = ?;";
//		
//		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
//		int index = 1;
//		for (int i = 0; i < results.length; i++) {
//			pstmt.setString(index, results[i]);
//			index++;
//		}
//		pstmt.setInt(index, metadataID);
//		pstmt.executeUpdate();
	}
	
	public void updateConfigFull(int configID, String pathToOsuDb, String pathToSongsFolder, String saveFolder,
			boolean isSongSourceShown, boolean isArtistNameShown, boolean isArtistNameUnicodeShown,
			boolean isSongTitleShown, boolean isSongTitleUnicodeShown, boolean isCreatorNameShown,
			boolean isTotalTimeShown, boolean isIsDownloadedShown, String ordering) throws Exception, SQLException {
		
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
				+ this.Data.Config.ORDERING + " = ? "
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
		pstmt.setInt(13, configID);
		pstmt.executeUpdate();
	}
	
	public void updateConfigString(int configID, String[] items, String[] results) throws SQLException, Exception {
		if (items.length != results.length) {
			throw new Exception("Update argument num doesn't match");
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
	
	public void updateConfigBoolean(int configID, String[] items, Boolean[] results) throws SQLException, Exception {
		if (items.length != results.length) {
			throw new Exception("Update argument num doesn't match");
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
	
	// TODO: make all these statements to be safe by providing checks
	public PreparedStatement getUpdateBeatmapSetBooleanPreparedStatement(String[] items) throws SQLException {
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
	
	// for threading only (TODO: change name to reflect database creation thread)
	public void cancelThread() throws SQLException {
		this.closeConnection();
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
	
	
	
	public class SongsDbData {
		public Metadata Metadata = new Metadata();
		public Config Config = new Config();
		public Beatmap Beatmap = new Beatmap();
		public BeatmapSet BeatmapSet = new BeatmapSet();
		public Artist Artist = new Artist();
		public Song Song = new Song();
		public SongTag SongTag = new SongTag();
		public BeatmapSet_SongTag BeatmapSet_SongTag = new BeatmapSet_SongTag();
//		Beatmap_SongTag Beatmap_SongTag = new Beatmap_SongTag();
		
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
			public final String GRADE_STANDARD = "GradeStandard";
			public final String GRADE_TAIKO = "GradeTaiko";
			public final String GRADE_CTB = "GradeCTB";
			public final String GRADE_MANIA = "GradeMania";
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