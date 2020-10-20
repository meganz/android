package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;

import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;


/*
 * Helper class to process shared files from other activities
 */
public class ShareInfo implements Serializable {

	public String title = null;
	private long lastModified;
	public transient InputStream inputStream = null;
	public long size = -1;
	private File file = null;
	public boolean isContact = false;
	public Uri contactUri = null;

	private static Intent mIntent;
	
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

	public long getLastModified() {
	    return lastModified;
    }
	
	/*
	 * Process incoming Intent and get list of ShareInfo objects
	 */
	public static List<ShareInfo> processIntent(Intent intent, Context context) {
		logDebug(intent.getAction() + " of action");
		
		if (intent.getAction() == null || intent.getAction().equals(FileExplorerActivityLollipop.ACTION_PROCESSED)||intent.getAction().equals(FileExplorerActivityLollipop.ACTION_PROCESSED)) {
			return null;
		}
		if (context == null) {
			return null;
		}

		mIntent = intent;

		// Process multiple items
		if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
			logDebug("Multiple!");
			return processIntentMultiple(intent, context);
		}
		ShareInfo shareInfo = new ShareInfo();

		Bundle extras = intent.getExtras();
		// File data in EXTRA_STREAM
		if (extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
			logDebug("Extras is not null");
			Object streamObject = extras.get(Intent.EXTRA_STREAM);
			if (streamObject instanceof Uri) {
				logDebug("Instance of URI");
				logDebug(streamObject.toString());
				shareInfo.processUri((Uri) streamObject, context);
			} else if (streamObject == null) {
				logDebug("Stream object is null!");
				return null;
			} else {
				logDebug("Unhandled type " + streamObject.getClass().getName());
				for (String key : extras.keySet()) {
					logDebug("Key " + key);
				}
				return processIntentMultiple(intent, context);
			}
		}
		else if (intent.getClipData() != null) {
			if(Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
				logDebug("Multiple ACTION_GET_CONTENT");
				return processGetContentMultiple(intent, context);
			}
		}
		// Get File info from Data URI
		else {
			Uri dataUri = intent.getData();
			if (dataUri == null) {
				logWarning("Data uri is null");
//
//				if(Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
//					log("Multiple ACTION_GET_CONTENT");
//					return processGetContentMultiple(intent, context);
//				}
				return null;
			}
			shareInfo.processUri(dataUri, context);
		}
		if (shareInfo.file == null) {
			logWarning("Share info file is null");
			return null;
		}
		intent.setAction(FileExplorerActivityLollipop.ACTION_PROCESSED);
		
		ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
		result.add(shareInfo);
		return result;
	}
	
	/*
	 * Process Multiple files from GET_CONTENT Intent
	 */
	@SuppressLint("NewApi")
	public static List<ShareInfo> processGetContentMultiple(Intent intent,Context context) {
		logDebug("processGetContentMultiple");
		ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
		ClipData cD = intent.getClipData();
		if(cD!=null&&cD.getItemCount()!=0){
		
            for(int i = 0; i < cD.getItemCount(); i++){
            	ClipData.Item item = cD.getItemAt(i);
            	Uri uri = item.getUri();
				logDebug("ClipData uri: " + uri);
            	if (uri == null)
    				continue;
				logDebug("Uri: " + uri.toString());
    			ShareInfo info = new ShareInfo();
    			info.processUri(uri, context);
    			if (info.file == null) {
    				continue;
    			}
    			result.add(info);
            }
		}
		else{
			logWarning("ClipData NUll or size=0");
			return null;
		}
		
		intent.setAction(FileExplorerActivityLollipop.ACTION_PROCESSED);
		
		return result;
	}
	
	
	/*
	 * Process Multiple files
	 */
	public static List<ShareInfo> processIntentMultiple(Intent intent,Context context) {
		logDebug("processIntentMultiple");
		ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		
		ArrayList<Uri> imageUri = intent.getParcelableArrayListExtra(Intent.EXTRA_ALLOW_MULTIPLE);

		if (imageUris == null || imageUris.size() == 0) {
			logWarning("imageUris == null || imageUris.size() == 0");
			return null;
		}
		ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
		for (Uri uri : imageUris) {
			if (uri == null) {
				logWarning("continue --> uri null");
				continue;
			}
			logDebug("Uri: " + uri.toString());
			ShareInfo info = new ShareInfo();
			info.processUri(uri, context);
			if (info.file == null) {
				logWarning("continue -->info.file null");
				continue;
			}
			result.add(info);
		}

		intent.setAction(FileExplorerActivityLollipop.ACTION_PROCESSED);

		return result;
	}

	/*
	 * Get info from Uri
	 */
	private void processUri(Uri uri, Context context) {
		logDebug("processUri: " + uri);
		// getting input stream
		inputStream = null;
		try {
			inputStream = context.getContentResolver().openInputStream(uri);
		} catch (FileNotFoundException fileNotFound) {
			logError("Can't find uri: " + uri, fileNotFound);
		    return;
        } catch (Exception e) {
			logError("inputStream EXCEPTION!", e);
			String path = uri.getPath();
			logDebug("Process Uri path in the exception: " + path);
		}

		String scheme = uri.getScheme();
		if(scheme != null)
		{
			if (scheme.equals("content")) {
				logDebug("processUri go to scheme content");
				processContent(uri, context);
			} else if (scheme.equals("file")) {
				logDebug("processUri go to file content");
				processFile(uri, context);
			}
		}
		else{
			logWarning("Scheme NULL");
		}

		if (inputStream != null) {
			logDebug("processUri inputStream != null");

			file = null;
			String path = uri.getPath();
			logDebug("processUri-path: " + path);
			try{
				file = new File(path);
			}
			catch(Exception e) {
				logError("Error when creating File!", e);
			}

			if((file != null) && file.exists() && file.canRead())
			{
				size = file.length();
				logDebug("The file is accesible!");
				return;
			}

			file = null;
			path = getRealPathFromURI(context, uri);
			if(path!=null){
				logDebug("RealPath: " + path);
				try
				{
					file = new File(path);
				}
				catch(Exception e) {
					logError("EXCEPTION: No real path from URI", e);
				}
			}
			else{
				logWarning("Real path is NULL");
			}

			if((file != null) && file.exists() && file.canRead())
			{
				size = file.length();
				logDebug("Return here");
				return;
			}

            if (title == null) {
				logWarning("Title is null, return!");
                return;
            }
            if (title.contains("../") || title.contains(("..%2F"))) {
				logDebug("Internal path traversal: " + title);
                return;
            }
			logDebug("Internal No path traversal: " + title);
            if (context instanceof PdfViewerActivityLollipop
					|| (mIntent != null && mIntent.getType() != null && mIntent.getType().equals("application/pdf"))) {
				title = addPdfFileExtension(title);
            }
            file = new File(context.getCacheDir(), title);

			logDebug("Start copy to: " + file.getAbsolutePath());

			try {
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
				logDebug("File size: " + size);
			}
			catch (IOException e) {
				logError("Catch IO exception", e);
				inputStream = null;
				if (file != null) {
					file.delete();
				}
			}
		}
		else{
			logDebug("inputStream is NULL");
			String path = uri.getPath();
			logDebug("PATH: " + path);
			if (path != null){
				String [] s = path.split("file://");
				if (s.length > 1){
					String p = s[1];
					String [] s1 = p.split("/ORIGINAL");
					if (s1.length > 1){
						path = s1[0];
//						path.replaceAll("%20", " ");
						try {
							path = URLDecoder.decode(path, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							path.replaceAll("%20", " ");
						}
					}
				}
			}
			logDebug("REAL PATH: " + path);

			file = null;
			try{
				file = new File(path);
			}
			catch(Exception e){
				logError("Error when creating File!", e);
//					log(e.getMessage());
			}
			if((file != null) && file.exists() && file.canRead()) {
				size = file.length();
				logDebug("The file is accesible!");
				return;
			}
			else{
				logWarning("The file is not accesible!");
				isContact = true;
				contactUri = uri;
			}
		}
		logDebug("END processUri");
	}
	
	/*
	 * Get info from content provider
	 */
	private void processContent(Uri uri, Context context) {
		logDebug("processContent: " + uri);

		ContentProviderClient client = null;
		Cursor cursor = null;
		try {
			client = context.getContentResolver().acquireContentProviderClient(uri);
			if (client != null) {
				cursor = client.query(uri, null, null, null, null);
			}
		} catch (Exception e) {
			logError("client or cursor EXCEPTION: ", e);
		}

		if (cursor == null || cursor.getCount() == 0) {
			logWarning("Error with cursor");
			if (cursor != null) {
				cursor.close();
			}
			if (client != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					client.close();
				} else {
					client.release();
				}
			}
			return;
		}

		cursor.moveToFirst();
		int displayIndex = cursor.getColumnIndex("_display_name");
		if(displayIndex != -1)
			title = cursor.getString(displayIndex);
		int sizeIndex = cursor.getColumnIndex("_size");
		if (sizeIndex != -1) {
			String sizeString = cursor.getString(sizeIndex);
			if(sizeString!=null){
				long size = Long.valueOf(sizeString);
				if (size > 0) {
					logDebug("Size: " + size);
					this.size = size;
				}
			}
		}
		int lastModifiedIndex = cursor.getColumnIndex("last_modified");
		if(lastModifiedIndex != -1) {
		    this.lastModified = cursor.getLong(lastModifiedIndex);
        }

		if (size == -1 || inputStream == null) {
			logDebug("Keep going");
			int dataIndex = cursor.getColumnIndex("_data");
			if (dataIndex != -1) {
				String data = cursor.getString(dataIndex);
				if (data == null){
					logWarning("RETURN - data is NULL");
					return;
				}
				File dataFile = new File(data);
				if (dataFile.exists() && dataFile.canRead()) {
					if (size == -1) {
						long size = dataFile.length();
						if (size > 0) {
							logDebug("Size is: " + size);
							this.size = size;
						}
					}
					else{
						logWarning("Not valid size");
					}

					if (inputStream == null) {
						try {
							inputStream = new FileInputStream(dataFile);
						} catch (FileNotFoundException e) {
							logError("Exception", e);
						}

					}
					else{
						logWarning("inputStream is NULL");
					}
				}
			}
		}
		else{
			logWarning("Nothing done!");
		}

		cursor.close();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			client.close();
		} else {
			client.release();
		}

		logDebug("---- END process content----");
	}
	
	/*
	 * Process Uri as File path
	 */
	private void processFile(Uri uri, Context context) {
		logDebug("processing file");
		File file = null;
		try {
			file = new File(new URI(uri.toString()));
		} catch (URISyntaxException e1) {
			file = new File(uri.toString().replace("file:///", "/"));
		}
		if (!file.exists() || !file.canRead()) {
			logWarning("Can't read :( " + file.exists() + " " + file.canRead() + " "
					+ uri.toString());
			return;
		}
		if (file.isDirectory()) {
			logDebug("Is folder");
			return;
		}
		logDebug("Continue processing...");
		size = file.length();
		title = file.getName();
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
		}
		logDebug(title + " " + size);
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
}
