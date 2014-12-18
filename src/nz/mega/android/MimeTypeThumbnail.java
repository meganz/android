package nz.mega.android;

import java.util.HashMap;

import nz.mega.android.utils.Util;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;


/*
 * Mime type for files
 */
public class MimeTypeThumbnail {
	
	// Icon resource mapping for different file type extensions
	private static HashMap<String, Integer> resourcesCache;
	private static SparseArray<String[]> resources;
	static {
		resourcesCache = new HashMap<String, Integer>();
		resources = new SparseArray<String[]>();
		resources.put(R.drawable.ic_3d_thumbnail, new String[] {"3ds", "3dm", "max", "obj", });
		resources.put(R.drawable.after_effects_thumbnail, new String[] {"aec", "aep", "aepx", "aes", "aet", "aetx", });
		resources.put(R.drawable.audio_thumbnail, new String[] {"aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", });
		resources.put(R.drawable.cad_thumbnail, new String[] {"dwg", "dxf", });
		resources.put(R.drawable.compressed_thumbnail, new String[] {"bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "apk", "dmg", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg", });
		resources.put(R.drawable.database_thumbnail, new String[] {"accdb", "db", "dbf", "mdb", "pdb", "sql", });
		resources.put(R.drawable.dmg_thumbnail, new String[]{"dmg", });
		resources.put(R.drawable.dreamweaver_thumbnail, new String[] {"dwt", });
		resources.put(R.drawable.excell_thumbnail, new String[] {"xls", "xlsx", "xlt", "xltm", "xltx", });
		resources.put(R.drawable.exe_thumbnail, new String[] {"apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf", });
		resources.put(R.drawable.fla_lang_thumbnail, new String[] {"as", "asc", "ascs", });
		resources.put(R.drawable.flash_thumbnail, new String[] {"fla", });
		resources.put(R.drawable.font_thumbnail, new String[] {"fnt", "fon", "otf", "ttf", });
		resources.put(R.drawable.gis_thumbnail, new String[] {"gpx", "kml", "kmz", });
		resources.put(R.drawable.graphic_thumbnail, new String[] {"tga", "tif", "tiff", "bmp", "gif", "png", });
		resources.put(R.drawable.html_thumbnail, new String[] {"dhtml", "htm", "html", "shtml", "xhtml", });
		resources.put(R.drawable.illustrator_thumbnail, new String[] {"ai", "aia", "aip", "ait", "art", "irs", });
		resources.put(R.drawable.image_thumbnail, new String[] {"jpg", "jpeg", });
		resources.put(R.drawable.indesign_thumbnail, new String[] {"indd", });
		resources.put(R.drawable.java_thumbnail, new String[] {"class", "jar", "java", });
		resources.put(R.drawable.midi_thumbnail, new String[] {"mid", "midi", });
		resources.put(R.drawable.pdf_thumbnail, new String[] {"pdf", });
		resources.put(R.drawable.photoshop_thumbnail, new String[] {"abr", "csh", "psb", "psd", });
		resources.put(R.drawable.playlist_thumbnail, new String[] {"asx", "m3u", "pls", });
		resources.put(R.drawable.podcast_thumbnail, new String[] {"pcast", });
		resources.put(R.drawable.powerpoint_thumbnail, new String[] {"ppc", "ppt", "pptx", "pps",  });
		resources.put(R.drawable.premiere_thumbnail, new String[] {"plb", "ppj", "prproj", "prtpset", });
		resources.put(R.drawable.raw_thumbnail, new String[] {"3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf", });
		resources.put(R.drawable.real_audio_thumbnail, new String[] {"ra", "ram", "rm", });
		resources.put(R.drawable.source_thumbnail, new String[] {"c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh", });
		resources.put(R.drawable.spreadsheet_thumbnail, new String[] {"123", "gsheet", "nb", "ods", "ots", "sxc", "xlr", });
		resources.put(R.drawable.subtitles_thumbnail, new String[] {"srt", });
		resources.put(R.drawable.swf_thumbnail, new String[] {"swf", "flv", });
		resources.put(R.drawable.text_thumbnail, new String[] {"ans", "ascii", "log", "odt", "rtf", "txt", "wpd", });
		resources.put(R.drawable.torrent_thumbnail, new String[] {"torrent", });
		resources.put(R.drawable.vcard_thumbnail, new String[] {"vcard", "vcf", });
		resources.put(R.drawable.vector_thumbnail, new String[] {"cdr", "eps", "ps", "svg", "svgz", });
		resources.put(R.drawable.video_thumbnail, new String[] {"3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mkv", "mk3d", "vob", "movie", });
		resources.put(R.drawable.video_vob_thumbnail, new String[]{"vob", });
		resources.put(R.drawable.web_data_thumbnail, new String[] {"asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml", });
		resources.put(R.drawable.word_thumbnail, new String[] {"doc", "docx", "dotx", "wps", });
	}

	private String type;
	private String extension;
	private int resId;

	private MimeTypeThumbnail(String type, String extension) {
		this.type = type;
		this.extension = extension;
		resId = -1;
	}

	/*
	 * Get MimeType for file name
	 */
	@SuppressLint("DefaultLocale")
	public static MimeTypeThumbnail typeForName(String name) {
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
		return new MimeTypeThumbnail(detectedType, extension);
	}

	public String getType() {
		return type;
	}
	
	public boolean isDocument(){
		boolean r = type.startsWith("application/pdf") || type.startsWith("application/msword") || type.startsWith("application/vnd.ms-excel") || type.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || type.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || type.startsWith("application/rtf") || type.startsWith("text/plain");
		
		return r;
	}
	
	/*
	 * Check is MimeType of image type
	 */
	public boolean isImage() {
		return type.startsWith("image/");
	}
	
	public boolean isPdf(){
		return type.startsWith("application/pdf");
	}
	
	public boolean isZip(){
		
		if(type.startsWith("application/zip")||type.startsWith("multipart/x-zip")){
			return true;
		}
		return false;
		
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
				resId = R.drawable.generic_thumbnail;
			}
		}
		return resId;
	}

	public static void log(String message) {
		Util.log("MimeType", message);
	}

}