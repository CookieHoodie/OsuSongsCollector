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
				+ this.Data.Metadata.NUMBER_OF_BEATMAPS + " INTEGER,"
				+ this.Data.Metadata.PATH_TO_OSU_DB + " TEXT,"
				+ this.Data.Metadata.PATH_TO_SONGS_FOLDER + " TEXT,"
				+ this.Data.Metadata.SAVE_FOLDER + " TEXT"
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
	
	
	private void insertIntoMetadata(int osuVersion, int folderCount, String playerName, int numberOfBeatmaps, String pathToOsuDb, String pathToSongsFolder, String saveFolder) throws SQLException {
		String sql = "INSERT INTO " + this.Data.Metadata.TABLE_NAME + "(" 
				+ this.Data.Metadata.OSU_VERSION + "," 
				+ this.Data.Metadata.FOLDER_COUNT + "," 
				+ this.Data.Metadata.PLAYER_NAME + "," 
				+ this.Data.Metadata.NUMBER_OF_BEATMAPS + ","
				+ this.Data.Metadata.PATH_TO_OSU_DB + ","
				+ this.Data.Metadata.PATH_TO_SONGS_FOLDER + ","
				+ this.Data.Metadata.SAVE_FOLDER
				+ ") VALUES(?,?,?,?,?,?,?)";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setInt(1, osuVersion);
		pstmt.setInt(2, folderCount);
		pstmt.setString(3, playerName);
		pstmt.setInt(4, numberOfBeatmaps);
		pstmt.setString(5, pathToOsuDb);
		pstmt.setString(6, pathToSongsFolder);
		pstmt.setString(7, saveFolder);
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
	
	
	public void insertAllData(OsuDbParser osuDb) throws SQLException, InterruptedException {
		this.insertIntoMetadata(osuDb.getOsuVersion(), osuDb.getFolderCount(), osuDb.getPlayerName(), osuDb.getNumberOfBeatmaps(), osuDb.getPathToOsuDb(), osuDb.getPathToSongsFolder(), "");
		
		int batchSize = 500; // actually much bigger than this
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
			for (int j = 0; j < beatmapSet.size() && !isRanked; j++) {
				if (beatmapSet.get(j).getRankedStatus() == 4) {
					isRanked = true;
					// store the index of the beatmap that is ranked. Most of the time this will be 0 but not in some case, so better 
					// use this as indication when assigning data to beatmapSet
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
				int artistID = this.selectArtistIDFromArtist(beatmap.getArtistName(), beatmap.getArtistNameUnicode());
				int songID = this.selectSongIDFromSong(beatmap.getSongTitle(), beatmap.getSongTitleUnicode(), beatmap.getSongSource());
				this.insertIntoBeatmapSetBatch(beatmapSetPStatement, beatmap.getBeatmapSetID(), artistID, songID, beatmap.getCreatorName(), beatmap.getFolderName(), beatmap.getAudioFileName(), false, false);
				
			}
			// if not, better loop through the unranked beatmaps to collect the data
			else {
				for (Beatmap beatmap : beatmapSet) {
					int artistID = this.selectArtistIDFromArtist(beatmap.getArtistName(), beatmap.getArtistNameUnicode());
					int songID = this.selectSongIDFromSong(beatmap.getSongTitle(), beatmap.getSongTitleUnicode(), beatmap.getSongSource());
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
				int beatmapSetAutoID = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmapSet.get(value.get(1)).getFolderName(), beatmapSet.get(value.get(1)).getAudioFileName());
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
					int beatmapSetAutoID = this.selectBeatmapSetAutoIDFromBeatmapSet(beatmap.getFolderName(), beatmap.getAudioFileName());
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
	
	
	private int selectArtistIDFromArtist(String artistName, String artistNameUnicode) throws SQLException {
		String sql = "SELECT " + this.Data.Artist.ARTIST_ID + " FROM " + this.Data.Artist.TABLE_NAME + " "
				+ "WHERE " + this.Data.Artist.ARTIST_NAME + " = ? AND " + this.Data.Artist.ARTIST_NAME_UNICODE + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, artistName);
		pstmt.setString(2, artistNameUnicode);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()) {
			return rs.getInt(1);
		}
		else {
			throw new SQLException("Failed to retrive newly inserted data");
		}
	}
	
	private int selectSongIDFromSong(String songTitle, String songTitleUnicode, String songSource) throws SQLException {
		String sql = "SELECT " + this.Data.Song.SONG_ID + " FROM " + this.Data.Song.TABLE_NAME + " "
				+ "WHERE " + this.Data.Song.SONG_TITLE + " = ? AND " + this.Data.Song.SONG_TITLE_UNICODE + " = ? AND "
				+ this.Data.Song.SONG_SOURCE + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, songTitle);
		pstmt.setString(2, songTitleUnicode);
		pstmt.setString(3, songSource);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()) {
			return rs.getInt(1);
		}
		else {
			throw new SQLException("Failed to retrive newly inserted data");
		}
	}
	
	private int selectBeatmapSetAutoIDFromBeatmapSet(String folderName, String audioName) throws SQLException {
		String sql = "SELECT " + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + " FROM " + this.Data.BeatmapSet.TABLE_NAME + " "
				+ "WHERE " + this.Data.BeatmapSet.FOLDER_NAME + " = ? AND " + this.Data.BeatmapSet.AUDIO_NAME + " = ?";
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		pstmt.setString(1, folderName);
		pstmt.setString(2, audioName);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()) {
			return rs.getInt(1);
		}
		else {
			throw new SQLException("Failed to retrive newly inserted data");
		}
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
	
	
	//TODO: For searching, loop through the string to check if it's unicode using Character.UnicodeBlock.Of method
	//to reduce database search
	
	// Add COLLATE NOCASE for all string search (although it's off by default just in case)
	
	// Display in format: SongSource(ArtistName) - songTitle ----- creatorName ------ audioLength etc
	// if no source, no need bracket for ArtistName
	// When unicode is chosen, artistName and songTitle is changed to unicode instead

	
	public ResultSet searchAll(String[] items, String[] searchedStrings, String[] orderBy) throws SQLException {
		// if searchedStrings == 1, no need having count
		
		String sql = "SELECT " + String.join(",", items) + "\n"
				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + " bs\n"
				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " b ON b." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = bs." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " a ON a." + this.Data.Artist.ARTIST_ID + " = bs." + this.Data.BeatmapSet.ARTIST_ID + "\n"
				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " s ON s." + this.Data.Song.SONG_ID + " = bs." + this.Data.BeatmapSet.SONG_ID + "\n"
				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " bst ON bst." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = bs." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " st ON st." + this.Data.SongTag.SONG_TAG_ID + " = bst." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
				+ "WHERE ";
		
		String[] searchedFields = {this.Data.SongTag.SONG_TAG_NAME, this.Data.Artist.ARTIST_NAME, this.Data.Artist.ARTIST_NAME_UNICODE
				, this.Data.Song.SONG_TITLE, this.Data.Song.SONG_TITLE_UNICODE, this.Data.BeatmapSet.CREATOR_NAME
				, this.Data.Song.SONG_SOURCE};
		
		List<String[]> allConditions = new ArrayList<String[]>();
		
		for (int i = 0; i < searchedFields.length; i++) {
			String[] conditions = new String[searchedStrings.length];
			for (int j = 0; j < searchedStrings.length; j++) {
				conditions[j] = searchedFields[i] + " COLLATE NOCASE LIKE ? ";
			}
			allConditions.add(conditions);
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < allConditions.size(); i++) {
			sb.append(String.join(" OR ", allConditions.get(i)));
			if (i != allConditions.size() - 1) {
				sb.append(" OR ");
			}
		}
		sb.append(" GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME);
		
		if (searchedStrings.length == 1) {
			sb.append(" HAVING COUNT(*) >= 1 ");
		}
		else {
			sb.append(" HAVING COUNT(CASE WHEN "); 
			for (int i = 0; i < searchedStrings.length; i++) { 
				for (int j = 0; j < allConditions.size(); j++) {
					sb.append(allConditions.get(j)[i]);
					if (j != allConditions.size() - 1) {
						sb.append(" OR ");
					}
				}
				sb.append(" THEN 1 END) >= 1 ");
				if (i != searchedStrings.length - 1) {
					sb.append(" AND COUNT(CASE WHEN ");
				}
			}
		}
		sb.append(" AND MAX(" + this.Data.Beatmap.TOTAL_TIME + ") ");
		// TODO: Collate nocase should not be applied to non-string fields
		sb.append(" ORDER BY ");
		for (int i = 0; i < orderBy.length; i++) {
			sb.append(orderBy[i] + " COLLATE NOCASE ");
			if (i != orderBy.length - 1) {
				sb.append(",");
			}
		}
		sql += sb.toString();
		
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		int index = 1; 
		
		for (int i = 0; i < searchedFields.length; i++) {
			for (int j = 0; j < searchedStrings.length; j++) {
				pstmt.setString(index, "%" + searchedStrings[j] + "%");
				index++;
			}
		}
		
		if (searchedStrings.length != 1) {
			for (int i = 0; i < searchedStrings.length; i++) {
				for (int j = 0; j < searchedFields.length; j++) {
					pstmt.setString(index, "%" + searchedStrings[i] + "%");
					index++;
				}
			}
		}
	
		return pstmt.executeQuery();
	}
	
	public ResultSet getTableInitData(String[] items) throws SQLException {
		String sql = "SELECT " + String.join(",", items) + "\n"
				+ "FROM " + this.Data.BeatmapSet.TABLE_NAME + " bs\n"
				+ "INNER JOIN " + this.Data.Beatmap.TABLE_NAME + " b ON b." + this.Data.Beatmap.BEATMAP_SET_AUTO_ID + " = bs." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + this.Data.Artist.TABLE_NAME + " a ON a." + this.Data.Artist.ARTIST_ID + " = bs." + this.Data.BeatmapSet.ARTIST_ID + "\n"
				+ "INNER JOIN " + this.Data.Song.TABLE_NAME + " s ON s." + this.Data.Song.SONG_ID + " = bs." + this.Data.BeatmapSet.SONG_ID + "\n"
				+ "INNER JOIN " + this.Data.BeatmapSet_SongTag.TABLE_NAME + " bst ON bst." + this.Data.BeatmapSet_SongTag.BEATMAP_SET_AUTO_ID + " = bs." + this.Data.BeatmapSet.BEATMAP_SET_AUTO_ID + "\n"
				+ "INNER JOIN " + this.Data.SongTag.TABLE_NAME + " st ON st." + this.Data.SongTag.SONG_TAG_ID + " = bst." + this.Data.BeatmapSet_SongTag.SONG_TAG_ID + "\n"
				+ "GROUP BY " + this.Data.BeatmapSet.FOLDER_NAME + ", " + this.Data.BeatmapSet.AUDIO_NAME + "\n"
				+ "HAVING MAX(" + this.Data.Beatmap.TOTAL_TIME + ")\n"
				+ "ORDER BY " + this.Data.Beatmap.LAST_MODIFICATION_TIME;
		Statement stmt = this.getConn().createStatement();
		return stmt.executeQuery(sql);
	}
	
	public void updateMetadata(int metadataID, String[] items, String[] results) throws Exception, SQLException {
		if (items.length != results.length) {
			throw new Exception("Update argument num doesn't match");
		}
		
		String sql = "UPDATE " + this.Data.Metadata.TABLE_NAME + "\n"
				+ "SET ";
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < items.length; i++) {
			sj.add(items[i] + " = ?");
		}
		sql += sj.toString();
		sql += " WHERE " + this.Data.Metadata.METADATA_ID + " = ?;";
		
		PreparedStatement pstmt = this.getConn().prepareStatement(sql);
		int index = 1;
		for (int i = 0; i < results.length; i++) {
			pstmt.setString(index, results[i]);
			index++;
		}
		pstmt.setInt(index, metadataID);
		pstmt.executeUpdate();
	}
	
	
	// for threading only
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
			public final String PATH_TO_OSU_DB = "PathToOsuDb";
			public final String PATH_TO_SONGS_FOLDER = "PathToSongsFolder";
			public final String SAVE_FOLDER = "SaveFolder";
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