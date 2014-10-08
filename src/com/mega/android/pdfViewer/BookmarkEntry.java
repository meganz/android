package com.mega.android.pdfViewer;

public class BookmarkEntry implements Comparable<BookmarkEntry> {
	public int numberOfPages;
	public int page;
	public float absoluteZoomLevel;
	public int rotation;
	public int offsetX;
	public String comment;
	
	public BookmarkEntry(int numberOfPages, int page, float absoluteZoomLevel,
			int rotation, int offsetX) {
		this(null, numberOfPages, page, absoluteZoomLevel, rotation, offsetX);
	}
	
	public BookmarkEntry(String comment, int numberOfPages, int page, float absoluteZoomLevel,
			int rotation, int offsetX) {
		this.comment = comment;
		this.numberOfPages = numberOfPages;
		this.page = page;
		this.absoluteZoomLevel = absoluteZoomLevel;
		this.rotation = rotation;
		this.offsetX = offsetX;
	}
	
	public BookmarkEntry(String s) {
		this(null, s);
	}
	
	public BookmarkEntry(String comment, String s) {
		this.comment = comment;
		
		String data[] = s.split(" ");
		
		if (0 < data.length) {
			this.numberOfPages = Integer.parseInt(data[0]);
		}
		else {
			this.numberOfPages = 0;
		}
		
		if (1 < data.length) {
			this.page = Integer.parseInt(data[1]);
		}
		else { 
			this.page = 0;
		}
		
		if (2 < data.length) {
			this.absoluteZoomLevel = Float.parseFloat(data[2]);
		}
		else {
			this.absoluteZoomLevel = 0f;
		}
		
		if (3 < data.length) {
			this.rotation = Integer.parseInt(data[3]);
		}
		else {
			this.rotation = 0;
		}
		
		if (4 < data.length) {
			this.offsetX = Integer.parseInt(data[4]); 
		}
		else {
			this.offsetX = 0;
		}
	}
	
	public String toString() {
		return ""+numberOfPages+" "+page+" "+absoluteZoomLevel+" "+rotation+" "+offsetX;
	}

	public int compareTo(BookmarkEntry entry) {
		if (this.page < entry.page)
			return -1;
		else if (entry.page < this.page)
			return 1;
		else 
			return this.comment.compareTo(entry.comment);
	}
	
	public boolean equals(BookmarkEntry entry) {
		return this.toString() == entry.toString() && 
				this.comment == entry.comment;
	}
}
