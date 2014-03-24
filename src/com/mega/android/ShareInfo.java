package com.mega.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/*
 * Helper class to process shared files from other activities
 */
public class ShareInfo {

	public String title = null;
	public InputStream inputStream = null;
	public long size = -1;
	private File file = null;
	public boolean failed = false;
	
	/*
	 * Get ShareInfo from File
	 */
	public static ShareInfo infoFromFile(File file) {
		ShareInfo info = new ShareInfo();
		info.file = file;
		info.size = file.length();
		info.title = file.getName();
		try {
			info.inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return null;
		}
		return info;
	}
	
	public String getFileAbsolutePath() {
		return file.getAbsolutePath();
	}
	
	public String getTitle() {
		return title;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public long getSize() {
		return size;
	}
	
}
