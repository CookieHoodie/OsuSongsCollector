package application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OsuSongsCollector {
	// private members
	private OsuDbParser osuDb;
	
	// public members
	public String fullpath;
	
	// constructors
	public OsuSongsCollector(OsuDbParser osuDb) throws FileNotFoundException, IOException {
//		this.fullpath = fullpath;
//		this.osuDb = new OsuDbParser(fullpath);
		this.osuDb = osuDb;
	}
	
	// public methods
	public List<List<Beatmap>> searchFor(String searchedString, Comparator<List<Beatmap>> comparator) {
		String loweredSearchedString = searchedString.toLowerCase();
		String[] splitLoweredSearchedString = loweredSearchedString.split("=", 2);
		List<List<Beatmap>> result = new ArrayList<List<Beatmap>>();
		if (splitLoweredSearchedString.length == 1 || !this.matchSearchCriteria(splitLoweredSearchedString[0])) {
			result = this.osuDb.getSongsFolder().stream()
				     .filter(item -> item.get(0).getSongTitle().toLowerCase().contains(loweredSearchedString)
				    		 || item.get(0).getSongTitleUnicode().toLowerCase().contains(loweredSearchedString)
				    		 || item.get(0).getArtistName().toLowerCase().contains(loweredSearchedString)
				    		 || item.get(0).getArtistNameUnicode().toLowerCase().contains(loweredSearchedString)
				    		 || item.get(0).getCreatorName().toLowerCase().contains(loweredSearchedString)
				    		 || item.get(0).getSongSource().toLowerCase().contains(loweredSearchedString)
				    		 || item.get(0).getSongTags().toLowerCase().contains(loweredSearchedString))
				     .collect(Collectors.toList());
		}
		else {
			switch (splitLoweredSearchedString[0]) {
			case "songtitle":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getSongTitle().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "songtitleunicode":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getSongTitleUnicode().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "artistname":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getArtistName().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "artistnameunicode":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getArtistNameUnicode().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "creatorname":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getCreatorName().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "songsource":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getSongSource().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "songtags":
				result = this.osuDb.getSongsFolder().stream()
			     .filter(item -> item.get(0).getSongTags().toLowerCase().contains(splitLoweredSearchedString[1]))
			     .collect(Collectors.toList());
				break;
			case "beatmapid":
				if (splitLoweredSearchedString[1].matches("[0-9]+")) {
					int beatmapId = Integer.parseInt(splitLoweredSearchedString[1]);
					Optional<Beatmap> beatmap = this.osuDb.getSongsFolder().stream()
				      .flatMap(Collection::stream)
				      .filter(item -> item.getBeatmapID() == beatmapId)
				      .findFirst();
					if (beatmap.isPresent()) {
						List<Beatmap> temp = new ArrayList<Beatmap>();
						temp.add(beatmap.get());
						result.add(temp);
					}
				}
				break;
			case "beatmapsetid":
				if (splitLoweredSearchedString[1].matches("[0-9]+")) {
					int beatmapSetId = Integer.parseInt(splitLoweredSearchedString[1]);
					Optional<List<Beatmap>> beatmapSet = this.osuDb.getSongsFolder().stream()
				      .filter(item -> item.get(0).getBeatmapSetID() == beatmapSetId)
				      .findFirst();
					if (beatmapSet.isPresent()) {
						List<Beatmap> temp = beatmapSet.get();
						result.add(temp);
					}
				}
				break;
			}
		}
		Collections.sort(result, comparator);
		return result;
		
		// base on songTitle(UNI), artistName(UNI), songSource, songTags, beatmapID, beatmapSetID, length, played
	}
	
	// private methods
	private boolean matchSearchCriteria(String keyword) {
		if (keyword.equals("songtitle") || keyword.equals("songtitleunicode") || keyword.equals("artistname")
				|| keyword.equals("artistnameunicode") || keyword.equals("creatorname") || keyword.equals("songsource")
				|| keyword.equals("songtags") || keyword.equals("beatmapid") || keyword.equals("beatmapsetid") ) {
			return true;
		}
		return false;
	}
}

//TODO: setLocale, UTF-8
// change length to actual length of mp3
// maybe change loading osuDb into start().