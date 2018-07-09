package application;

import java.util.Date;

public class Beatmap {
	private String artistName;
	private String artistNameUnicode;
	private String songTitle;
	private String songTitleUnicode;
	private String creatorName;
//	private String difficulty;
	private String audioFileName;
//	private String MD5Hash;
	private String nameOfOsuFile;
	private Byte rankedStatus;
	private long lastModificationTime;
//	private float AR;
//	private float CS;
//	private float HP;
//	private float OD;
	private int totalTime; // in milli sec
	private int previewTime;
	private int beatmapID;
	private int beatmapSetID;
	private int threadID;
//	private Byte gradeStandard;
//	private Byte gradeTaiko;
//	private Byte gradeCTB;
//	private Byte gradeMania;
//	private Byte gameplayMode;
	private String songSource;
	private String songTags;
	private boolean isUnplayed;
	private long lastPlayedTime;
	private String folderName;
//	private Date lastCheckedTime;
	
	public String getArtistName() {
		return artistName;
	}
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}
	public String getArtistNameUnicode() {
		return artistNameUnicode;
	}
	public void setArtistNameUnicode(String artistNameUnicode) {
		this.artistNameUnicode = artistNameUnicode;
	}
	public String getSongTitle() {
		return songTitle;
	}
	public void setSongTitle(String songTitle) {
		this.songTitle = songTitle;
	}
	public String getSongTitleUnicode() {
		return songTitleUnicode;
	}
	public void setSongTitleUnicode(String songTitleUnicode) {
		this.songTitleUnicode = songTitleUnicode;
	}
	public String getCreatorName() {
		return creatorName;
	}
	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}
//	public String getDifficulty() {
//		return difficulty;
//	}
//	public void setDifficulty(String difficulty) {
//		this.difficulty = difficulty;
//	}
	public String getAudioFileName() {
		return audioFileName;
	}
	public void setAudioFileName(String audioFileName) {
		this.audioFileName = audioFileName;
	}
//	public String getMD5Hash() {
//		return MD5Hash;
//	}
//	public void setMD5Hash(String mD5Hash) {
//		MD5Hash = mD5Hash;
//	}
	public String getNameOfOsuFile() {
		return nameOfOsuFile;
	}
	public void setNameOfOsuFile(String nameOfOsuFile) {
		this.nameOfOsuFile = nameOfOsuFile;
	}
	public Byte getRankedStatus() {
		return rankedStatus;
	}
	public void setRankedStatus(Byte rankedStatus) {
		this.rankedStatus = rankedStatus;
	}
	public long getLastModificationTime() {
		return lastModificationTime;
	}
	public void setLastModificationTime(long lastModificationTime) {
		this.lastModificationTime = lastModificationTime;
	}
//	public float getAR() {
//		return AR;
//	}
//	public void setAR(float aR) {
//		AR = aR;
//	}
//	public float getCS() {
//		return CS;
//	}
//	public void setCS(float cS) {
//		CS = cS;
//	}
//	public float getHP() {
//		return HP;
//	}
//	public void setHP(float hP) {
//		HP = hP;
//	}
//	public float getOD() {
//		return OD;
//	}
//	public void setOD(float oD) {
//		OD = oD;
//	}
	public int getTotalTime() {
		return totalTime;
	}
	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}
	public int getPreviewTime() {
		return previewTime;
	}
	public void setPreviewTime(int previewTime) {
		this.previewTime = previewTime;
	}
	public int getBeatmapID() {
		return beatmapID;
	}
	public void setBeatmapID(int beatmapID) {
		this.beatmapID = beatmapID;
	}
	public int getBeatmapSetID() {
		return beatmapSetID;
	}
	public void setBeatmapSetID(int beatmapSetID) {
		this.beatmapSetID = beatmapSetID;
	}
	public int getThreadID() {
		return threadID;
	}
	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}
//	public Byte getGradeStandard() {
//		return gradeStandard;
//	}
//	public void setGradeStandard(Byte gradeStandard) {
//		this.gradeStandard = gradeStandard;
//	}
//	public Byte getGradeTaiko() {
//		return gradeTaiko;
//	}
//	public void setGradeTaiko(Byte gradeTaiko) {
//		this.gradeTaiko = gradeTaiko;
//	}
//	public Byte getGradeCTB() {
//		return gradeCTB;
//	}
//	public void setGradeCTB(Byte gradeCTB) {
//		this.gradeCTB = gradeCTB;
//	}
//	public Byte getGradeMania() {
//		return gradeMania;
//	}
//	public void setGradeMania(Byte gradeMania) {
//		this.gradeMania = gradeMania;
//	}
//	public Byte getGameplayMode() {
//		return gameplayMode;
//	}
//	public void setGameplayMode(Byte gameplayMode) {
//		this.gameplayMode = gameplayMode;
//	}
	public String getSongSource() {
		return songSource;
	}
	public void setSongSource(String songSource) {
		this.songSource = songSource;
	}
	public String getSongTags() {
		return songTags;
	}
	public void setSongTags(String songTags) {
		this.songTags = songTags;
	}
	public boolean isUnplayed() {
		return isUnplayed;
	}
	public void setUnplayed(boolean isUnplayed) {
		this.isUnplayed = isUnplayed;
	}
	public long getLastPlayedTime() {
		return lastPlayedTime;
	}
	public void setLastPlayedTime(long lastPlayedTime) {
		this.lastPlayedTime = lastPlayedTime;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
//	public Date getLastCheckedTime() {
//		return lastCheckedTime;
//	}
//	public void setLastCheckedTime(Date lastCheckedTime) {
//		this.lastCheckedTime = lastCheckedTime;
//	}
}
