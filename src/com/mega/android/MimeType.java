package com.mega.android;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

/*
 * Mime type for files
 */
public class MimeType {
	
	// Icon resource mapping for different file type extensions
	private static HashMap<String, Integer> resourcesCache;
	private static SparseArray<String[]> resources;
	static {
		resourcesCache = new HashMap<String, Integer>();
		resources = new SparseArray<String[]>();
		resources.put(R.drawable.mime_3d, new String[] {"3ds", "3dm", "max", "obj", });
		resources.put(R.drawable.mime_aftereffects, new String[] {"aec", "aep", "aepx", "aes", "aet", "aetx", });
		resources.put(R.drawable.mime_audio, new String[] {"aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", });
		resources.put(R.drawable.mime_cad, new String[] {"dwg", "dxf", });
		resources.put(R.drawable.mime_compressed, new String[] {"bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "apk", "dmg", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg", });
		resources.put(R.drawable.mime_database, new String[] {"accdb", "db", "dbf", "mdb", "pdb", "sql", });
		resources.put(R.drawable.mime_dreamweaver, new String[] {"dwt", });
		resources.put(R.drawable.mime_excel, new String[] {"xls", "xlsx", "xlt", "xltm", "xltx", });
		resources.put(R.drawable.mime_executable, new String[] {"apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf", });
		resources.put(R.drawable.mime_fla_lang, new String[] {"as", "asc", "ascs", });
		resources.put(R.drawable.mime_flash, new String[] {"fla", });
		resources.put(R.drawable.mime_flash_video, new String[] {"flv", });
		resources.put(R.drawable.mime_font, new String[] {"fnt", "fon", "otf", "ttf", });
		resources.put(R.drawable.mime_gps, new String[] {"gpx", "kml", "kmz", });
		resources.put(R.drawable.mime_graphic, new String[] {"tga", "tif", "tiff", "bmp", "gif", "png", });
		resources.put(R.drawable.mime_html, new String[] {"dhtml", "htm", "html", "shtml", "xhtml", });
		resources.put(R.drawable.mime_illustrator, new String[] {"ai", "aia", "aip", "ait", "art", "irs", });
		resources.put(R.drawable.mime_image, new String[] {"jpg", "jpeg", });
		resources.put(R.drawable.mime_indesign, new String[] {"indd", });
		resources.put(R.drawable.mime_java, new String[] {"class", "jar", "java", });
		resources.put(R.drawable.mime_midi, new String[] {"mid", "midi", });
		resources.put(R.drawable.mime_pdf, new String[] {"pdf", });
		resources.put(R.drawable.mime_photoshop, new String[] {"abr", "csh", "psb", "psd", });
		resources.put(R.drawable.mime_playlist, new String[] {"asx", "m3u", "pls", });
		resources.put(R.drawable.mime_podcast, new String[] {"pcast", });
		resources.put(R.drawable.mime_powerpoint, new String[] {"ppc", "ppt", "pptx", "pps",  });
		resources.put(R.drawable.mime_premiere, new String[] {"plb", "ppj", "prproj", "prtpset", });
		resources.put(R.drawable.mime_raw, new String[] {"3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf", });
		resources.put(R.drawable.mime_real_audio, new String[] {"ra", "ram", "rm", });
		resources.put(R.drawable.mime_sourcecode, new String[] {"c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh", });
		resources.put(R.drawable.mime_spreadsheet, new String[] {"123", "gsheet", "nb", "ods", "ots", "sxc", "xlr", });
		resources.put(R.drawable.mime_subtitles, new String[] {"srt", });
		resources.put(R.drawable.mime_swf, new String[] {"swf", });
		resources.put(R.drawable.mime_text, new String[] {"ans", "ascii", "log", "odt", "rtf", "txt", "wpd", });
		resources.put(R.drawable.mime_torrent, new String[] {"torrent", });
		resources.put(R.drawable.mime_vcard, new String[] {"vcard", "vcf", });
		resources.put(R.drawable.mime_vector, new String[] {"cdr", "eps", "ps", "svg", "svgz", });
		resources.put(R.drawable.mime_video, new String[] {"3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mkv", "mk3d", "vob", "movie", });
		resources.put(R.drawable.mime_video_vob, new String[]{"vob", });
		resources.put(R.drawable.mime_web_data, new String[] {"asp", "aspx", "php", "php3", "php4", "php5", "phtml", });
		resources.put(R.drawable.mime_web_lang, new String[] {"css", "inc", "js", "xml", });
		resources.put(R.drawable.mime_word, new String[] {"doc", "docx", "dotx", "wps", });
	}

	private String type;
	private String extension;
	private int resId;

	private MimeType(String type, String extension) {
		this.type = type;
		this.extension = extension;
		resId = -1;
	}

	/*
	 * Get MimeType for file name
	 */
	@SuppressLint("DefaultLocale")
	public static MimeType typeForName(String name) {
		if (name == null) {
			name = "";
		}
		String fixedName = name.trim().toLowerCase();
		String extension = null;
		int index = fixedName.lastIndexOf(".");
		if((index != -1) && ((index+1)<fixedName.length())) {
			extension = fixedName.substring(index + 1);
		} else {
			extension = fixedName;
		}
		String detectedType = MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(extension);
		if (detectedType == null) {
			if(extension.equals("mkv"))
				detectedType = "video/x-matroska";
			else
				detectedType = "application/octet-stream";
		}
		if (extension == null) {
			extension = "";
		}
		return new MimeType(detectedType, extension);
	}

	public String getType() {
		return type;
	}
	
	/*
	 * Check is MimeType of image type
	 */
	public boolean isImage() {
		return type.startsWith("image/");
	}

	/*
	 * Once a file is downloaded, prior to create the preview, check if the file is really an image
	 */
	public boolean isImage(String path){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);
		if (options.outWidth != -1 && options.outHeight != -1) {
		    // This is an image file.
			return true;
		}
		else {
		    // This is not an image file.
			return false;
		}
	}
	
	/*
	 * Check is MimeType of video type
	 */
	public boolean isVideo() {
		return type.startsWith("video/") || extension.equals("mkv");
	}
	
	/*
	 * Check is MimeType of audio type
	 */
	public boolean isAudio() {
		return type.startsWith("audio/");
	}

	/*
	 * Get Icon for current MimeType
	 */
	public int getIconResourceId() {
		if (resId == -1) {
			if (resourcesCache.containsKey(extension)) {
				resId = resourcesCache.get(extension);
			} else {
				for (int i = 0, len = resources.size(); i < len; i++) {
					int keyResId = resources.keyAt(i);
					for (String valueExtension : resources.get(keyResId)) {
						if (extension.equals(valueExtension)) {
							resId = keyResId;
							resourcesCache.put(extension, resId);
							break;
						}
					}
					if (resId != -1) {
						break;
					}
				}
			}
			if (resId == -1) {
				resId = R.drawable.mime_generic;
			}
		}
		return resId;
	}

	public static void log(String message) {
		Util.log("MimeType", message);
	}

}