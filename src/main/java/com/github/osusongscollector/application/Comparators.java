package com.github.osusongscollector.application;

import com.github.osusongscollector.controllers.SongsDisplayController.TableViewData;

import java.util.Comparator;

public class Comparators {
	public static class SongTitleComparator implements Comparator<TableViewData> {
		@Override public int compare(TableViewData row1, TableViewData row2) {
			return row1.songTitleProperty().get().toLowerCase().compareTo(row2.songTitleProperty().get().toLowerCase());
		}
		
		@Override public String toString() {
			return "Song Title";
		}
	}

	public static class ArtistNameComparator implements Comparator<TableViewData> {
		@Override public int compare(TableViewData row1, TableViewData row2) {
			return row1.artistNameProperty().get().toLowerCase().compareTo(row2.artistNameProperty().get().toLowerCase());
		}
		
		@Override public String toString() {
			return "Artist";
		}
	}
	
	public static class CreatorNameComparator implements Comparator<TableViewData> {
		@Override public int compare(TableViewData row1, TableViewData row2) {
			return row1.creatorNameProperty().get().toLowerCase().compareTo(row2.creatorNameProperty().get().toLowerCase());
		}
		
		@Override public String toString() {
			return "Creator";
		}
	}
	
	public static class TotalTimeComparator implements Comparator<TableViewData> {
		@Override public int compare(TableViewData row1, TableViewData row2) {
			return Integer.compare(row1.totalTimeProperty().get(), row2.totalTimeProperty().get());
		}
		
		@Override public String toString() {
			return "Length";
		}
	}
	
	public static class LastModificationTimeComparator implements Comparator<TableViewData> {
		@Override public int compare(TableViewData row1, TableViewData row2) {
			return Long.compare(row1.lastModificationTimeProperty().get(), row2.lastModificationTimeProperty().get());
		}
		
		@Override public String toString() {
			return "Date Added";
		}
	}
	
}
