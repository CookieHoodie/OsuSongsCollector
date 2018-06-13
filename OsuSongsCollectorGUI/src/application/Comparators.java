package application;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Comparators {
	public static Comparator<List<Beatmap>> songTitleComparator = new Comparator<List<Beatmap>>() {
    	@Override
    	public int compare(List<Beatmap> b1, List<Beatmap> b2) {
    		String songTitle1 = b1.get(0).getSongTitle().toLowerCase();
    		String songTitle2 = b2.get(0).getSongTitle().toLowerCase();
    		
    		return songTitle1.compareTo(songTitle2);
    	}
    };
    
    public static Comparator<List<Beatmap>> creatorNameComparator = new Comparator<List<Beatmap>>() {
    	@Override
    	public int compare(List<Beatmap> b1, List<Beatmap> b2) {
    		String creatorName1 = b1.get(0).getCreatorName().toLowerCase();
    		String creatorName2 = b2.get(0).getCreatorName().toLowerCase();
    		
    		return creatorName1.compareTo(creatorName2);
    	}
    };
    
//    public static Comparator<List<Beatmap>> dateAddedComparator = new Comparator<List<Beatmap>>() {
//    	@Override
//    	public int compare(List<Beatmap> b1, List<Beatmap> b2) {
//    		if (b1.get(0).getLastModificationTime().before(b2.get(0).getLastModificationTime())) {
//                return -1;
//            } else if (b1.get(0).getLastModificationTime().after(b2.get(0).getLastModificationTime())) {
//                return 1;
//            } else {
//                return 0;
//            }  
//    	}
//    };
    
    public static Comparator<List<Beatmap>> lengthComparator = new Comparator<List<Beatmap>>() {
    	@Override
    	public int compare(List<Beatmap> b1, List<Beatmap> b2) {
    		int length1 = Collections.min(b1, Comparators.lengthInnerComparator).getTotalTime();
    		int length2 = Collections.min(b2, Comparators.lengthInnerComparator).getTotalTime();
    		return Integer.compare(length1, length2);
    	}
    };
    
    private static Comparator<Beatmap> lengthInnerComparator = new Comparator<Beatmap>() {
    	@Override
    	public int compare(Beatmap b1, Beatmap b2) {
    		return Integer.compare(b1.getTotalTime(), b2.getTotalTime());
    	}
    };
    
    public static Comparator<List<Beatmap>> artistNameComparator = new Comparator<List<Beatmap>>() {
    	@Override
    	public int compare(List<Beatmap> b1, List<Beatmap> b2) {
    		String artistName1 = b1.get(0).getArtistName().toLowerCase();
    		String artistName2 = b2.get(0).getArtistName().toLowerCase();
    		if (artistName1.equals(artistName2)) {
    			return b1.get(0).getSongTitle().toLowerCase().compareTo(b2.get(0).getSongTitle().toLowerCase());
    		}
    		return artistName1.compareTo(artistName2);
    	}
    };
}
