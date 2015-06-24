package nz.mega.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nz.mega.android.utils.Util;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;


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
	
	/*
	 * Process incoming Intent and get list of ShareInfo objects
	 */
	public static List<ShareInfo> processIntent(Intent intent, Context context) {
		log(intent.getAction() + " of action");
		
		if (intent.getAction() == null || intent.getAction().equals(FileExplorerActivity.ACTION_PROCESSED)) {
			return null;
		}
		if (context == null) {
			return null;
		}
		// Process multiple items
		if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
			log("multiple!");
			return processIntentMultiple(intent, context);
		}
		ShareInfo shareInfo = new ShareInfo();

		Bundle extras = intent.getExtras();
		// File data in EXTRA_STREAM
		if (extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
			log("extras is not null");
			Object streamObject = extras.get(Intent.EXTRA_STREAM);
			if (streamObject instanceof Uri) {
				log("instance of URI");
				log(streamObject.toString());
				shareInfo.processUri((Uri) streamObject, context);
			} else if (streamObject == null) {
				log("stream object is null!");
				return null;
			} else {
				log("unhandled type " + streamObject.getClass().getName());
				for (String key : extras.keySet()) {
					log("Key " + key);
				}
				return processIntentMultiple(intent, context);
			}
		// Get File info from Data URI
		} else {
			Uri dataUri = intent.getData();
			if (dataUri == null) {
				log("data uri is null");
				return null;
			}
			shareInfo.processUri(dataUri, context);
		}
		if (shareInfo.file == null) {
			log("share info file is null");
			return null;
		}
		intent.setAction(FileExplorerActivity.ACTION_PROCESSED);
		ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
		result.add(shareInfo);
		return result;
	}
	
	/*
	 * Process Multiple files
	 */
	public static List<ShareInfo> processIntentMultiple(Intent intent,
			Context context) {
		log("processIntentMultiple");
		ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

		if (imageUris == null || imageUris.size() == 0) {
			return null;
		}
		ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
		for (Uri uri : imageUris) {
			if (uri == null)
				continue;
			log("----: "+uri.toString());
			ShareInfo info = new ShareInfo();
			info.processUri(uri, context);
			if (info.file == null) {
				continue;
			}
			result.add(info);
		}
		intent.setAction(FileExplorerActivity.ACTION_PROCESSED);
		return result;
	}
	
	/*
	 * Get info from Uri
	 */
	private void processUri(Uri uri, Context context) {
		// getting input stream
		inputStream = null;
		try {
			inputStream = context.getContentResolver().openInputStream(uri);
		} catch (Exception e) {
		}

		String scheme = uri.getScheme();
		if(scheme != null)
		{
			if (scheme.equals("content")) {
				log("scheme content");
				processContent(uri, context);
			} else if (scheme.equals("file")) {
				log("file content");
				processFile(uri, context);
			}
		}

		if (inputStream != null) {
			try {
				file = null;
				String path = uri.getPath();
				try{ file = new File(path); }
				catch(Exception e){}
				if((file != null) && file.exists() && file.canRead())
				{
					size = file.length();
					return;
				}
				
				file = null;
				path = getRealPathFromURI(context, uri);
				try
				{ file = new File(path); }
				catch(Exception e){}
				if((file != null) && file.exists() && file.canRead())
				{
					size = file.length();
					return;
				}
				
				if (context.getExternalCacheDir() != null){
					if (title != null){
						file = new File(context.getExternalCacheDir(), title);
					}
					else{
						return;
					}
				}
				else{
					if (title != null){
						file = new File(context.getCacheDir(), title);
					}
					else{
						return;
					}
				}
				OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
				int bufferSize = 1024;
				byte[] buffer = new byte[bufferSize];
				int len = 0;
				while ((len = inputStream.read(buffer)) != -1) {
					stream.write(buffer, 0, len);
				}
				if (stream != null) {
					stream.close();
				}

				inputStream = new FileInputStream(file);
				size = file.length();
			} 
			catch (IOException e) {
				inputStream = null;
				if (file != null) {
					file.delete();
				}
			}
		}
	}
	
	/*
	 * Get info from content provider
	 */
	private void processContent(Uri uri, Context context) {
		ContentProviderClient client = null;
		try {
			client = context.getContentResolver().acquireContentProviderClient(uri);
			Cursor cursor = null;
			cursor = client.query(uri, null, null, null, null);
			if(cursor.getCount()==0) return;
			cursor.moveToFirst();
			int displayIndex = cursor.getColumnIndex("_display_name");
			if(displayIndex != -1)
				title = cursor.getString(displayIndex);
			int sizeIndex = cursor.getColumnIndex("_size");
			if (sizeIndex != -1) {
				long size = Long.valueOf(cursor.getString(sizeIndex));
				if (size > 0) {
					this.size = size;
				}
			}

			if (size == -1 || inputStream == null) {
				int dataIndex = cursor.getColumnIndex("_data");
				if (dataIndex != -1) {
					String data = cursor.getString(dataIndex);
					File dataFile = new File(data);
					if (dataFile.exists() && dataFile.canRead()) {
						if (size == -1) {
							long size = dataFile.length();
							if (size > 0) {
								this.size = size;
							}
						}
						if (inputStream == null) {
							try {
								inputStream = new FileInputStream(dataFile);
							} catch (FileNotFoundException e) {
							}
						}
					}
				}
			}
		
			client.release();
		} 
		catch (Exception e) {
		}
	}
	
	/*
	 * Process Uri as File path
	 */
	private void processFile(Uri uri, Context context) {
		log("processing file");
		File file = null;
		try {
			file = new File(new URI(uri.toString()));
		} catch (URISyntaxException e1) {
			file = new File(uri.toString().replace("file:///", "/"));
		}
		if (!file.exists() || !file.canRead()) {
			log("cantread :( " + file.exists() + " " + file.canRead() + " "
					+ uri.toString());
			return;
		}
		if (file.isDirectory()) {
			log("is folder");
			return;
		}
		log("continue processing..");
		size = file.length();
		title = file.getName();
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
		}
		log(title + " " + size);
	}
	
	private String getRealPathFromURI(Context context, Uri contentURI) {
		if(contentURI == null) return null;
	    String path = null;
	    Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(contentURI, null, null, null, null);
		    if(cursor == null) return null;
		    if(cursor.getCount() == 0)
		    {
		    	cursor.close();
		    	return null;
		    }
		    
		    cursor.moveToFirst();
		    int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA); 
		    if(idx == -1) 
		    {
		    	cursor.close();
		    	return null;
		    }
		    
		    try { path = cursor.getString(idx); } 
		    catch(Exception ex)
		    {
		    	cursor.close();
		    	return null;
		    }
	    }
	    catch(Exception e)
	    {
	    	if(cursor != null)
	    		cursor.close();
	    	return null;
	    }
		
    	if(cursor != null)
    		cursor.close();
	    return path;
	}
	
	private static void log(String log) {
		Util.log("ShareInfo", log);
	}
	
}
