package com.mega.android.pdfViewer;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;

public class Recent extends ArrayList<String> {
	
	/**
	 * Default serial version identifier because base class is serializable.
	 */
	private static final long serialVersionUID = 1L;
	private final static int MAX_RECENT=5; /* must be at least 1 */
	private final String PREF_TAG = "Recent";
	private final String RECENT_PREFIX = "Recent.";
	private final String RECENT_COUNT = "count";
	
	private Context context;
	
	public Recent(Context context) {
		super();
		
		this.context = context;

		SharedPreferences pref = this.context.getSharedPreferences(PREF_TAG, 0);
		
		int count = pref.getInt(RECENT_COUNT, 0);
		
		for(int i=0; i<count; i++) {
			String fileName = pref.getString(RECENT_PREFIX + i, "");
			File file = new File(fileName);
			if (file.exists()) {
				add(fileName);
			}
		}
	}
	
	private void write() {
		SharedPreferences.Editor edit = 
				this.context.getSharedPreferences(PREF_TAG, 0).edit();
		
		edit.putInt(RECENT_COUNT, size());
		
		for(int i=0; i<size(); i++) {
			edit.putString(RECENT_PREFIX + i, get(i));
		}
		
		edit.commit();
	}
	
	void commit() {
		for(int i=size()-1; i>=0; i--) {
			for(int j=0; j<i; j++) {
				if (get(i).equals(get(j))) {
					remove(i);
					break;
				}
			}
		}
		
		for(int i=size()-1; i>=MAX_RECENT; i--) {
			remove(i);
		}
		
		write();
	}
}
