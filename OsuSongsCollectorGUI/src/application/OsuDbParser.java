package application;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import javafx.concurrent.Task;

public class OsuDbParser extends OsuReader{
	// for threading
	private BiConsumer<Integer, Integer> progressUpdate = null;
	
    // public member
	private String pathToSongsFolder;
	private String pathToOsuDb;
    private int osuVersion;
    private int folderCount;
    private String playerName;
    private int numberOfBeatmaps; 
//    private List<Beatmap> beatmaps = new ArrayList<Beatmap>();
    private List<List<Beatmap>> songsFolder = new ArrayList<List<Beatmap>>();
    
    // constructors
    public OsuDbParser(String pathToOsuDb, String pathToSongsFolder) throws FileNotFoundException, IOException {
        this(new FileInputStream(pathToOsuDb), pathToSongsFolder, pathToOsuDb);
    }

    public OsuDbParser(FileInputStream source, String pathToSongsFolder, String pathToOsuDb) {
        this.setPathToOsuDb(pathToOsuDb);
    	this.setPathToSongsFolder(pathToSongsFolder);
        this.setReader(new DataInputStream(source));
    }
    
    // public methods
    public void startParsing() throws IOException, InterruptedException {
    	try {
			this.startLoading();
		} 
        finally {
        	this.closeFile();
        }
    }
    
   public void startParsingMetadataOnly() throws IOException {
	   try {
		   this.parseAndSetMetadata();
	   }
	   finally {
		   this.closeFile();
	   }
   }
    
    public void setThreadData(BiConsumer<Integer, Integer> progressUpdate) {
		this.setProgressUpdate(progressUpdate);
	}
    
    // private methods
    private void parseAndSetMetadata() throws IOException {
    	this.setOsuVersion(this.readInt());
    	this.setFolderCount(this.readInt());
    	this.skipBytes(9); // bool + date
    	this.setPlayerName(this.readString());
    	this.setNumberOfBeatmaps(this.readInt());
    }
    
    private void startLoading() throws IOException, InterruptedException {
		this.parseAndSetMetadata();
    	Map<String, List<Beatmap>> folders = new TreeMap<String, List<Beatmap>>();
    	for (int i = 0; i < this.numberOfBeatmaps; i++) {
//    		if (this.task != null) {
//    			if (this.task.isCancelled()) {
//    				return;
//    			}
//    		}
    		if (Thread.currentThread().isInterrupted()) {
    			throw new InterruptedException("LoadOsuDbTask is interrupted");
    		}
    		Beatmap beatmap = new Beatmap();
    		this.skipBytes(4);
			beatmap.setArtistName(this.readString());
			beatmap.setArtistNameUnicode(this.readString());
			beatmap.setSongTitle(this.readString());
			beatmap.setSongTitleUnicode(this.readString());
			beatmap.setCreatorName(this.readString());
//			beatmap.setDifficulty(this.readString());
			this.skipString();
			beatmap.setAudioFileName(this.readString());
//			beatmap.setMD5Hash(this.readString());
			this.skipString();
			beatmap.setNameOfOsuFile(this.readString());
			beatmap.setRankedStatus(this.readByte());
			this.skipBytes(6); // short * 3
			beatmap.setLastModificationTime(this.readDate());
//			beatmap.setAR(this.readSingle());
//			beatmap.setCS(this.readSingle());
//			beatmap.setHP(this.readSingle());
//			beatmap.setOD(this.readSingle());
			this.skipBytes(24); // 8 + 16
			for (int j = 0; j < 4; j++) {
				int numOfPairs = this.readInt();
				this.skipBytes(numOfPairs * 14);
			}
			this.skipBytes(4); 
			beatmap.setTotalTime(this.readInt());
			beatmap.setPreviewTime(this.readInt());
			int numOfTimingPoints = this.readInt();
			this.skipBytes(17 * numOfTimingPoints);
			beatmap.setBeatmapID(this.readInt());
			beatmap.setBeatmapSetID(this.readInt());
			beatmap.setThreadID(this.readInt());
//			beatmap.setGradeStandard(this.readByte());
//			beatmap.setGradeTaiko(this.readByte());
//			beatmap.setGradeCTB(this.readByte());
//			beatmap.setGradeMania(this.readByte());
			this.skipBytes(11); // 2 + 4 + 1 + 4
//			beatmap.setGameplayMode(this.readByte());
			beatmap.setSongSource(this.readString());
			beatmap.setSongTags(this.readString());
			this.skipBytes(2); 
			this.skipString();
			beatmap.setUnplayed(this.readBoolean());
			beatmap.setLastPlayedTime(this.readDate());
			this.skipBytes(1); 
			beatmap.setFolderName(this.readString());
//			beatmap.setLastCheckedTime(this.readDate());
			this.skipBytes(18); // bool * 5 + int + byte + 8
			
			if (folders.containsKey(beatmap.getFolderName())) {
				folders.get(beatmap.getFolderName()).add(beatmap);
			}
			else {
				List<Beatmap> b = new ArrayList<Beatmap>();
				b.add(beatmap);
				folders.put(beatmap.getFolderName(), b);
			}
			
//			this.getBeatmaps().add(beatmap);
			
			
			if (this.progressUpdate != null) {
				progressUpdate.accept(i + 1, numberOfBeatmaps);
			}
    	}
    	this.setSongsFolder(new ArrayList<List<Beatmap>>(folders.values()));
    }

	public int getOsuVersion() {
		return osuVersion;
	}

	private void setOsuVersion(int osuVersion) {
		this.osuVersion = osuVersion;
	}

	public int getFolderCount() {
		return folderCount;
	}

	private void setFolderCount(int folderCount) {
		this.folderCount = folderCount;
	}

	public String getPlayerName() {
		return playerName;
	}

	private void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public int getNumberOfBeatmaps() {
		return numberOfBeatmaps;
	}

	private void setNumberOfBeatmaps(int numberOfBeatmaps) {
		this.numberOfBeatmaps = numberOfBeatmaps;
	}

	public List<List<Beatmap>> getSongsFolder() {
		return songsFolder;
	}

	private void setSongsFolder(List<List<Beatmap>> songsFolder) {
		this.songsFolder = songsFolder;
	}
//	public List<Beatmap> getBeatmaps() {
//		return this.beatmaps;
//	}
	
	private void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
		this.progressUpdate = progressUpdate;
	}

	public String getPathToSongsFolder() {
		return pathToSongsFolder;
	}

	public void setPathToSongsFolder(String pathToSongsFolder) {
		this.pathToSongsFolder = pathToSongsFolder;
	}

	public String getPathToOsuDb() {
		return pathToOsuDb;
	}

	private void setPathToOsuDb(String fullPathToOsuDb) {
		this.pathToOsuDb = fullPathToOsuDb;
	}
}
